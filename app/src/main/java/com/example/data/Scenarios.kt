package com.example.data

data class ConversationScenario(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val initialAiMessagePreset: Map<String, String>,
    val initialAiMessageTranslation: String,
    val systemPrompt: String
) {
    fun getInitialMessageForCode(langCode: String): String {
        return initialAiMessagePreset[langCode] ?: initialAiMessagePreset["en"] ?: "Hello! Let's start practicing."
    }
}

object ScenarioRegistry {
    val scenarios = listOf(
        ConversationScenario(
            id = "restaurant",
            title = "Ordering Food",
            description = "Simulate ordering delicious coffee and food at a local cafe.",
            emoji = "🍔",
            initialAiMessagePreset = mapOf(
                "ja" to "いらっしゃいませ！カフェへようこそ。ご注文はお決まりですか？",
                "es" to "¡Hola! Bienvenidos al café. ¿Qué te gustaría ordenar hoy?",
                "fr" to "Bonjour! Bienvenue au café. Qu'est-ce que vous aimeriez commander aujourd'hui?",
                "de" to "Hallo! Willkommen im Café. Was möchten Sie heute bestellen?",
                "it" to "Ciao! Benvenuti al caffè. Cosa vorresti ordinare oggi?",
                "en" to "Hello! Welcome to the cafe. What would you like to order today?"
            ),
            initialAiMessageTranslation = "Hello! Welcome to the cafe. What would you like to order today?",
            systemPrompt = "You are playing the role of a warm, polite waiter/waitress at a cosy bistro. Address the user, take their order, ask if they want anything else, and provide their total gracefully. Speak purely in the target language. Provide constructive feedback on grammar or spelling directly if there are mistakes, but prioritize flow. Keep replies short (1-3 sentences)."
        ),
        ConversationScenario(
            id = "directions",
            title = "Asking for Directions",
            description = "Ask a friendly local how to find the nearest train station.",
            emoji = "🗺️",
            initialAiMessagePreset = mapOf(
                "ja" to "こんにちは！何かお困りですか？近くに駅を探していますか？",
                "es" to "¡Hola! ¿Necesitas ayuda? ¿Estás buscando la estación de tren más cercana?",
                "fr" to "Bonjour! Est-ce que vous avez besoin d'aide? Vous cherchez la gare la plus proche?",
                "de" to "Hallo! Brauchen Sie Hilfe? Suchen Sie den nächsten Bahnhof?",
                "it" to "Ciao! Hai bisogno di aiuto? Stai cercando la stazione ferroviaria più vicina?",
                "en" to "Hello! Do you need help? Are you looking for the nearest train station?"
            ),
            initialAiMessageTranslation = "Hello! Do you need help? Are you looking for the nearest train station?",
            systemPrompt = "You are playing the role of a friendly local walking on the street. Help the user find the nearest train station, describe simple landmarks, and give them warm walking directions. Speak purely in target language. Keep replies short (1-3 sentences)."
        ),
        ConversationScenario(
            id = "small_talk",
            title = "Making Small Talk",
            description = "Chat about weather, hobbies, and introduce yourself to a new friend.",
            emoji = "💬",
            initialAiMessagePreset = mapOf(
                "ja" to "はじめまして！今日はとてもいい天気ですね。普段は何をするのが好きですか？",
                "es" to "¡Mucho gusto! Hoy hace un clima hermoso. ¿Qué te gusta hacer en tu tiempo libre?",
                "fr" to "Enchanté! Il fait un temps magnifique aujourd'hui. Qu'est-ce que vous aimez faire pendant votre temps libre?",
                "de" to "Schön, Sie kennenzulernen! Heute ist tolles Wetter. Was machen Sie gerne in Ihrer Freizeit?",
                "it" to "Piacere di conoscerti! Oggi c'è un tempo fantastico. Cosa ti piace fare nel tempo libero?",
                "en" to "Nice to meet you! Beautiful weather today. What do you like to do in your free time?"
            ),
            initialAiMessageTranslation = "Nice to meet you! Beautiful weather today. What do you like to do in your free time?",
            systemPrompt = "You are playing the role of a warm, interested classmate or study group buddy. Ask the user about their day, their hobbies, and why they are studying this language. Respond with matching enthusiasm. Speak purely in target language. Keep replies short (1-3 sentences)."
        ),
        ConversationScenario(
            id = "hotel",
            title = "Hotel Check-In",
            description = "Check into your hotel room at the front desk with key cards.",
            emoji = "🏨",
            initialAiMessagePreset = mapOf(
                "ja" to "いらっしゃいませ。ホテルのフロントでございます。ご予約のお名前をお伺いできますか？",
                "es" to "Buenas tardes. Bienvenido a nuestro hotel. ¿Me permite su nombre para la reservación, por favor?",
                "fr" to "Bonjour. Bienvenue à l'hôtel. Puis-je avoir votre nom pour la réservation, s'il vous plaît?",
                "de" to "Guten Tag. Willkommen im Hotel. Kann ich bitte Ihren Namen für die Reservierung haben?",
                "it" to "Buon pomeriggio. Benvenuto nel nostro hotel. Posso avere il suo nome per la prenotazione, per favore?",
                "en" to "Good afternoon. Welcome to our hotel. Can I have your name for the reservation, please?"
            ),
            initialAiMessageTranslation = "Good afternoon. Welcome to our hotel. Can I have your name for the reservation, please?",
            systemPrompt = "You are a professional hotel receptionist at the front lobby desk. Ask for the guest's name, confirm reservation details, explain check-out protocols, and offer baggage assistance. Speak purely in target language. Keep replies short (1-3 sentences)."
        ),
        ConversationScenario(
            id = "shopping",
            title = "Shopping & Bargaining",
            description = "Browse a street boutique and try to negotiate a small savings.",
            emoji = "🛍️",
            initialAiMessagePreset = mapOf(
                "ja" to "こんにちは！このお土産はとても人気ですよ。何かお探しですか？",
                "es" to "¡Hola! Estos recuerdos hechos a mano son muy populares. ¿Buscas algo especial?",
                "fr" to "Bonjour! Ces souvenirs faits main sont très populaires. Vous cherchez quelque chose de spécial?",
                "de" to "Hallo! Diese handgemachten Souvenirs sind sehr beliebt. Suchen Sie etwas Besonderes?",
                "it" to "Ciao! Questi souvenir fatti a mano sono molto popolari. Cerchi qualcosa in speciale?",
                "en" to "Hello! These handmade souvenirs are extremely popular. Are you looking for something special?"
            ),
            initialAiMessageTranslation = "Hello! These handmade souvenirs are extremely popular. Are you looking for something special?",
            systemPrompt = "You are a lively boutique store clerk or street vendor selling handmade crafts or souvenir clothes. Engage with the customer, display various items, handle pricing queries, or offer a slight discount. Speak purely in target language. Keep replies short (1-3 sentences)."
        )
    )

    fun getById(id: String): ConversationScenario? {
        return scenarios.firstOrNull { it.id == id }
    }
}
