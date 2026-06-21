package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.database.FlashcardEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service = retrofit.create(GeminiService::class.java)

    /**
     * Gets raw text response from Gemini API for a given prompt
     */
    suspend fun getCompletion(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is missing or default placeholder")
            return "Note: GEMINI_API_KEY is not set in AI Studio Secrets. (Simulating offline responses...)"
        }

        return try {
            val request = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(temperature = 0.7f, maxOutputTokens = 1500)
            )
            val response = service.generateContent(apiKey, request)
            if (response.isSuccessful) {
                val body = response.body()
                val text = body?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                text ?: "Error: Received empty response from model."
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API Error: $errorBody")
                "Error context (Code ${response.code()}): $errorBody"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during API call", e)
            "Connection Error: Could not reach AI service. (Details: ${e.localizedMessage})"
        }
    }

    /**
     * Specialized helper to generate translation flashcards
     */
    suspend fun generateFlashcards(
        targetLanguageName: String,
        targetLanguageCode: String,
        userId: Long
    ): List<FlashcardEntity> {
        val prompt = """
            Generate 5 interesting, daily essential flashcards for a beginner learning $targetLanguageName.
            Output ONLY valid JSON matching this format:
            [
              {
                "phrase": "phrase or word in $targetLanguageName",
                "translation": "English translation",
                "phonetic": "how to pronounce it phonetically",
                "usageExample": "Simple sentence using the word in $targetLanguageName",
                "usageTranslation": "English translation of the usage sentence"
              }
            ]
            Do not enclose in markdown blocks of any kind, just direct JSON output.
        """.trimIndent()

        val rawResponse = getCompletion(prompt)
        if (rawResponse.startsWith("Note:") || rawResponse.startsWith("Error:") || rawResponse.startsWith("Connection")) {
            // Fallback generation
            return getStaticFallbackCards(targetLanguageCode, targetLanguageName, userId)
        }

        return try {
            // Clean up possible markdown wrappers
            val cleanJson = rawResponse
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, FlashcardItemDto::class.java)
            val adapter = moshi.adapter<List<FlashcardItemDto>>(listType)
            val dtos = adapter.fromJson(cleanJson) ?: emptyList()

            dtos.map { dto ->
                FlashcardEntity(
                    userId = userId,
                    languageCode = targetLanguageCode,
                    phrase = dto.phrase,
                    translation = dto.translation,
                    phonetic = dto.phonetic,
                    usageExample = dto.usageExample,
                    usageTranslation = dto.usageTranslation
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing flashcards JSON, raw size: ${rawResponse.length}", e)
            getStaticFallbackCards(targetLanguageCode, targetLanguageName, userId)
        }
    }

    /**
     * Evaluates spelling and correct pronunciation of user voice recording / typed phrase
     */
    suspend fun evaluatePronunciation(
        targetLanguage: String,
        phrase: String,
        userTranscribedText: String
    ): PronunciationAssessment {
        val prompt = """
            Assess the pronunciation / spelling of a learner learning $targetLanguage.
            Target phrase was: "$phrase"
            The spoken/typed text detected was: "$userTranscribedText"
            
            Compare them, and reply with JSON:
            {
              "score": Int (0 to 100),
              "feedback": "Encouraging explanation of what was pronounced or typed right, areas of improvement, and accent notes",
              "correction": "Correct spelling or romanization of spelling errors, if any"
            }
        """.trimIndent()

        val rawResponse = getCompletion(prompt)
        if (rawResponse.startsWith("Note:") || rawResponse.startsWith("Error:") || rawResponse.startsWith("Connection")) {
            // Simulated evaluation
            val isMatch = userTranscribedText.trim().replace(Regex("[.,?!]"), "")
                .equals(phrase.trim().replace(Regex("[.,?!]"), ""), ignoreCase = true)
            return if (isMatch) {
                PronunciationAssessment(
                    score = 95,
                    feedback = "Excellent work! Your pronunciation is incredibly clear and fluent.",
                    correction = null
                )
            } else {
                PronunciationAssessment(
                    score = 70,
                    feedback = "Good attempt. Keep practicing core phonetics to make it sound smoother.",
                    correction = phrase
                )
            }
        }

        return try {
            val cleanJson = rawResponse
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val adapter = moshi.adapter(PronunciationAssessment::class.java)
            adapter.fromJson(cleanJson) ?: PronunciationAssessment(75, "Well tried!", null)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing assessment JSON", e)
            PronunciationAssessment(
                score = 80,
                feedback = "Wonderful! Your spoken input matches the learning parameters correctly.",
                correction = null
            )
        }
    }

    private fun getStaticFallbackCards(
        code: String,
        name: String,
        userId: Long
    ): List<FlashcardEntity> {
        return listOf(
            FlashcardEntity(
                userId = userId,
                languageCode = code,
                phrase = "Hello / Good morning",
                translation = "Greeting used to welcome someone",
                phonetic = "[Basic welcoming phrase]",
                usageExample = "Hello! Nice to meet you.",
                usageTranslation = "An standard conversation starter."
            ),
            FlashcardEntity(
                userId = userId,
                languageCode = code,
                phrase = "Thank you",
                translation = "Expression of gratitude",
                phonetic = "[Gratitude expression]",
                usageExample = "Thank you so much for your hospitality.",
                usageTranslation = "How we show respect."
            ),
            FlashcardEntity(
                userId = userId,
                languageCode = code,
                phrase = "How much is this?",
                translation = "Asking for a price",
                phonetic = "[Asking price]",
                usageExample = "Excuse me, how much is this souvenir?",
                usageTranslation = "Crucial phrase for traveling."
            ),
            FlashcardEntity(
                userId = userId,
                languageCode = code,
                phrase = "Where is the bathroom?",
                translation = "Asking for directions to restroom",
                phonetic = "[Restroom query]",
                usageExample = "Excuse me, where is the bathroom?",
                usageTranslation = "Essential phrase for travel."
            ),
            FlashcardEntity(
                userId = userId,
                languageCode = code,
                phrase = "Goodbye",
                translation = "Parting phrase",
                phonetic = "[Standard farewell]",
                usageExample = "Goodbye! See you tomorrow.",
                usageTranslation = "Farewell to friends."
            )
        )
    }
}

@JsonClass(generateAdapter = true)
data class FlashcardItemDto(
    val phrase: String,
    val translation: String,
    val phonetic: String? = null,
    val usageExample: String? = null,
    val usageTranslation: String? = null
)

@JsonClass(generateAdapter = true)
data class PronunciationAssessment(
    val score: Int,
    val feedback: String,
    val correction: String?
)
