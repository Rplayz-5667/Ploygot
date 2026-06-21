package com.example.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Language
import com.example.data.LanguageRegistry
import com.example.data.ConversationScenario
import com.example.data.ScenarioRegistry
import com.example.data.api.GeminiClient
import com.example.data.api.PronunciationAssessment
import com.example.data.database.ChatHistoryEntity
import com.example.data.database.FlashcardEntity
import com.example.data.repository.AppRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val appRepository = AppRepository(application)
    private val TAG = "MainViewModel"

    // 150+ Languages Registry Access
    val languages = LanguageRegistry.languages
    private val _selectedLanguage = MutableStateFlow(LanguageRegistry.getByCode("ja")!!)
    val selectedLanguage: StateFlow<Language> = _selectedLanguage.asStateFlow()

    // Active Scenario practice state
    private val _activeScenario = MutableStateFlow<ConversationScenario?>(null)
    val activeScenario: StateFlow<ConversationScenario?> = _activeScenario.asStateFlow()

    // Database flashcards
    private val _flashcards = MutableStateFlow<List<FlashcardEntity>>(emptyList())
    val flashcards: StateFlow<List<FlashcardEntity>> = _flashcards.asStateFlow()

    private val _flashcardCount = MutableStateFlow(0)
    val flashcardCount: StateFlow<Int> = _flashcardCount.asStateFlow()

    private val _masteredCount = MutableStateFlow(0)
    val masteredCount: StateFlow<Int> = _masteredCount.asStateFlow()

    // Local SQLite (IndexedDB Equivalent) Cache states
    private val _isPreseedingOffline = MutableStateFlow(false)
    val isPreseedingOffline: StateFlow<Boolean> = _isPreseedingOffline.asStateFlow()

    private val _preseedEventMessage = MutableStateFlow<String?>(null)
    val preseedEventMessage: StateFlow<String?> = _preseedEventMessage.asStateFlow()

    // Active AI Generator loading status
    private val _isLoadingFlashcards = MutableStateFlow(false)
    val isLoadingFlashcards: StateFlow<Boolean> = _isLoadingFlashcards.asStateFlow()

    // Activity Chat flow
    private val _chatMessages = MutableStateFlow<List<ChatHistoryEntity>>(emptyList())
    val chatMessages: StateFlow<List<ChatHistoryEntity>> = _chatMessages.asStateFlow()

    private val _isSendingChat = MutableStateFlow(false)
    val isSendingChat: StateFlow<Boolean> = _isSendingChat.asStateFlow()

    // TextToSpeech engine
    private var tts: TextToSpeech? = null
    val isTtsReady = mutableStateOf(false)

    // Current Pronunciation Arena parameters
    val pronunciationPhrase = mutableStateOf("Konnichiwa, o-genki desu ka?")
    val pronunciationTranslation = mutableStateOf("Hello, how are you?")
    val pronunciationPhonetic = mutableStateOf("[Kon-nee-chee-wah, oh-gen-kee deh-su kah?]")
    val userSpeechInput = mutableStateOf("")
    val assessmentResult = mutableStateOf<PronunciationAssessment?>(null)
    val isAssessing = mutableStateOf(false)
    val maxPronunScore = mutableStateOf(0)

    private var currentUserId: Long = 1L

    init {
        tts = TextToSpeech(application, this)
        initDataCollectors()
    }

    fun setUserId(userId: Long) {
        if (currentUserId != userId) {
            currentUserId = userId
            initDataCollectors()
        }
    }

    fun selectLanguage(language: Language, authViewModel: AuthViewModel) {
        _selectedLanguage.value = language
        authViewModel.changeTargetLanguage(language.code)
        initDataCollectors()

        // Set up pronunciation phrase based on language preset
        pronunResetWithLang(language)
    }

    private fun pronunResetWithLang(language: Language) {
        pronunciationPhrase.value = when (language.code) {
            "ja" -> "Konnichiwa, o-genki desu ka?"
            "es" -> "Hola, ¿cómo estás?"
            "fr" -> "Bonjour, comment ça va?"
            "de" -> "Hallo, wie geht es dir?"
            "it" -> "Ciao, come stai?"
            "zh" -> "Nǐ hǎo, nǐ hǎo ma?"
            "ko" -> "Annyeonghaseyo, jal jinaess-eoyo?"
            else -> "Hello, ${language.greeting}!"
        }
        pronunciationTranslation.value = "Hello, how are you?"
        pronunciationPhonetic.value = "[Standard greeting and state check]"
        userSpeechInput.value = ""
        assessmentResult.value = null
    }

    private fun initDataCollectors() {
        viewModelScope.launch {
            appRepository.getFlashcards(currentUserId, _selectedLanguage.value.code).collectLatest {
                _flashcards.value = it
            }
        }
        viewModelScope.launch {
            appRepository.getFlashcardCount(currentUserId, _selectedLanguage.value.code).collectLatest {
                _flashcardCount.value = it
            }
        }
        viewModelScope.launch {
            appRepository.getMasteredCount(currentUserId, _selectedLanguage.value.code).collectLatest {
                _masteredCount.value = it
            }
        }
        viewModelScope.launch {
            appRepository.getChats(currentUserId, _selectedLanguage.value.code).collectLatest {
                _chatMessages.value = it
            }
        }
    }

    // === TEXT TO SPEECH OPERATIONS ===
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady.value = true
            updateTtsLocale()
        } else {
            Log.e(TAG, "Failed initializing TTS engine")
        }
    }

    private fun updateTtsLocale() {
        val languageCode = _selectedLanguage.value.code
        val locale = getLocaleForCode(languageCode)
        tts?.language = locale
    }

    fun speakText(text: String) {
        if (!isTtsReady.value) return
        updateTtsLocale()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PolyglotSpeechId")
    }

    private fun getLocaleForCode(code: String): Locale {
        return when (code) {
            "en" -> Locale.ENGLISH
            "es" -> Locale("es", "ES")
            "fr" -> Locale.FRANCE
            "de" -> Locale.GERMANY
            "it" -> Locale.ITALY
            "pt" -> Locale("pt", "PT")
            "ja" -> Locale.JAPAN
            "ko" -> Locale.KOREA
            "zh" -> Locale.CHINESE
            "ru" -> Locale("ru", "RU")
            else -> Locale.US // default
        }
    }

    // === FLASHCARD ACTIONS ===
    fun triggerAiDeckGeneration() {
        viewModelScope.launch {
            _isLoadingFlashcards.value = true
            try {
                appRepository.loadAiDailyChallenge(
                    currentUserId,
                    _selectedLanguage.value.name,
                    _selectedLanguage.value.code
                )
            } catch (e: Exception) {
                Log.e(TAG, "AI generated cards error", e)
            } finally {
                _isLoadingFlashcards.value = false
            }
        }
    }

    fun addNewFlashcard(phrase: String, translation: String, phonetic: String?, usage: String?, usageTranslation: String?) {
        viewModelScope.launch {
            appRepository.createCustomFlashcard(
                userId = currentUserId,
                languageCode = _selectedLanguage.value.code,
                phrase = phrase,
                translation = translation,
                phonetic = phonetic,
                usageExample = usage,
                usageTranslation = usageTranslation
            )
        }
    }

    fun gradeFlashcardReview(flashcard: FlashcardEntity, knewIt: Boolean, authViewModel: AuthViewModel) {
        viewModelScope.launch {
            appRepository.gradeReview(flashcard, knewIt)
            if (knewIt) {
                authViewModel.awardXp(10, "Flashcard")
            }
        }
    }

    fun deleteCard(flashcard: FlashcardEntity) {
        viewModelScope.launch {
            appRepository.deleteFlashcard(flashcard)
        }
    }

    fun triggerPreseedOfflineVocabulary() {
        viewModelScope.launch {
            _isPreseedingOffline.value = true
            _preseedEventMessage.value = null
            try {
                val count = appRepository.preseedOfflineCards(currentUserId, _selectedLanguage.value.code)
                _preseedEventMessage.value = "Successfully pre-seeded $count offline items in standard SQLite (Mapped from IndexedDB equivalent)!"
            } catch (e: Exception) {
                Log.e("MainViewModel", "Preseed offline cards error", e)
                _preseedEventMessage.value = "Failed to pre-seed: ${e.localizedMessage}"
            } finally {
                _isPreseedingOffline.value = false
            }
        }
    }

    fun clearPreseedMessage() {
        _preseedEventMessage.value = null
    }

    // === CONVERSATIONAL CHAT ACTIVITIES ===
    fun startScenario(scenario: ConversationScenario) {
        viewModelScope.launch {
            _activeScenario.value = scenario
            appRepository.clearChat(currentUserId, _selectedLanguage.value.code)

            // Insert initial message from AI
            val initMsg = scenario.getInitialMessageForCode(_selectedLanguage.value.code)
            appRepository.addChatMessage(
                userId = currentUserId,
                languageCode = _selectedLanguage.value.code,
                sender = "ai",
                content = initMsg,
                translation = scenario.initialAiMessageTranslation,
                feedback = "Scenario practice started! Complete tasks for: ${scenario.title} (${scenario.emoji})"
            )

            // Auto-speak the initial message to kick off the voice-based practice!
            speakText(initMsg)
        }
    }

    fun exitScenario() {
        _activeScenario.value = null
        viewModelScope.launch {
            appRepository.clearChat(currentUserId, _selectedLanguage.value.code)
        }
    }

    fun sendChatMessage(userText: String, authViewModel: AuthViewModel) {
        if (userText.isBlank()) return
        viewModelScope.launch {
            _isSendingChat.value = true
            // Save user msg
            appRepository.addChatMessage(
                userId = currentUserId,
                languageCode = _selectedLanguage.value.code,
                sender = "user",
                content = userText
            )

            // Compose full conversation content for conversational retention
            val contextString = _chatMessages.value.takeLast(10).joinToString("\n") {
                "${it.sender}: ${it.content}"
            }

            // Assemble roleplaying system guidelines
            val scenario = _activeScenario.value
            val systemRole = if (scenario != null) {
                scenario.systemPrompt
            } else {
                "You are a patient, conversational language partner for the language \"${_selectedLanguage.value.name}\". Keep the response relatively short (1 to 3 simple sentences). Speak purely in that language."
            }

            val prompt = """
                $systemRole
                
                The conversation history so far (use this as context):
                $contextString
                
                Respond to user's last message in "${_selectedLanguage.value.name}". 
                Also, analyze the user's last message for grammar, spelling corrections, and provide constructive suggestions on pronunciation/phonetics in English.
                
                You MUST return JSON matching this exact structure:
                {
                  "responseText": "Your conversational reply to the user, strictly formatted in ${_selectedLanguage.value.name}",
                  "translation": "English translation of your reply",
                  "feedback": "Spelling/Grammar correction feedback, or pronunciation advice based on their input, in English. Keep it brief and encouraging."
                }
                
                Direct JSON output only. DO NOT wrap in markdown code blocks, just raw JSON text.
            """.trimIndent()

            val aiResponse = GeminiClient.getCompletion(prompt)
            _isSendingChat.value = false

            var responseText = aiResponse
            var translation: String? = null
            var feedback: String? = null

            try {
                // Clean markdown wrappers if any
                val cleanJson = aiResponse
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val moshi = MoshiHelper.moshi
                val adapter = moshi.adapter(AiChatReplyDto::class.java)
                val dto = adapter.fromJson(cleanJson)
                if (dto != null) {
                    responseText = dto.responseText
                    translation = dto.translation
                    feedback = dto.feedback
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed standard JSON chat response parsing, cleaning with fallback...", e)
                // Fallback Braces parser
                responseText = aiResponse
                try {
                    if (aiResponse.contains("{") && aiResponse.contains("}")) {
                        val startIdx = aiResponse.indexOf("{")
                        val endIdx = aiResponse.indexOf("}")
                        if (startIdx < endIdx) {
                            translation = aiResponse.substring(startIdx + 1, endIdx).trim()
                            responseText = aiResponse.substring(0, startIdx).trim()
                        }
                    }
                } catch (innerEx: Exception) {
                    Log.e(TAG, "Error cleaning AI response braces", innerEx)
                }
                feedback = "Grammar check: Good conversational effort! To review specific grammar elements, try starting a Roleplay Scenario above."
            }

            appRepository.addChatMessage(
                userId = currentUserId,
                languageCode = _selectedLanguage.value.code,
                sender = "ai",
                content = responseText,
                translation = translation,
                feedback = feedback
            )

            // Speak response aloud if TTS is available for voice immersion
            speakText(responseText)

            // Award points for conversational text
            authViewModel.awardXp(15, "Chat")
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            appRepository.clearChat(currentUserId, _selectedLanguage.value.code)
        }
    }

    // === PRONUNCIATION PRACTICE ARENA ===
    fun generateNewPronunciationPhrase() {
        viewModelScope.launch {
            isAssessing.value = true
            val prompt = """
                Formulate 1 daily conversation sentence in "${_selectedLanguage.value.name}" appropriate for a basic speaker.
                Include pronunciation breakdown / phonetic transcription, and English translation.
                Format as JSON:
                {
                  "phrase": "The sentence",
                  "translation": "The English translation",
                  "phonetic": "[phonetic pronunciation assist]"
                }
                Direct JSON response without markdown.
            """.trimIndent()

            val rawResponse = GeminiClient.getCompletion(prompt)
            isAssessing.value = false

            try {
                val cleanJson = rawResponse
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val moshi = MoshiHelper.moshi
                val adapter = moshi.adapter(PronunPhraseDto::class.java)
                val dto = adapter.fromJson(cleanJson)
                if (dto != null) {
                    pronunciationPhrase.value = dto.phrase
                    pronunciationTranslation.value = dto.translation
                    pronunciationPhonetic.value = dto.phonetic ?: ""
                    userSpeechInput.value = ""
                    assessmentResult.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed dynamic pronun generation, using registry fallback", e)
                pronunResetWithLang(_selectedLanguage.value)
            }
        }
    }

    fun assessPronunciation(transcriptText: String, authViewModel: AuthViewModel) {
        if (transcriptText.isBlank()) return
        userSpeechInput.value = transcriptText
        viewModelScope.launch {
            isAssessing.value = true
            val assessment = GeminiClient.evaluatePronunciation(
                _selectedLanguage.value.name,
                pronunciationPhrase.value,
                transcriptText
            )
            assessmentResult.value = assessment
            isAssessing.value = false

            if (assessment.score > maxPronunScore.value) {
                maxPronunScore.value = assessment.score
            }

            // Persist pronunciation high score in database & update user state
            authViewModel.updatePronunciationScore(assessment.score)

            // Award dynamic XP based on pronunciation quality!
            val xpEarned = when {
                assessment.score >= 90 -> 25
                assessment.score >= 70 -> 15
                else -> 5
            }
            authViewModel.awardXp(xpEarned, "Pronunciation")
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}

// Utility class definitions matching serializers
@com.squareup.moshi.JsonClass(generateAdapter = true)
data class PronunPhraseDto(
    val phrase: String,
    val translation: String,
    val phonetic: String?
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class AiChatReplyDto(
    val responseText: String,
    val translation: String,
    val feedback: String? = null
)

object MoshiHelper {
    val moshi: Moshi = Moshi.Builder()
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
}
