package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.DarkOlive
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.LimeGreen
import com.example.ui.theme.PolyglotTheme
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize Daily Streak Reminder notification channels
        com.example.data.notifications.NotificationHelper.initNotificationChannel(this)
        setContent {
            PolyglotTheme {
                val authUiState by authViewModel.uiState.collectAsState()
                var currentScreen by remember { mutableStateOf("home") }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val state = authUiState) {
                        is AuthUiState.Authenticated -> {
                            // Synchronize correct active user parameters to loading states
                            mainViewModel.setUserId(state.user.id)

                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                bottomBar = {
                                    NavigationBar(
                                        containerColor = Color(0xFFF2EFE9),
                                        tonalElevation = 8.dp
                                    ) {
                                        NavigationBarItem(
                                            selected = currentScreen == "home",
                                            onClick = { currentScreen = "home" },
                                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                            label = { Text("Home", fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = DarkOlive,
                                                selectedTextColor = DarkOlive,
                                                indicatorColor = LimeGreen,
                                                unselectedIconColor = DarkOlive.copy(alpha = 0.5f),
                                                unselectedTextColor = DarkOlive.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.testTag("nav_home")
                                        )

                                        NavigationBarItem(
                                            selected = currentScreen == "flashcards",
                                            onClick = { currentScreen = "flashcards" },
                                            icon = { Icon(Icons.Default.Style, contentDescription = "Flashcards") },
                                            label = { Text("Memory", fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = DarkOlive,
                                                selectedTextColor = DarkOlive,
                                                indicatorColor = LimeGreen,
                                                unselectedIconColor = DarkOlive.copy(alpha = 0.5f),
                                                unselectedTextColor = DarkOlive.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.testTag("nav_mem")
                                        )

                                        NavigationBarItem(
                                            selected = currentScreen == "chat",
                                            onClick = { currentScreen = "chat" },
                                            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chat") },
                                            label = { Text("Chat", fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = DarkOlive,
                                                selectedTextColor = DarkOlive,
                                                indicatorColor = LimeGreen,
                                                unselectedIconColor = DarkOlive.copy(alpha = 0.5f),
                                                unselectedTextColor = DarkOlive.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.testTag("nav_chat")
                                        )

                                        NavigationBarItem(
                                            selected = currentScreen == "pronun",
                                            onClick = { currentScreen = "pronun" },
                                            icon = { Icon(Icons.Default.Mic, contentDescription = "Pronunciation") },
                                            label = { Text("Accent", fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = DarkOlive,
                                                selectedTextColor = DarkOlive,
                                                indicatorColor = LimeGreen,
                                                unselectedIconColor = DarkOlive.copy(alpha = 0.5f),
                                                unselectedTextColor = DarkOlive.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.testTag("nav_pronun")
                                        )

                                        NavigationBarItem(
                                            selected = currentScreen == "settings",
                                            onClick = { currentScreen = "settings" },
                                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                            label = { Text("Settings", fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = DarkOlive,
                                                selectedTextColor = DarkOlive,
                                                indicatorColor = LimeGreen,
                                                unselectedIconColor = DarkOlive.copy(alpha = 0.5f),
                                                unselectedTextColor = DarkOlive.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.testTag("nav_settings")
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = { fadeIn() with fadeOut() },
                                    label = "screen_navigation"
                                ) { screen ->
                                    when (screen) {
                                        "home" -> DashboardScreen(
                                            user = state.user,
                                            mainViewModel = mainViewModel,
                                            authViewModel = authViewModel,
                                            onNavigateToFlashcards = { currentScreen = "flashcards" },
                                            onNavigateToChat = { currentScreen = "chat" },
                                            onNavigateToPronun = { currentScreen = "pronun" },
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                        "flashcards" -> FlashcardScreen(
                                            mainViewModel = mainViewModel,
                                            authViewModel = authViewModel,
                                            onBack = { currentScreen = "home" },
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                        "chat" -> ChatScreen(
                                            mainViewModel = mainViewModel,
                                            authViewModel = authViewModel,
                                            onBack = { currentScreen = "home" },
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                        "pronun" -> PronunciationScreen(
                                            mainViewModel = mainViewModel,
                                            authViewModel = authViewModel,
                                            onBack = { currentScreen = "home" },
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                        "settings" -> SettingsScreen(
                                            user = state.user,
                                            mainViewModel = mainViewModel,
                                            authViewModel = authViewModel,
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            // Unauthenticated, Error, or Loading fallbacks
                            LoginScreen(
                                authViewModel = authViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
