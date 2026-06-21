package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.MainViewModel

@Composable
fun PronunciationScreen(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedLanguage by mainViewModel.selectedLanguage.collectAsState()
    val isTtsReady by mainViewModel.isTtsReady

    val phrase by mainViewModel.pronunciationPhrase
    val translation by mainViewModel.pronunciationTranslation
    val phonetic by mainViewModel.pronunciationPhonetic
    val userSpeechInput by mainViewModel.userSpeechInput
    val assessmentResult by mainViewModel.assessmentResult
    val isAssessing by mainViewModel.isAssessing

    var showSimulatedInputPrompt by remember { mutableStateOf(false) }
    var mockSpokenValue by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWarm)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header
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
                    text = "ACCENT & PRONUNCIATION ESPORTS",
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepGreen
                )
                Text(
                    text = "Pronounce Aloud",
                    style = MaterialTheme.typography.displayLarge,
                    color = DarkOlive,
                    fontSize = 24.sp
                )
            }
        }

        // Active sentence practicing card block
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, SandyBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(LimeGreen, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Practice Accent",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive
                        )
                    }

                    // Native TTS Audio playout helper button!
                    IconButton(
                        onClick = { mainViewModel.speakText(phrase) },
                        modifier = Modifier
                            .background(CreamCard, RoundedCornerShape(12.dp))
                            .testTag("hear_pronunciation")
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Hear Pronunciation",
                            tint = DeepGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // The text
                Text(
                    text = phrase,
                    style = MaterialTheme.typography.headlineMedium,
                    color = DarkOlive,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )

                if (phonetic.isNotBlank()) {
                    Text(
                        text = phonetic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepGreen,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp), color = SandyBorder)

                // Translation
                Text(
                    text = "English Translation:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray
                )
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkOlive,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Speech & Assessment stats
        if (isAssessing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DeepGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Gemini is listening & grading accent...", fontSize = 13.sp, color = TextGray)
                }
            }
        } else if (assessmentResult != null) {
            val result = assessmentResult!!
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = CreamCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SandyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assessment Score",
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive,
                            fontSize = 16.sp
                        )

                        Box(
                            modifier = Modifier
                                .background(
                                    if (result.score >= 80) LimeGreen else SoftPeach,
                                    RoundedCornerShape(12.dp)
                                        )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${result.score}/100",
                                fontWeight = FontWeight.Bold,
                                color = DarkOlive,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Text(
                        text = result.feedback,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkOlive
                    )

                    if (!result.correction.isNullOrBlank()) {
                        Divider(color = SandyBorder)
                        Text(
                            text = "AI Suggested Phonetic Correction:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = DeepGreen
                        )
                        Text(
                            text = result.correction,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkOlive,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Action Buttons Row mapping
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tap to practice speak simulating dialog trigger
            Button(
                onClick = {
                    mockSpokenValue = phrase
                    showSimulatedInputPrompt = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("speak_button"),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = "Mic")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tap to Speak Aloud", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Shuffle challenge phrase
            Button(
                onClick = { mainViewModel.generateNewPronunciationPhrase() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = DeepGreen),
                elevation = null
            ) {
                Text("Skip to New Phrase 🧭", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    // simulated audio recorder dialog helper
    if (showSimulatedInputPrompt) {
        Dialog(onDismissRequest = { showSimulatedInputPrompt = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Simulate Spoken Audio",
                        style = MaterialTheme.typography.titleLarge,
                        color = DarkOlive,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Since this is operating inside a streamed web Android container, you can type your speech transcribing text below or click a perfect copy match to assess pronunciation instantly.",
                        fontSize = 12.sp,
                        color = TextGray
                    )

                    // Quick Match Preset option selection
                    OutlinedButton(
                        onClick = { mockSpokenValue = phrase },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy sentence exactly (Assess Fluent accent)")
                    }

                    OutlinedButton(
                        onClick = { mockSpokenValue = phrase.substring(0, (phrase.length / 2).coerceAtLeast(3)) + "..." },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy partial sentence (Assess Hesitant accent)")
                    }

                    OutlinedTextField(
                        value = mockSpokenValue,
                        onValueChange = { mockSpokenValue = it },
                        label = { Text("Spoken Text Transcript") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepGreen, unfocusedBorderColor = SandyBorder)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showSimulatedInputPrompt = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextGray),
                            elevation = null
                        ) {
                            Text("Dismiss")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (mockSpokenValue.isNotBlank()) {
                                    mainViewModel.assessPronunciation(mockSpokenValue, authViewModel)
                                    showSimulatedInputPrompt = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
                        ) {
                            Text("Assess", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
