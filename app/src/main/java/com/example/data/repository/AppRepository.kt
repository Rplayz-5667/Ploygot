package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.GeminiClient
import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

class AppRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val flashcardDao = db.flashcardDao()
    private val chatHistoryDao = db.chatHistoryDao()

    fun getFlashcards(userId: Long, languageCode: String): Flow<List<FlashcardEntity>> {
        return flashcardDao.getFlashcardsFlow(userId, languageCode)
    }

    fun getFlashcardCount(userId: Long, languageCode: String): Flow<Int> {
        return flashcardDao.getFlashcardCountFlow(userId, languageCode)
    }

    fun getMasteredCount(userId: Long, languageCode: String): Flow<Int> {
        return flashcardDao.getMasteredCountFlow(userId, languageCode)
    }

    suspend fun getDueFlashcards(userId: Long, languageCode: String): List<FlashcardEntity> {
        return flashcardDao.getDueFlashcards(userId, languageCode, System.currentTimeMillis())
    }

    suspend fun createCustomFlashcard(
        userId: Long,
        languageCode: String,
        phrase: String,
        translation: String,
        phonetic: String? = null,
        usageExample: String? = null,
        usageTranslation: String? = null
    ): Long {
        val fc = FlashcardEntity(
            userId = userId,
            languageCode = languageCode,
            phrase = phrase,
            translation = translation,
            phonetic = phonetic,
            usageExample = usageExample,
            usageTranslation = usageTranslation
        )
        return flashcardDao.insertFlashcard(fc)
    }

    suspend fun loadAiDailyChallenge(userId: Long, languageName: String, languageCode: String): List<FlashcardEntity> {
        val newCards = GeminiClient.generateFlashcards(languageName, languageCode, userId)
        if (newCards.isNotEmpty()) {
            flashcardDao.insertFlashcards(newCards)
        }
        return newCards
    }

    suspend fun gradeReview(flashcard: FlashcardEntity, knewIt: Boolean) {
        val nextBox = if (knewIt) {
            (flashcard.box + 1).coerceAtMost(5)
        } else {
            1 // Reset to Box 1 for relearning
        }

        // Add spaced review timing: Box 1 (1 min), Box 2 (5 min), Box 3 (30 min), Box 4 (2 hours), Box 5 (24 hours)
        val intervalMs = when (nextBox) {
            1 -> 60_000L
            2 -> 300_000L
            3 -> 1_800_000L
            4 -> 7_200_000L
            else -> 86_400_000L // Box 5
        }

        val updated = flashcard.copy(
            box = nextBox,
            nextReviewTime = System.currentTimeMillis() + intervalMs,
            isMastered = nextBox == 5,
            lastTestedTime = System.currentTimeMillis()
        )
        flashcardDao.updateFlashcard(updated)
    }

    suspend fun deleteFlashcard(flashcard: FlashcardEntity) {
        flashcardDao.deleteFlashcard(flashcard)
    }

    // === CHAT ACTIVITIES ===
    fun getChats(userId: Long, languageCode: String): Flow<List<ChatHistoryEntity>> {
        return chatHistoryDao.getChatHistoryFlow(userId, languageCode)
    }

    suspend fun addChatMessage(userId: Long, languageCode: String, sender: String, content: String, translation: String? = null, feedback: String? = null) {
        val msg = ChatHistoryEntity(
            userId = userId,
            languageCode = languageCode,
            sender = sender,
            content = content,
            translation = translation,
            feedback = feedback
        )
        chatHistoryDao.insertChatMessage(msg)
    }

    suspend fun clearChat(userId: Long, languageCode: String) {
        chatHistoryDao.clearChatHistory(userId, languageCode)
    }

    suspend fun preseedOfflineCards(userId: Long, languageCode: String): Int {
        val seededList = when (languageCode) {
            "ja" -> listOf(
                FlashcardEntity(userId = userId, languageCode = "ja", phrase = "こんにちは", translation = "Hello / Good afternoon", phonetic = "Konnichiwa", usageExample = "こんにちは！お元気ですか？", usageTranslation = "Hello! How are you?"),
                FlashcardEntity(userId = userId, languageCode = "ja", phrase = "ありがとう", translation = "Thank you", phonetic = "Arigatou", usageExample = "ありがとう！助かります。", usageTranslation = "Thank you! That helps a lot."),
                FlashcardEntity(userId = userId, languageCode = "ja", phrase = "すみません", translation = "Excuse me / Sorry", phonetic = "Sumimasen", usageExample = "すみません、これはいくらですか？", usageTranslation = "Excuse me, how much is this?"),
                FlashcardEntity(userId = userId, languageCode = "ja", phrase = "お元気ですか？", translation = "How are you?", phonetic = "O-genki desu ka?", usageExample = "お久しぶりですね！お元気ですか？", usageTranslation = "It's been a while! How are you?"),
                FlashcardEntity(userId = userId, languageCode = "ja", phrase = "トイレはどこですか？", translation = "Where is the bathroom?", phonetic = "Toire wa doko desu ka?", usageExample = "あの、トイレはどこですか？", usageTranslation = "Excuse me, where is the restroom?"),
                FlashcardEntity(userId = userId, languageCode = "ja", phrase = "美味しい", translation = "Delicious / Tasty", phonetic = "Oishi-i", usageExample = "このラーメンはとても美味しいです。", usageTranslation = "This ramen is very delicious."),
                FlashcardEntity(userId = userId, languageCode = "ja", phrase = "私は日本に行きたいです", translation = "I want to go to Japan", phonetic = "Watashi wa nihon ni ikitai desu", usageExample = "日本語を勉強して、私は日本に行きたいです。", usageTranslation = "I study Japanese and I want to go to Japan."),
                FlashcardEntity(userId = userId, languageCode = "ja", phrase = "はじめまして", translation = "Nice to meet you", phonetic = "Hajimemashite", usageExample = "はじめまして。たなかです。", usageTranslation = "Nice to meet you. I am Tanaka."),
                FlashcardEntity(userId = userId, languageCode = "ja", phrase = "乾杯", translation = "Cheers!", phonetic = "Kanpai", usageExample = "みんなで乾杯しましょう！", usageTranslation = "Let's all cheer!"),
                FlashcardEntity(userId = userId, languageCode = "ja", phrase = "さようなら", translation = "Goodbye", phonetic = "Sayounara", usageExample = "さようなら、また来週！", usageTranslation = "Goodbye, see you next week!")
            )
            "es" -> listOf(
                FlashcardEntity(userId = userId, languageCode = "es", phrase = "Hola", translation = "Hello", phonetic = "[Ola]", usageExample = "¡Hola! ¿Cómo estás?", usageTranslation = "Hello! How are you?"),
                FlashcardEntity(userId = userId, languageCode = "es", phrase = "Gracias", translation = "Thank you", phonetic = "[Grah-syas]", usageExample = "Muchas gracias por la comida.", usageTranslation = "Thank you very much for the food."),
                FlashcardEntity(userId = userId, languageCode = "es", phrase = "Por favor", translation = "Please", phonetic = "[Por fah-vor]", usageExample = "Un vaso de agua, por favor.", usageTranslation = "A glass of water, please."),
                FlashcardEntity(userId = userId, languageCode = "es", phrase = "Buenos días", translation = "Good morning", phonetic = "[Bwenos dee-as]", usageExample = "¡Buenos días, mi amigo!", usageTranslation = "Good morning, my friend!"),
                FlashcardEntity(userId = userId, languageCode = "es", phrase = "Buenas noches", translation = "Good night", phonetic = "[Bwenas noches]", usageExample = "Que tengas buenas noches.", usageTranslation = "Have a good night."),
                FlashcardEntity(userId = userId, languageCode = "es", phrase = "¿Dónde está el baño?", translation = "Where is the bathroom?", phonetic = "[Don-de es-tah el bah-nyo]", usageExample = "Por favor, ¿dónde está el baño?", usageTranslation = "Please, where is the restroom?"),
                FlashcardEntity(userId = userId, languageCode = "es", phrase = "¿Cuánto cuesta esto?", translation = "How much is this?", phonetic = "[Kwan-to kwes-ta es-to]", usageExample = "Disculpe, ¿cuánto cuesta este libro?", usageTranslation = "Excuse me, how much is this book?"),
                FlashcardEntity(userId = userId, languageCode = "es", phrase = "Me gusta mucho", translation = "I like it a lot", phonetic = "[Me goos-ta moo-cho]", usageExample = "Me gusta mucho la música española.", usageTranslation = "I like Spanish music a lot."),
                FlashcardEntity(userId = userId, languageCode = "es", phrase = "¡Salud!", translation = "Cheers! / Bless you!", phonetic = "[Sah-lood]", usageExample = "¡Salud para todos!", usageTranslation = "Cheers to everyone!"),
                FlashcardEntity(userId = userId, languageCode = "es", phrase = "Adiós", translation = "Goodbye", phonetic = "[Ah-dyohs]", usageExample = "¡Adiós! Nos vemos pronto.", usageTranslation = "Goodbye! See you soon.")
            )
            "fr" -> listOf(
                FlashcardEntity(userId = userId, languageCode = "fr", phrase = "Bonjour", translation = "Hello / Good morning", phonetic = "Bon-zhoor", usageExample = "Bonjour! Comment ça va?", usageTranslation = "Hello! How is it going?"),
                FlashcardEntity(userId = userId, languageCode = "fr", phrase = "Merci beaucoup", translation = "Thank you very much", phonetic = "Mair-see bo-koo", usageExample = "Merci beaucoup pour votre aide précieuse.", usageTranslation = "Thank you very much for your precious help."),
                FlashcardEntity(userId = userId, languageCode = "fr", phrase = "S'il vous plaît", translation = "Please", phonetic = "Seel voo play", usageExample = "Un café, s'il vous plaît.", usageTranslation = "A coffee, please."),
                FlashcardEntity(userId = userId, languageCode = "fr", phrase = "Enchanté", translation = "Nice to meet you", phonetic = "On-shon-tay", usageExample = "Enchanté! Je m'appelle Jean.", usageTranslation = "Nice to meet you! My name is Jean."),
                FlashcardEntity(userId = userId, languageCode = "fr", phrase = "Où sont les toilettes?", translation = "Where is the bathroom?", phonetic = "Oo son lay twah-let?", usageExample = "Excusez-moi, où sont les toilettes?", usageTranslation = "Excuse me, where are the restrooms?"),
                FlashcardEntity(userId = userId, languageCode = "fr", phrase = "Combien ça coûte?", translation = "How much is this?", phonetic = "Kom-byan sah koot?", usageExample = "Pardon, combien ça coûte?", usageTranslation = "Pardon, how much does it cost?"),
                FlashcardEntity(userId = userId, languageCode = "fr", phrase = "C'est délicieux", translation = "It is delicious", phonetic = "Say day-lee-syuh", usageExample = "Ce croissant chaud, c'est délicieux!", usageTranslation = "This warm croissant is delicious!"),
                FlashcardEntity(userId = userId, languageCode = "fr", phrase = "Excusez-moi", translation = "Excuse me", phonetic = "Ex-koo-zay mwah", usageExample = "Excusez-moi de vous déranger.", usageTranslation = "Excuse me for disturbing you."),
                FlashcardEntity(userId = userId, languageCode = "fr", phrase = "Santé !", translation = "Cheers!", phonetic = "Son-tay", usageExample = "Levez vos verres, santé !", usageTranslation = "Raise your glasses, cheers!"),
                FlashcardEntity(userId = userId, languageCode = "fr", phrase = "Au revoir", translation = "Goodbye", phonetic = "Oh re-vwar", usageExample = "Au revoir et bonne soirée !", usageTranslation = "Goodbye and have a good evening!")
            )
            "de" -> listOf(
                FlashcardEntity(userId = userId, languageCode = "de", phrase = "Hallo", translation = "Hello", phonetic = "Hallo", usageExample = "Hallo! Wie geht es dir?", usageTranslation = "Hello! How are you?"),
                FlashcardEntity(userId = userId, languageCode = "de", phrase = "Danke schön", translation = "Thank you dynamic", phonetic = "Dahn-kuh shoen", usageExample = "Danke schön für das Geschenk.", usageTranslation = "Thank you very much for the gift."),
                FlashcardEntity(userId = userId, languageCode = "de", phrase = "Bitte", translation = "Please", phonetic = "Bit-te", usageExample = "Ein Bier, bitte.", usageTranslation = "A beer, please."),
                FlashcardEntity(userId = userId, languageCode = "de", phrase = "Guten Morgen", translation = "Good morning", phonetic = "Goo-ten Mor-gen", usageExample = "Guten Morgen, Schatz!", usageTranslation = "Good morning, darling!"),
                FlashcardEntity(userId = userId, languageCode = "de", phrase = "Wie viel kostet das?", translation = "How much is this?", phonetic = "Vee feel cos-tet das", usageExample = "Wie viel kostet dieses Buch?", usageTranslation = "How much does this book cost?"),
                FlashcardEntity(userId = userId, languageCode = "de", phrase = "Wo ist die Toilette?", translation = "Where is the toilet?", phonetic = "Vo ist dee toy-let-te", usageExample = "Entschuldigung, wo ist die Toilette?", usageTranslation = "Excuse me, where is the toilet?"),
                FlashcardEntity(userId = userId, languageCode = "de", phrase = "Das ist lecker", translation = "That is delicious", phonetic = "Das ist le-cker", usageExample = "Diese Wurst ist lecker!", usageTranslation = "This sausage is delicious!"),
                FlashcardEntity(userId = userId, languageCode = "de", phrase = "Prost!", translation = "Cheers!", phonetic = "Prohst", usageExample = "Prost auf unsere Freundschaft!", usageTranslation = "Cheers to our friendship!"),
                FlashcardEntity(userId = userId, languageCode = "de", phrase = "Auf Wiedersehen", translation = "Goodbye", phonetic = "Owf veeder-zayn", usageExample = "Auf Wiedersehen, bis bald!", usageTranslation = "Goodbye, see you soon!")
            )
            "it" -> listOf(
                FlashcardEntity(userId = userId, languageCode = "it", phrase = "Ciao", translation = "Hello / Goodbye", phonetic = "Chow", usageExample = "Ciao, come stai?", usageTranslation = "Hello, how are you?"),
                FlashcardEntity(userId = userId, languageCode = "it", phrase = "Grazie", translation = "Thank you", phonetic = "Grat-see-eh", usageExample = "Grazie mille per l'ospitalità.", usageTranslation = "Thank you extremely for the hospitality."),
                FlashcardEntity(userId = userId, languageCode = "it", phrase = "Per favore", translation = "Please", phonetic = "Pair fah-voh-ray", usageExample = "Un espresso, per favore.", usageTranslation = "An espresso, please."),
                FlashcardEntity(userId = userId, languageCode = "it", phrase = "Buongiorno", translation = "Good morning", phonetic = "Bwon-jor-no", usageExample = "Buongiorno a tutti!", usageTranslation = "Good morning to everyone!"),
                FlashcardEntity(userId = userId, languageCode = "it", phrase = "Quanto costa?", translation = "How much is this?", phonetic = "Kwan-to cos-tah", usageExample = "Quanto costa questo biglietto?", usageTranslation = "How much is this ticket?"),
                FlashcardEntity(userId = userId, languageCode = "it", phrase = "Dov'è il bagno?", translation = "Where is the bathroom?", phonetic = "Dov-eh il bah-nyo", usageExample = "Scusi, dov'è il bagno?", usageTranslation = "Excuse me, where is the bathroom?"),
                FlashcardEntity(userId = userId, languageCode = "it", phrase = "Delizioso!", translation = "Delicious!", phonetic = "Day-leet-syoh-so", usageExample = "Questo gelato è delizioso!", usageTranslation = "This gelato is delicious!"),
                FlashcardEntity(userId = userId, languageCode = "it", phrase = "Salute!", translation = "Cheers!", phonetic = "Sah-loo-tay", usageExample = "Buon appetito e salute!", usageTranslation = "Enjoy your meal and cheers!"),
                FlashcardEntity(userId = userId, languageCode = "it", phrase = "Arrivederci", translation = "Goodbye", phonetic = "Ah-ree-vay-dair-chee", usageExample = "Arrivederci, signora!", usageTranslation = "Goodbye, ma'am!")
            )
            "ko" -> listOf(
                FlashcardEntity(userId = userId, languageCode = "ko", phrase = "안녕하세요", translation = "Hello", phonetic = "An-nyeong-ha-se-yo", usageExample = "안녕하세요! 오랜만입니다.", usageTranslation = "Hello! Long time no see."),
                FlashcardEntity(userId = userId, languageCode = "ko", phrase = "감사합니다", translation = "Thank you", phonetic = "Gam-sa-ham-ni-da", usageExample = "도와주셔서 대단히 감사합니다.", usageTranslation = "Thank you very much for helping."),
                FlashcardEntity(userId = userId, languageCode = "ko", phrase = "부탁합니다", translation = "Please", phonetic = "Bu-tak-ham-ni-da", usageExample = "이것 좀 부탁합니다.", usageTranslation = "This one, please."),
                FlashcardEntity(userId = userId, languageCode = "ko", phrase = "맛있어요", translation = "It is delicious", phonetic = "Mas-is-seo-yo", usageExample = "이 비빔밥은 정말 맛있어요.", usageTranslation = "This bibimbap is really delicious."),
                FlashcardEntity(userId = userId, languageCode = "ko", phrase = "이게 얼마예요?", translation = "How much is this?", phonetic = "I-ge ol-ma-ye-yo", usageExample = "저기요, 이게 얼마예요?", usageTranslation = "Excuse me, how much is this?"),
                FlashcardEntity(userId = userId, languageCode = "ko", phrase = "화장실 어디예요?", translation = "Where is the bathroom?", phonetic = "Hwa-jang-sil eo-di-ye-yo", usageExample = "실례지만 화장실 어디예요?", usageTranslation = "Pardon me, where is the bathroom?"),
                FlashcardEntity(userId = userId, languageCode = "ko", phrase = "건배!", translation = "Cheers!", phonetic = "Geon-bae", usageExample = "우리의 성공을 위해 건배!", usageTranslation = "Cheers to our success!"),
                FlashcardEntity(userId = userId, languageCode = "ko", phrase = "안녕히 가세요", translation = "Goodbye (to one leaving)", phonetic = "An-nyeong-hi ga-se-yo", usageExample = "조심히 안녕히 가세요.", usageTranslation = "Goodbye and go safely.")
            )
            "zh" -> listOf(
                FlashcardEntity(userId = userId, languageCode = "zh", phrase = "你好", translation = "Hello", phonetic = "Nǐ hǎo", usageExample = "你好！很高兴认识你。", usageTranslation = "Hello! Glad to meet you."),
                FlashcardEntity(userId = userId, languageCode = "zh", phrase = "谢谢", translation = "Thank you", phonetic = "Xièxie", usageExample = "谢谢你送给我的礼物。", usageTranslation = "Thank you for the gift you gave me."),
                FlashcardEntity(userId = userId, languageCode = "zh", phrase = "请", translation = "Please", phonetic = "Qǐng", usageExample = "请进，请坐。", usageTranslation = "Please come in, please sit down."),
                FlashcardEntity(userId = userId, languageCode = "zh", phrase = "这个多少钱？", translation = "How much is this?", phonetic = "Zhège duōshǎo qián?", usageExample = "老板，这个多少钱？", usageTranslation = "Boss, how much does this cost?"),
                FlashcardEntity(userId = userId, languageCode = "zh", phrase = "洗手间在哪儿？", translation = "Where is the toilet?", phonetic = "Xǐshǒujiān zài nǎ'er?", usageExample = "请问，洗手间在哪儿？", usageTranslation = "Excuse me, where is the restroom?"),
                FlashcardEntity(userId = userId, languageCode = "zh", phrase = "好吃", translation = "Delicious / Good to eat", phonetic = "Hǎochī", usageExample = "中国菜真的很好吃！", usageTranslation = "Chinese food is really delicious!"),
                FlashcardEntity(userId = userId, languageCode = "zh", phrase = "干杯", translation = "Cheers!", phonetic = "Gānbēi", usageExample = "大家一起干杯！", usageTranslation = "Cheers together everyone!"),
                FlashcardEntity(userId = userId, languageCode = "zh", phrase = "再见", translation = "Goodbye", phonetic = "Zàijiàn", usageExample = "再见，祝你一路平安！", usageTranslation = "Goodbye, wish you a safe journey!")
            )
            else -> listOf(
                FlashcardEntity(userId = userId, languageCode = languageCode, phrase = "Hello / Greeting", translation = "Standard interactive welcome phrase", phonetic = "[Basic greeting]", usageExample = "Hello! Nice to meet you.", usageTranslation = "Standard friendly start."),
                FlashcardEntity(userId = userId, languageCode = languageCode, phrase = "Thank you", translation = "Expression of gratitude", phonetic = "[Gratitude]", usageExample = "Thank you so much for the support.", usageTranslation = "Expressing standard gratitude."),
                FlashcardEntity(userId = userId, languageCode = languageCode, phrase = "Please", translation = "Expression of polite request", phonetic = "[Request softner]", usageExample = "Please, point standard direction.", usageTranslation = "Softening inquiries."),
                FlashcardEntity(userId = userId, languageCode = languageCode, phrase = "Where is the bathroom?", translation = "Location inquiry for restroom", phonetic = "[Restroom finding query]", usageExample = "Pardon me, where is the bathroom?", usageTranslation = "Important travel query."),
                FlashcardEntity(userId = userId, languageCode = languageCode, phrase = "How much is this?", translation = "Inquiry of transaction cost", phonetic = "[Price query]", usageExample = "Excuse me, how much is this souvenir?", usageTranslation = "Inquiring about prices offline."),
                FlashcardEntity(userId = userId, languageCode = languageCode, phrase = "Delicious", translation = "Excellent tasting food", phonetic = "[Taste compliment]", usageExample = "This local pastry is delicious.", usageTranslation = "Complimenting local cuisine."),
                FlashcardEntity(userId = userId, languageCode = languageCode, phrase = "Cheers!", translation = "Expression used when toasting", phonetic = "[Social toast]", usageExample = "Cheers to a beautiful trip!", usageTranslation = "Friendly celebration phrase."),
                FlashcardEntity(userId = userId, languageCode = languageCode, phrase = "Goodbye", translation = "Standard parting greeting", phonetic = "[Farewell]", usageExample = "Goodbye, safe travels back!", usageTranslation = "Parting respect.")
            )
        }
        flashcardDao.insertFlashcards(seededList)
        return seededList.size
    }
}
