package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.FlashcardEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.MainViewModel

// --- OFFLINE-FIRST QUIZ ENGINE STRUCTURES (INDEXEDB / SQLite REPRESENTATION) ---
sealed interface QuizState {
    object Ready : QuizState
    data class Active(
        val questions: List<QuizQuestion>,
        val currentIndex: Int,
        val selectedOptionIndex: Int?,
        val isFirstSelectionCorrect: Boolean,
        val totalCorrect: Int
    ) : QuizState
    data class Complete(
        val totalQuestions: Int,
        val correctCount: Int,
        val xpEarned: Int
    ) : QuizState
}

data class QuizQuestion(
    val phrase: String,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val description: String,
    val isForeignToEnglish: Boolean
)

fun getQuizOptions(
    correctValue: String,
    isForeignTarget: Boolean,
    allCards: List<FlashcardEntity>,
    langCode: String
): List<String> {
    val options = mutableSetOf(correctValue)
    
    // Grab possible options from other local SQLite flashcards
    val pool = allCards.filter { 
        val itemVal = if (isForeignTarget) it.phrase else it.translation
        itemVal != correctValue 
    }.map { if (isForeignTarget) it.phrase else it.translation }.shuffled()
    
    for (item in pool) {
        if (options.size >= 4) break
        options.add(item)
    }
    
    // Add language specific backups if size < 4 (Offline Fallback mechanism)
    if (options.size < 4) {
        val backupPool = when (langCode) {
            "ja" -> if (isForeignTarget) listOf("こんにちは", "ありがとう", "すみません", "美味しい", "お元気ですか？", "さようなら", "乾杯", "トイレはどこですか？") else listOf("Hello / Good afternoon", "Thank you", "Excuse me / Sorry", "Delicious / Tasty", "How are you?", "Goodbye", "Cheers!", "Where is the bathroom?")
            "es" -> if (isForeignTarget) listOf("Hola", "Gracias", "Por favor", "Buenos días", "Buenas noches", "Dónde está el baño", "Me gusta mucho", "Adiós") else listOf("Hello", "Thank you", "Please", "Good morning", "Good night", "Where is the bathroom?", "I like it a lot", "Goodbye")
            "fr" -> if (isForeignTarget) listOf("Bonjour", "Merci beaucoup", "S'il vous plaît", "Enchanté", "Où sont les toilettes?", "C'est délicieux", "Excusez-moi", "Au revoir") else listOf("Hello / Good morning", "Thank you very much", "Please", "Nice to meet you", "Where is the bathroom?", "It is delicious", "Excuse me", "Goodbye")
            "de" -> if (isForeignTarget) listOf("Hallo", "Danke schön", "Bitte", "Guten Morgen", "Wo ist die Toilette?", "Das ist lecker", "Prost!", "Auf Wiedersehen") else listOf("Hello", "Thank you dynamic", "Please", "Good morning", "Where is the toilet?", "That is delicious", "Cheers!", "Goodbye")
            "it" -> if (isForeignTarget) listOf("Ciao", "Grazie", "Per favore", "Buongiorno", "Quanto costa?", "Dov'è il bagno?", "Delizioso!", "Arrivederci") else listOf("Hello / Goodbye", "Thank you", "Please", "Good morning", "How much is this?", "Where is the bathroom?", "Delicious!", "Goodbye")
            "ko" -> if (isForeignTarget) listOf("안녕하세요", "감사합니다", "부탁합니다", "맛있어요", "이게 얼마예요?", "화장실 어디예요?", "건배!", "안녕히 가세요") else listOf("Hello", "Thank you", "Please", "It is delicious", "How much is this?", "Where is the bathroom?", "Cheers!", "Goodbye (to one leaving)")
            "zh" -> if (isForeignTarget) listOf("你好", "谢谢", "请", "这个多少钱？", "洗手间在哪儿？", "好吃", "干杯", "再见") else listOf("Hello", "Thank you", "Please", "How much is this?", "Where is the toilet?", "Delicious / Good to eat", "Cheers!", "Goodbye")
            else -> if (isForeignTarget) listOf("Greeting Word", "Appreciation", "Request", "Question Word", "Farewell Word") else listOf("Hello / Greeting", "Thank you", "Please", "Where is the bathroom?", "Goodbye")
        }
        for (bk in backupPool.shuffled()) {
            if (options.size >= 4) break
            if (bk != correctValue) {
                options.add(bk)
            }
        }
    }
    
    // Absolute minimum fallback to guarantee 4 option elements
    var index = 1
    while (options.size < 4) {
        options.add(if (isForeignTarget) "Option_${index++}" else "TranslationOption_${index++}")
    }
    
    return options.toList().shuffled()
}

fun generateQuestions(allCards: List<FlashcardEntity>, langCode: String): List<QuizQuestion> {
    if (allCards.isEmpty()) return emptyList()
    val targetCards = allCards.shuffled().take(5)
    
    return targetCards.map { card ->
        val isForeignToEnglish = (0..1).random() == 0
        val correctValue = if (isForeignToEnglish) card.translation else card.phrase
        val questionText = if (isForeignToEnglish) {
            "What is the correct English translation for this phrase?"
        } else {
            "How do you translate this English word/phrase to the target language?"
        }
        
        val options = getQuizOptions(correctValue, !isForeignToEnglish, allCards, langCode)
        val correctIdx = options.indexOf(correctValue).coerceAtLeast(0)
        
        QuizQuestion(
            phrase = if (isForeignToEnglish) card.phrase else card.translation,
            questionText = questionText,
            options = options,
            correctIndex = correctIdx,
            description = buildString {
                if (!card.phonetic.isNullOrBlank()) append("Phonetic: ${card.phonetic}\n")
                if (!card.usageExample.isNullOrBlank()) append("Example: ${card.usageExample}\n➔ ${card.usageTranslation ?: ""}")
            },
            isForeignToEnglish = isForeignToEnglish
        )
    }
}

@Composable
fun FlashcardScreen(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedLanguage by mainViewModel.selectedLanguage.collectAsState()
    val flashcards by mainViewModel.flashcards.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    // Flashcard Quiz Study State
    var reviewIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    // Flip degree offset
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWarm)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkOlive)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "MEMORY DECKS",
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepGreen
                )
                Text(
                    text = "${selectedLanguage.name} Flashcards",
                    style = MaterialTheme.typography.displayLarge,
                    color = DarkOlive,
                    fontSize = 24.sp
                )
            }
        }

        // --- OFFLINE ROOM/SQLITE DATA SYNC CENTER (INDEXEDB EQUIVALENT) ---
        var isOfflinePanelExpanded by remember { mutableStateOf(false) }
        val isPreseedingOffline by mainViewModel.isPreseedingOffline.collectAsState()
        val preseedEventMessage by mainViewModel.preseedEventMessage.collectAsState()
        val isLoadingFlashcards by mainViewModel.isLoadingFlashcards.collectAsState()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SandyBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SoftPeach),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isOfflinePanelExpanded = !isOfflinePanelExpanded }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📡", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "Offline Storage Profile",
                                fontWeight = FontWeight.Bold,
                                color = DarkOlive,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "IndexedDB / SQLite Engine Mapped",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Badge
                    Box(
                        modifier = Modifier
                            .background(
                                if (flashcards.isNotEmpty()) DeepGreen else Color(0xFFD27D2D),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (flashcards.isNotEmpty()) "Ready (Offline)" else "Seed Required",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isOfflinePanelExpanded) {
                    Divider(color = SandyBorder)
                    
                    Text(
                        text = "Native Android uses SQLite via Room Persistence library as the high-performance local equivalence of browser-based IndexedDB. Core lesson data is safely stored on your device to support fully offline spaced-repetition dictionary reviews.",
                        fontSize = 11.sp,
                        color = DarkOlive,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Stats Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Language", fontSize = 9.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Text(selectedLanguage.name, fontSize = 11.sp, color = DarkOlive, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Local Storage", fontSize = 9.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Text("SQLite (IndexedDB)", fontSize = 11.sp, color = DarkOlive, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("In-Cache Count", fontSize = 9.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Text("${flashcards.size} Items", fontSize = 11.sp, color = DarkOlive, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isPreseedingOffline || isLoadingFlashcards) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = DeepGreen,
                            trackColor = SandyBorder
                        )
                    }

                    // Success or error alerts
                    if (preseedEventMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgWarm, RoundedCornerShape(12.dp))
                                .border(1.dp, SandyBorder, RoundedCornerShape(12.dp))
                                .clickable { mainViewModel.clearPreseedMessage() }
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preseedEventMessage ?: "",
                                    fontSize = 11.sp,
                                    color = DeepGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("Dismiss", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { mainViewModel.triggerPreseedOfflineVocabulary() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = LimeGreen, contentColor = DarkOlive),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isPreseedingOffline
                        ) {
                            Text("Pre-seed Offline Pack", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { mainViewModel.triggerAiDeckGeneration() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoadingFlashcards
                        ) {
                            Text("Sync AI Deck", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = "▼ Tap for local db sync, pre-seeding target vocab & status.",
                        fontSize = 10.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        var currentTab by remember { mutableStateOf("deck") } // "deck" or "quiz"
        var autoPlayEnabled by remember { mutableStateOf(true) }
        var quizState by remember { mutableStateOf<QuizState>(QuizState.Ready) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEFECE6), RoundedCornerShape(16.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (currentTab == "deck") Color.White else Color.Transparent)
                    .clickable { currentTab = "deck" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Study Cards 📇",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentTab == "deck") DarkOlive else DarkOlive.copy(alpha = 0.6f)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (currentTab == "quiz") Color.White else Color.Transparent)
                    .clickable { currentTab = "quiz" }
                    .padding(vertical = 10.dp)
                    .testTag("quiz_tab_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Offline Quiz 🏆",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentTab == "quiz") DarkOlive else DarkOlive.copy(alpha = 0.6f)
                )
            }
        }

        if (currentTab == "deck") {
            // Display current active card
            if (flashcards.isNotEmpty()) {
                val safeIndex = reviewIndex.coerceIn(0, flashcards.size - 1)
                val activeCard = flashcards[safeIndex]

                LaunchedEffect(activeCard) {
                    if (autoPlayEnabled) {
                        mainViewModel.speakText(activeCard.phrase)
                    }
                }

                // Progress header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Card ${safeIndex + 1} of ${flashcards.size}",
                        fontWeight = FontWeight.Bold,
                        color = TextGray,
                        fontSize = 14.sp
                    )

                    // Auto-play Toggle Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (autoPlayEnabled) DeepGreen.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { autoPlayEnabled = !autoPlayEnabled }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("autoplay_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Autoplay Audio Toggle",
                            tint = if (autoPlayEnabled) DeepGreen else TextGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (autoPlayEnabled) "Auto ON" else "Auto OFF",
                            color = if (autoPlayEnabled) DeepGreen else TextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(LimeGreen, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Box ${activeCard.box}/5",
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive,
                            fontSize = 11.sp
                        )
                    }
                }

                // Interactive Flippable Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 8 * density
                        }
                        .clickable { isFlipped = !isFlipped }
                        .background(Color.White, RoundedCornerShape(32.dp))
                        .border(1.dp, SandyBorder, RoundedCornerShape(32.dp))
                        .testTag("flip_card"),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        // FRONT STATE
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = selectedLanguage.flag,
                                fontSize = 44.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeCard.phrase,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = DarkOlive,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        mainViewModel.speakText(activeCard.phrase)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(DeepGreen.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                        .testTag("speak_front_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Speak Phrase",
                                        tint = DeepGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            if (!activeCard.phonetic.isNullOrBlank()) {
                                Text(
                                    text = activeCard.phonetic,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "(Tap to Flip Card)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                        }
                    } else {
                        // BACK STATE (Mirror horizontally so text remains normal!)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .graphicsLayer { rotationY = 180f }
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(SoftPeach, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("TRANSLATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkOlive)
                            }

                            Text(
                                text = activeCard.translation,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkOlive,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(DeepGreen.copy(alpha = 0.1f))
                                    .clickable { mainViewModel.speakText(activeCard.phrase) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("speak_back_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Pronounce Original",
                                    tint = DeepGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = activeCard.phrase,
                                    color = DeepGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!activeCard.usageExample.isNullOrBlank()) {
                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = SandyBorder)
                                Text(
                                    text = "Usage Example:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = DeepGreen
                                )
                                Text(
                                    text = activeCard.usageExample,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DarkOlive,
                                    textAlign = TextAlign.Center
                                )
                                if (!activeCard.usageTranslation.isNullOrBlank()) {
                                    Text(
                                        text = activeCard.usageTranslation,
                                        fontSize = 13.sp,
                                        color = TextGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Leitner Grading buttons options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Still Learning/Incorrect
                    Button(
                        onClick = {
                            isFlipped = false
                            mainViewModel.gradeFlashcardReview(activeCard, false, authViewModel)
                            reviewIndex = (reviewIndex + 1) % flashcards.size
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftPeach, contentColor = DarkOlive),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text("Needs Review ❌", fontWeight = FontWeight.Bold)
                    }

                    // Mastered/Correct
                    Button(
                        onClick = {
                            isFlipped = false
                            mainViewModel.gradeFlashcardReview(activeCard, true, authViewModel)
                            reviewIndex = (reviewIndex + 1) % flashcards.size
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text("I Knew It! ✅", fontWeight = FontWeight.Bold)
                    }
                }

                // Delete current card button
                TextButton(
                    onClick = {
                        mainViewModel.deleteCard(activeCard)
                        reviewIndex = 0
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Card from Deck", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

            } else {
                // Empty Deck Screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(32.dp))
                        .border(2.dp, SandyBorder, RoundedCornerShape(32.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📁", fontSize = 48.sp)
                        Text(
                            text = "Deck is Empty",
                            style = MaterialTheme.typography.titleLarge,
                            color = DarkOlive,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Generate 5 AI vocabulary lists using the daily challenge challenge on your home screen or add custom flashcards manually step-by-step.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // Add Card Floating action button
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LimeGreen, contentColor = DarkOlive),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Custom Card", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // --- OFFLINE-FIRST QUIZ ENGINE UI ---
            when (val state = quizState) {
                is QuizState.Ready -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(32.dp))
                            .border(1.dp, SandyBorder, RoundedCornerShape(32.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🏆",
                                fontSize = 64.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Offline Vocabulary Quiz",
                                style = MaterialTheme.typography.titleLarge,
                                color = DarkOlive,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Test your knowledge offline! The engine pulls directly from your local SQLite cache (equivalent of IndexedDB).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Divider(color = SandyBorder, modifier = Modifier.padding(vertical = 4.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚡", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("5 Multiple-choice questions", fontSize = 13.sp, color = DarkOlive, fontWeight = FontWeight.SemiBold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔁", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Bidirectional testing format", fontSize = 13.sp, color = DarkOlive, fontWeight = FontWeight.SemiBold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💎", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Earn +5 XP per correct answer!", fontSize = 13.sp, color = DarkOlive, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (flashcards.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SoftPeach, RoundedCornerShape(16.dp))
                                        .border(1.dp, SandyBorder, RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("⚠️ Local Deck is Empty", fontWeight = FontWeight.Bold, color = DarkOlive, fontSize = 13.sp)
                                        Text(
                                            "Please expand the 'Offline Storage Profile' panel above and click 'Pre-seed Offline Pack' to load standard local cards.",
                                            color = TextGray,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val questions = generateQuestions(flashcards, selectedLanguage.code)
                                        if (questions.isNotEmpty()) {
                                            quizState = QuizState.Active(
                                                questions = questions,
                                                currentIndex = 0,
                                                selectedOptionIndex = null,
                                                isFirstSelectionCorrect = false,
                                                totalCorrect = 0
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("start_offline_quiz_button")
                                ) {
                                    Text("Start Offline Quiz", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
                is QuizState.Active -> {
                    val currentQuestion = state.questions[state.currentIndex]
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(32.dp))
                            .border(1.dp, SandyBorder, RoundedCornerShape(32.dp))
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Quiz Progress details
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Question ${state.currentIndex + 1} of ${state.questions.size}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextGray
                                    )
                                    TextButton(
                                        onClick = { quizState = QuizState.Ready },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Quit ❌", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Progress Bar
                                val frac = (state.currentIndex + 1).toFloat() / state.questions.size.toFloat()
                                val animatedFrac by animateFloatAsState(
                                    targetValue = frac,
                                    animationSpec = tween(durationMillis = 400),
                                    label = "QuizProgress"
                                )
                                LinearProgressIndicator(
                                    progress = animatedFrac,
                                    color = DeepGreen,
                                    trackColor = Color(0xFFECECE6),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }

                            // Target Word display area
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BgWarm, RoundedCornerShape(20.dp))
                                    .border(1.dp, SandyBorder, RoundedCornerShape(20.dp))
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedLanguage.flag,
                                        fontSize = 28.sp
                                    )
                                    
                                    val foreignWordToSpeak = if (currentQuestion.isForeignToEnglish) {
                                        currentQuestion.phrase
                                    } else {
                                        currentQuestion.options.getOrNull(currentQuestion.correctIndex) ?: ""
                                    }
                                    
                                    if (foreignWordToSpeak.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                mainViewModel.speakText(foreignWordToSpeak)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(DeepGreen.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                                                .testTag("speak_quiz_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Speak Target Word",
                                                tint = DeepGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = currentQuestion.phrase,
                                    fontSize = 20.sp,
                                    color = DarkOlive,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = currentQuestion.questionText,
                                    fontSize = 11.sp,
                                    color = TextGray,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Options Block
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                currentQuestion.options.forEachIndexed { optIndex, optionText ->
                                    val isSelected = state.selectedOptionIndex == optIndex
                                    val isCorrectOpt = currentQuestion.correctIndex == optIndex
                                    
                                    val btnBg = when {
                                        state.selectedOptionIndex == null -> Color.White
                                        isCorrectOpt -> Color(0xFFD1E7DD)
                                        isSelected -> Color(0xFFF8D7DA)
                                        else -> Color.White.copy(alpha = 0.5f)
                                    }
                                    val btnBorderColor = when {
                                        state.selectedOptionIndex == null -> SandyBorder
                                        isCorrectOpt -> Color(0xFF0F5132)
                                        isSelected -> Color(0xFF842029)
                                        else -> SandyBorder.copy(alpha = 0.5f)
                                    }
                                    val textStyleColor = when {
                                        state.selectedOptionIndex == null -> DarkOlive
                                        isCorrectOpt -> Color(0xFF0F5132)
                                        isSelected -> Color(0xFF842029)
                                        else -> DarkOlive.copy(alpha = 0.4f)
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = state.selectedOptionIndex == null) {
                                                val isCorrect = optIndex == currentQuestion.correctIndex
                                                quizState = state.copy(
                                                    selectedOptionIndex = optIndex,
                                                    isFirstSelectionCorrect = isCorrect,
                                                    totalCorrect = if (isCorrect) state.totalCorrect + 1 else state.totalCorrect
                                                )
                                            }
                                            .border(1.dp, btnBorderColor, RoundedCornerShape(12.dp)),
                                        colors = CardDefaults.cardColors(containerColor = btnBg),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = optionText,
                                                color = textStyleColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Start,
                                                modifier = Modifier.weight(1f)
                                            )
                                            
                                            if (state.selectedOptionIndex != null) {
                                                when {
                                                    isCorrectOpt -> {
                                                        Text("✅ Correct", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F5132))
                                                    }
                                                    isSelected -> {
                                                        Text("❌ Selected", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF842029))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Explanation and action button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.selectedOptionIndex != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                            Text(
                                                text = if (state.isFirstSelectionCorrect) "Correct answer! 🎉" else "Incorrect choice",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (state.isFirstSelectionCorrect) Color(0xFF0F5132) else Color(0xFF842029)
                                            )
                                            if (currentQuestion.description.isNotBlank()) {
                                                Text(
                                                    text = currentQuestion.description.replace("\n", " ").take(60),
                                                    fontSize = 10.sp,
                                                    color = TextGray,
                                                    lineHeight = 11.sp
                                                )
                                            }
                                        }

                                        val isLast = state.currentIndex == state.questions.size - 1
                                        Button(
                                            onClick = {
                                                if (isLast) {
                                                    val xp = state.totalCorrect * 5
                                                    authViewModel.awardXp(xp, "Offline Quiz")
                                                    quizState = QuizState.Complete(
                                                        totalQuestions = state.questions.size,
                                                        correctCount = state.totalCorrect,
                                                        xpEarned = xp
                                                    )
                                                } else {
                                                    quizState = state.copy(
                                                        currentIndex = state.currentIndex + 1,
                                                        selectedOptionIndex = null,
                                                        isFirstSelectionCorrect = false
                                                    )
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (isLast) "Finish 🏆" else "Next ➡️",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Select one answer to verify!",
                                        fontSize = 12.sp,
                                        color = TextGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                is QuizState.Complete -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(32.dp))
                            .border(1.dp, SandyBorder, RoundedCornerShape(32.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val pct = if (state.totalQuestions > 0) (state.correctCount * 100) / state.totalQuestions else 0
                            val trophy = when {
                                pct >= 80 -> "🏆🥇"
                                pct >= 40 -> "🥈"
                                else -> "🥉"
                            }
                            
                            Text(text = trophy, fontSize = 64.sp)

                            Text(
                                text = "Quiz Master Complete!",
                                style = MaterialTheme.typography.titleLarge,
                                color = DarkOlive,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "You scored ${state.correctCount} / ${state.totalQuestions} correct.\nThat is a score of $pct% accuracy!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Box(
                                modifier = Modifier
                                    .background(LimeGreen, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "+${state.xpEarned} XP Awarded! 💎",
                                    color = DarkOlive,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        quizState = QuizState.Ready
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftPeach, contentColor = DarkOlive),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Reset", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        val questions = generateQuestions(flashcards, selectedLanguage.code)
                                        if (questions.isNotEmpty()) {
                                            quizState = QuizState.Active(
                                                questions = questions,
                                                currentIndex = 0,
                                                selectedOptionIndex = null,
                                                isFirstSelectionCorrect = false,
                                                totalCorrect = 0
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Retry Quiz", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Custom Card input fields dialog
    if (showAddDialog) {
        var phraseInput by remember { mutableStateOf("") }
        var translationInput by remember { mutableStateOf("") }
        var phoneticInput by remember { mutableStateOf("") }
        var usageInput by remember { mutableStateOf("") }
        var usageTransInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Custom Card",
                        style = MaterialTheme.typography.titleLarge,
                        color = DarkOlive,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = phraseInput,
                        onValueChange = { phraseInput = it },
                        label = { Text("Phrase (${selectedLanguage.name})") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepGreen, unfocusedBorderColor = SandyBorder)
                    )

                    OutlinedTextField(
                        value = translationInput,
                        onValueChange = { translationInput = it },
                        label = { Text("English Translation") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepGreen, unfocusedBorderColor = SandyBorder)
                    )

                    OutlinedTextField(
                        value = phoneticInput,
                        onValueChange = { phoneticInput = it },
                        label = { Text("Phonetic Pronunciation (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepGreen, unfocusedBorderColor = SandyBorder)
                    )

                    OutlinedTextField(
                        value = usageInput,
                        onValueChange = { usageInput = it },
                        label = { Text("Usage Sentence in ${selectedLanguage.name} (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepGreen, unfocusedBorderColor = SandyBorder)
                    )

                    OutlinedTextField(
                        value = usageTransInput,
                        onValueChange = { usageTransInput = it },
                        label = { Text("Usage translation (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepGreen, unfocusedBorderColor = SandyBorder)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showAddDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextGray),
                            elevation = null
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (phraseInput.isNotBlank() && translationInput.isNotBlank()) {
                                    mainViewModel.addNewFlashcard(
                                        phrase = phraseInput,
                                        translation = translationInput,
                                        phonetic = phoneticInput.takeIf { it.isNotBlank() },
                                        usage = usageInput.takeIf { it.isNotBlank() },
                                        usageTranslation = usageTransInput.takeIf { it.isNotBlank() }
                                    )
                                    showAddDialog = false
                                    reviewIndex = 0
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
                        ) {
                            Text("Save Card", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
