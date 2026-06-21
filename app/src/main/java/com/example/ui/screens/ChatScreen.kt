package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ConversationScenario
import com.example.data.ScenarioRegistry
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ChatScreen(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedLanguage by mainViewModel.selectedLanguage.collectAsState()
    val chatMessages by mainViewModel.chatMessages.collectAsState()
    val isSendingChat by mainViewModel.isSendingChat.collectAsState()
    val activeScenario by mainViewModel.activeScenario.collectAsState()

    var userTextInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Voice practice simulation dialog states
    var showVoiceInputDial by remember { mutableStateOf(false) }
    var mockVoiceTranscriptText by remember { mutableStateOf("") }

    // Scroll to the latest bubble automatically
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWarm)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App bar Header
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
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "IMMERSIVE CONVERSATION PRACTICE",
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepGreen,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "AI Language Partner",
                    style = MaterialTheme.typography.displayLarge,
                    color = DarkOlive,
                    fontSize = 20.sp
                )
            }
            IconButton(
                onClick = { mainViewModel.clearChatHistory() },
                modifier = Modifier.background(SoftPeach, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = Color.Red.copy(alpha = 0.8f))
            }
        }

        // 1. Scenario Hub Row Selector (Shows when no active scenario)
        AnimatedVisibility(
            visible = activeScenario == null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🎭 Select a Simulated Scenario to Practice:",
                    fontWeight = FontWeight.Bold,
                    color = DarkOlive,
                    fontSize = 14.sp
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 6.dp)
                ) {
                    items(ScenarioRegistry.scenarios) { sc ->
                        Card(
                            onClick = { mainViewModel.startScenario(sc) },
                            modifier = Modifier
                                .width(200.dp)
                                .height(115.dp)
                                .testTag("scenario_card_${sc.id}"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CreamCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SandyBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sc.emoji, fontSize = 20.sp)
                                    Text(
                                        text = sc.title,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkOlive,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = sc.description,
                                    color = TextGray,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Ongoing Scenario Action Banner (Shows when practicing a scenario)
        AnimatedVisibility(
            visible = activeScenario != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            activeScenario?.let { sc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepGreen),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1.0f)
                        ) {
                            Text(sc.emoji, fontSize = 24.sp)
                            Column {
                                Text(
                                    text = "PRACTICING SCENARIO:",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = sc.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Button(
                            onClick = { mainViewModel.exitScenario() },
                            colors = ButtonDefaults.buttonColors(containerColor = LimeGreen, contentColor = DarkOlive),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.ExitToApp, contentDescription = "Exit", modifier = Modifier.size(14.dp))
                                Text("End Practice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Messages Box container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White, RoundedCornerShape(28.dp))
                .border(1.dp, SandyBorder, RoundedCornerShape(28.dp))
                .padding(14.dp)
        ) {
            if (chatMessages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💬", fontSize = 44.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Text(
                        text = "Commence Conversations",
                        fontWeight = FontWeight.Bold,
                        color = DarkOlive,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Select a simulated scenario above or start free-form greetings in ${selectedLanguage.name}. Tap any bubble to view English translations or speak aloud using the mic icon.",
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chatMessages) { chat ->
                        val isUser = chat.sender == "user"
                        var showTranslationState by remember { mutableStateOf(false) }
                        var showFeedbackState by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                if (!isUser) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(LimeGreen)
                                            .align(Alignment.Bottom),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🤖", fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Column(
                                    modifier = Modifier
                                        .widthIn(max = 240.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 20.dp,
                                                topEnd = 20.dp,
                                                bottomStart = if (isUser) 20.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 20.dp
                                            )
                                        )
                                        .background(if (isUser) DeepGreen else CreamCard)
                                        .clickable { showTranslationState = !showTranslationState }
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = chat.content,
                                        color = if (isUser) Color.White else DarkOlive,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    // Translation text
                                    if (!isUser && !chat.translation.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (showTranslationState) chat.translation else "(Tap to translate)",
                                            color = if (showTranslationState) TextGray.copy(alpha = 0.8f) else DeepGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (isUser) {
                                        Text(
                                            text = "(User spoken/typed)",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                // Interactive voice-based speaker button next to AI response
                                if (!isUser) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { mainViewModel.speakText(chat.content) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(CreamCard, CircleShape)
                                            .border(1.dp, SandyBorder, CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.VolumeUp,
                                            contentDescription = "Speak Text",
                                            tint = DeepGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Dynamic grammar & pronunciation corrections details
                            if (!isUser && !chat.feedback.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .padding(start = 40.dp)
                                        .clickable { showFeedbackState = !showFeedbackState },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.School,
                                        contentDescription = "Advisor Details",
                                        tint = DeepGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (showFeedbackState) "Hide AI Feedback 🎓" else "View Grammar & Pronunciation Tips 🎓",
                                        fontSize = 11.sp,
                                        color = DeepGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                AnimatedVisibility(
                                    visible = showFeedbackState,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp, start = 40.dp, end = 12.dp),
                                        colors = CardDefaults.cardColors(containerColor = CreamCard),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SandyBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "AI Grammar & Accent Analysis:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepGreen
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = chat.feedback!!,
                                                fontSize = 11.sp,
                                                color = DarkOlive,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isSendingChat) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🤖 AI Partner thinking...", fontSize = 11.sp, color = TextGray)
                            }
                        }
                    }
                }
            }
        }

        // Send message interactive keyboard and voice footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Voice record simulation option (microphone icon)
            IconButton(
                onClick = {
                    mockVoiceTranscriptText = ""
                    showVoiceInputDial = true
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(LimeGreen, RoundedCornerShape(18.dp))
                    .border(1.dp, SandyBorder.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .testTag("voice_chat_button")
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Simulate Voice Input",
                    tint = DarkOlive,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Text Entry
            OutlinedTextField(
                value = userTextInput,
                onValueChange = { userTextInput = it },
                placeholder = { Text("Converse in ${selectedLanguage.name}...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepGreen,
                    unfocusedBorderColor = SandyBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = DarkOlive,
                    unfocusedTextColor = DarkOlive
                ),
                singleLine = true
            )

            // Submit Button
            IconButton(
                onClick = {
                    if (userTextInput.isNotBlank()) {
                        mainViewModel.sendChatMessage(userTextInput, authViewModel)
                        userTextInput = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(DeepGreen, RoundedCornerShape(18.dp))
                    .testTag("send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send Message", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }

    // Voice Simulation Dialog
    if (showVoiceInputDial) {
        Dialog(onDismissRequest = { showVoiceInputDial = false }) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎤 Voice-Based Practice",
                            style = MaterialTheme.typography.titleMedium,
                            color = DarkOlive,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showVoiceInputDial = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray)
                        }
                    }

                    Text(
                        text = "Because web-simulated environments have sandboxed microphones, use this console to practice speaking aloud. Tap below to speak, or type/dictate your text:",
                        fontSize = 11.sp,
                        color = TextGray,
                        lineHeight = 15.sp
                    )

                    // Helper sample voice phrases
                    activeScenario?.let { sc ->
                        Text(
                            text = "💡 Practice phrases for this scenario (${sc.title}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DeepGreen
                        )
                        val advicePhrases = when(sc.id) {
                            "restaurant" -> listOf(
                                "I would like coffee and a croissant please." to "お願いします (Please/Request)",
                                "How much does it cost?" to "¿Cuánto cuesta?"
                            )
                            "directions" -> listOf(
                                "Where is the train station?" to "¿Dónde está la estación?",
                                "Please help me find the path." to "道を教えてください (Teach me the path)"
                            )
                            "small_talk" -> listOf(
                                "Nice to meet you! My name is user." to "はじめまして、よろしく (Nice to meet you)",
                                "I love studying languages." to "Me encanta estudiar idiomas."
                            )
                            else -> listOf(
                                "Hello! Pleased to meet you." to "Hola! Encantado de conocerte."
                            )
                        }
                        advicePhrases.forEach { (englishText, targetHint) ->
                            Card(
                                onClick = { mockVoiceTranscriptText = targetHint },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = CreamCard),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SandyBorder.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = DeepGreen, modifier = Modifier.size(16.dp))
                                    Column {
                                        Text(text = englishText, fontSize = 10.sp, color = TextGray)
                                        Text(text = targetHint, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkOlive)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = mockVoiceTranscriptText,
                        onValueChange = { mockVoiceTranscriptText = it },
                        label = { Text("Spoken Voice Transcription Transcript", fontSize = 12.sp) },
                        placeholder = { Text("Practice saying it, then type/copy here...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepGreen,
                            unfocusedBorderColor = SandyBorder,
                            focusedContainerColor = BgWarm,
                            unfocusedContainerColor = BgWarm
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showVoiceInputDial = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextGray),
                            elevation = null
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (mockVoiceTranscriptText.isNotBlank()) {
                                    mainViewModel.sendChatMessage(mockVoiceTranscriptText, authViewModel)
                                    showVoiceInputDial = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Check, contentDescription = "Enter Voice", modifier = Modifier.size(14.dp))
                                Text("Submit Spoken Text", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
