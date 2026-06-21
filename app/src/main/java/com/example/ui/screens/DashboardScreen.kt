package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.UserEntity
import com.example.data.database.StudyLogEntity
import com.example.data.repository.StreakReward
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.MainViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.widget.Toast
import android.util.Log
import com.example.data.notifications.NotificationHelper
import com.google.firebase.messaging.FirebaseMessaging
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing

@Composable
fun DashboardScreen(
    user: UserEntity,
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onNavigateToFlashcards: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToPronun: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedLanguage by mainViewModel.selectedLanguage.collectAsState()
    val flashcardCount by mainViewModel.flashcardCount.collectAsState()
    val masteredCount by mainViewModel.masteredCount.collectAsState()
    val isLoadingFlashcards by mainViewModel.isLoadingFlashcards.collectAsState()

    val leaderboard by authViewModel.leaderboardFlow.collectAsState(initial = emptyList())
    var activeTab by remember { mutableStateOf("learn") }

    var showLangSelector by remember { mutableStateOf(false) }

    val streakReward by authViewModel.streakRewardFlow.collectAsState()
    val studyLogs by authViewModel.studyLogsFlow.collectAsState(initial = emptyList())

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    val todayMinutes = remember(studyLogs) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayLogs = studyLogs.filter { log ->
            sdf.format(Date(log.timestamp)) == todayStr
        }
        todayLogs.sumOf { it.durationSeconds } / 60.0
    }
    val dailyProgressFraction = remember(todayMinutes, user.dailyGoalMinutes) {
        if (user.dailyGoalMinutes <= 0) 1.0f else (todayMinutes.toFloat() / user.dailyGoalMinutes.toFloat()).coerceIn(0f, 1f)
    }
    val animatedDailyProgressFraction by animateFloatAsState(
        targetValue = dailyProgressFraction,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "DailyProgress"
    )
    val goalCompletedToday = todayMinutes >= user.dailyGoalMinutes

    if (streakReward != null) {
        StreakCelebrationDialog(
            reward = streakReward!!,
            onDismiss = { authViewModel.clearStreakReward() }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWarm)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming header matching "Natural Tones" spec
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "YOUR PATH",
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepGreen
                )
                Text(
                    text = "Linguist",
                    style = MaterialTheme.typography.displayLarge,
                    color = DarkOlive
                )
            }

            // Customizable avatar / Google email initials bubble
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE6E2D3))
                    .border(2.dp, DeepGreen, CircleShape)
                    .clickable { authViewModel.logout() },
                contentAlignment = Alignment.Center
            ) {
                // Return emoji or initials
                Text(
                    text = user.avatarEmoji,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Sub-navigation Tabs Row (Modern capsule selector)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CreamCard, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "learn" to "🌱 Study",
                "league" to "🏆 League",
                "analytics" to "📊 Chart",
                "badges" to "🏅 Badges",
                "reminders" to "🔔 Alerts"
            ).forEach { (tabId, label) ->
                val isSelected = activeTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) LimeGreen else Color.Transparent)
                        .clickable { activeTab = tabId }
                        .padding(vertical = 10.dp)
                        .testTag("tab_$tabId"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isSelected) DarkOlive else TextGray
                    )
                }
            }
        }

        if (activeTab == "learn") {
            // DeepGreen Rounded Course Card
            Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .testTag("course_card"),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = DeepGreen, contentColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Current Course",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${selectedLanguage.name} ${selectedLanguage.flag}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Level: L${user.level}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Dynamic Progress Indicators based on study minutes goal
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Study Goal Progress",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${todayMinutes.toInt()}m / ${user.dailyGoalMinutes}m goal (${(dailyProgressFraction * 100).toInt()}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = animatedDailyProgressFraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = LimeGreen,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // Streak & Total Languages grids
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Streak Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = WarmCard),
                border = BorderStroke(1.dp, SandyBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (goalCompletedToday) LimeGreen else Color(0xFFE6E2D3), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (goalCompletedToday) "🔥" else "⏳", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "${user.streak}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Day Streak",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreen
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (goalCompletedToday) LimeGreen.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.3f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (goalCompletedToday) "Met" else "Goal: ${user.dailyGoalMinutes}m",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (goalCompletedToday) DeepGreen else TextGray
                                )
                            }
                        }
                    }
                }
            }

            // Choose Language Selector Toggle Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .clickable { showLangSelector = true },
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = CreamCard),
                border = BorderStroke(1.dp, SandyBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SoftPeach, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌐", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "150+",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive
                        )
                        Text(
                            text = "Languages Available",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreen
                        )
                    }
                }
            }
        }

        // Daily AI Challenge block (dashed styling!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, SandyBorder, RoundedCornerShape(32.dp))
                .background(WarmLight, RoundedCornerShape(32.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📖", fontSize = 36.sp)
                Text(
                    text = "Daily Challenge",
                    style = MaterialTheme.typography.titleLarge,
                    color = DarkOlive,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Unlock 5 highly useful daily vocabulary flashcards for ${selectedLanguage.name} generated by Gemini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                if (isLoadingFlashcards) {
                    CircularProgressIndicator(color = DeepGreen, modifier = Modifier.padding(top = 12.dp))
                } else {
                    Button(
                        onClick = { mainViewModel.triggerAiDeckGeneration() },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier.padding(top = 12.dp).testTag("challenge_button")
                    ) {
                        Text("Generate AI Cards", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Activity Action Headers
        Text(
            text = "Active Learning Spaces",
            style = MaterialTheme.typography.titleLarge,
            color = DarkOlive,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Action Buttons Row mapping
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LearningSpaceItem(
                title = "Study Flashcards",
                description = "Master words using Leitner spaced repetition",
                emoji = "🏷️",
                xpBadge = "10 XP",
                bgColor = CreamCard,
                onClick = onNavigateToFlashcards
            )

            LearningSpaceItem(
                title = "AI Conversational Chat",
                description = "Chat in target scripts with instant translation",
                emoji = "💬",
                xpBadge = "15 XP",
                bgColor = WarmCard,
                onClick = onNavigateToChat
            )

            LearningSpaceItem(
                title = "Accent & Pronunciation",
                description = "Practice speaking and receive prompt grading",
                emoji = "🗣️",
                xpBadge = "25 XP",
                bgColor = SoftPeach,
                onClick = onNavigateToPronun
            )
        }
        } else if (activeTab == "league") {
            LeaderboardSection(currentUser = user, leaderboard = leaderboard)
        } else if (activeTab == "analytics") {
            AnalyticsDashboardSection(user = user, studyLogs = studyLogs)
        } else if (activeTab == "reminders") {
            RemindersSection(user = user)
        } else {
            val maxPronunScoreState = mainViewModel.maxPronunScore.value
            val chatMessages by mainViewModel.chatMessages.collectAsState()
            val hasChatted = chatMessages.size
            BadgesSection(
                user = user,
                masteredCount = masteredCount,
                maxPronunScore = maxPronunScoreState,
                chatCount = hasChatted,
                flashcardCount = flashcardCount,
                authViewModel = authViewModel
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Language Search & Selector list dialog sheet
    if (showLangSelector) {
        LanguagePickerDialog(
            mainViewModel = mainViewModel,
            authViewModel = authViewModel,
            onDismiss = { showLangSelector = false }
        )
    }
}

@Composable
fun LearningSpaceItem(
    title: String,
    description: String,
    emoji: String,
    xpBadge: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkOlive)
            Text(description, fontSize = 12.sp, color = TextGray)
        }

        Box(
            modifier = Modifier
                .background(DeepGreen, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(xpBadge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// Gamification helper structures and views
data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val tier: String,
    val isUnlocked: Boolean,
    val progress: Float,
    val progressText: String
)

@Composable
fun LeaderboardSection(
    currentUser: UserEntity,
    leaderboard: List<UserEntity>
) {
    var leaderboardTab by remember { mutableStateOf("xp") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SandyBorder)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏆", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Emerald League",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkOlive
                    )
                    Text(
                        text = if (leaderboardTab == "xp") "Top polyglots competing globally" else "Leaderboard based on vocal accuracy high scores",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }

            // Interactive Selector Tabs for XP vs Vocal Accuracy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgWarm, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "xp" to "⚡ XP Point Rank",
                    "pronun" to "🗣️ Vocal Accuracy"
                ).forEach { (tabId, label) ->
                    val isTabSelected = leaderboardTab == tabId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTabSelected) LimeGreen else Color.Transparent)
                            .clickable { leaderboardTab = tabId }
                            .padding(vertical = 10.dp)
                            .testTag("leaderboard_tab_$tabId"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive
                        )
                    }
                }
            }

            Divider(color = SandyBorder)

            // Dynamic ranking calculation based on selected mode
            val sortedList = if (leaderboardTab == "xp") {
                leaderboard.sortedByDescending { it.xp }
            } else {
                leaderboard.sortedByDescending { it.pronunciationScore }
            }
            val rankIndex = sortedList.indexOfFirst { it.email == currentUser.email } + 1

            if (rankIndex > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LimeGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#$rankIndex",
                                fontWeight = FontWeight.Black,
                                color = DarkOlive,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                currentUser.avatarEmoji,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Your Ranking",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DarkOlive
                                )
                                Text(
                                    text = if (leaderboardTab == "xp") "Level ${currentUser.level}" else "Pronunciation Best Accent",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }
                        }
                        Text(
                            text = if (leaderboardTab == "xp") "${currentUser.xp} XP" else "${currentUser.pronunciationScore}% Acc",
                            fontWeight = FontWeight.Black,
                            color = DeepGreen,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedList.forEachIndexed { index, competitor ->
                    val isMe = competitor.email == currentUser.email
                    val rank = index + 1
                    val rowBg = if (isMe) LimeGreen.copy(alpha = 0.15f) else WarmLight
                    val borderStroke = if (isMe) BorderStroke(1.5.dp, DeepGreen) else BorderStroke(1.dp, SandyBorder)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = rowBg),
                        border = borderStroke
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Rank prefix
                                Box(
                                    modifier = Modifier.width(36.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    when (rank) {
                                        1 -> Text("👑", fontSize = 18.sp)
                                        2 -> Text("🥈", fontSize = 18.sp)
                                        3 -> Text("🥉", fontSize = 18.sp)
                                        else -> Text("#$rank", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextGray)
                                    }
                                }

                                Text(competitor.avatarEmoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = competitor.username,
                                            fontWeight = if (isMe) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = DarkOlive
                                        )
                                        if (isMe) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(DeepGreen, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("YOU", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (leaderboardTab == "xp") "Level ${competitor.level}" else "Accent Practice Competitor",
                                        fontSize = 10.sp,
                                        color = TextGray
                                    )
                                }
                            }
                            Text(
                                text = if (leaderboardTab == "xp") "${competitor.xp} XP" else "${competitor.pronunciationScore}% Acc",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DarkOlive
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BadgesSection(
    user: UserEntity,
    masteredCount: Int,
    maxPronunScore: Int,
    chatCount: Int,
    flashcardCount: Int,
    authViewModel: AuthViewModel
) {
    // Collect stats to evaluate badges
    val streakValue = user.streak
    val xpValue = user.xp
    val totalMastered = masteredCount

    val badgesList = listOf(
        // --- STREAK BADGES ---
        Badge(
            id = "streak_bronze",
            title = "Bronze Spark",
            description = "Maintain a 3-day consecutive study streak",
            icon = "🔥",
            tier = "Bronze",
            isUnlocked = streakValue >= 3,
            progress = (streakValue / 3f).coerceIn(0f, 1f),
            progressText = "$streakValue/3 Days"
        ),
        Badge(
            id = "streak_silver",
            title = "Silver Flare",
            description = "Maintain a 7-day consecutive study streak",
            icon = "⚡",
            tier = "Silver",
            isUnlocked = streakValue >= 7,
            progress = (streakValue / 7f).coerceIn(0f, 1f),
            progressText = "$streakValue/7 Days"
        ),
        Badge(
            id = "streak_gold",
            title = "Gold Flame",
            description = "Maintain a 14-day consecutive study streak",
            icon = "👑",
            tier = "Gold",
            isUnlocked = streakValue >= 14,
            progress = (streakValue / 14f).coerceIn(0f, 1f),
            progressText = "$streakValue/14 Days"
        ),

        // --- VOCAB BADGES ---
        Badge(
            id = "vocab_apprentice",
            title = "Vocab Apprentice",
            description = "Master 5 vocabulary words using Leitner memorization",
            icon = "🏷️",
            tier = "Bronze",
            isUnlocked = totalMastered >= 5,
            progress = (totalMastered / 5f).coerceIn(0f, 1f),
            progressText = "$totalMastered/5 Mastered"
        ),
        Badge(
            id = "vocab_specialist",
            title = "Vocab Specialist",
            description = "Master 20 vocabulary words using Leitner memorization",
            icon = "📚",
            tier = "Silver",
            isUnlocked = totalMastered >= 20,
            progress = (totalMastered / 20f).coerceIn(0f, 1f),
            progressText = "$totalMastered/20 Mastered"
        ),
        Badge(
            id = "vocab_titan",
            title = "Centurion Master",
            description = "Uncover and study 100 flashcards in your deck",
            icon = "🧠",
            tier = "Gold",
            isUnlocked = flashcardCount >= 100,
            progress = (flashcardCount / 100f).coerceIn(0f, 1f),
            progressText = "$flashcardCount/100 Cards"
        ),

        // --- ACADEMIC LEVEL BADGES ---
        Badge(
            id = "scholar_level_2",
            title = "Bronze Scholar",
            description = "Step forward and ascend to Course Level 2",
            icon = "📓",
            tier = "Bronze",
            isUnlocked = user.level >= 2,
            progress = if (user.level >= 2) 1.0f else 0.5f,
            progressText = "Lvl ${user.level}/2"
        ),
        Badge(
            id = "scholar_level_3",
            title = "Master Scholar",
            description = "Step forward and ascend to Course Level 3",
            icon = "🎓",
            tier = "Silver",
            isUnlocked = user.level >= 3,
            progress = (user.level / 3f).coerceIn(0f, 1f),
            progressText = "Lvl ${user.level}/3"
        ),
        Badge(
            id = "scholar_level_4",
            title = "Academic Icon",
            description = "Step forward and ascend to Course Level 4",
            icon = "🏛️",
            tier = "Gold",
            isUnlocked = user.level >= 4,
            progress = (user.level / 4f).coerceIn(0f, 1f),
            progressText = "Lvl ${user.level}/4"
        ),

        // --- CONVERSATION / CHAT BADGES ---
        Badge(
            id = "chat_apprentice",
            title = "Conversationalist",
            description = "Send 1 chatbot study practice message",
            icon = "💬",
            tier = "Bronze",
            isUnlocked = chatCount >= 1,
            progress = (chatCount / 1f).coerceIn(0f, 1f),
            progressText = "$chatCount/1 Msg"
        ),
        Badge(
            id = "chat_expert",
            title = "Discourse Guide",
            description = "Send 5 chatbot study practice messages",
            icon = "🗣️",
            tier = "Silver",
            isUnlocked = chatCount >= 5,
            progress = (chatCount / 5f).coerceIn(0f, 1f),
            progressText = "$chatCount/5 Msgs"
        ),
        Badge(
            id = "chat_titan",
            title = "Silver Orator",
            description = "Send 10 chatbot study practice messages",
            icon = "🎙️",
            tier = "Gold",
            isUnlocked = chatCount >= 10,
            progress = (chatCount / 10f).coerceIn(0f, 1f),
            progressText = "$chatCount/10 Msgs"
        ),

        // --- PRONUNCIATION BADGES ---
        Badge(
            id = "accent_apprentice",
            title = "Voice Pioneer",
            description = "Initiate pronunciation practice to get a score > 0",
            icon = "🎤",
            tier = "Bronze",
            isUnlocked = maxPronunScore > 0,
            progress = if (maxPronunScore > 0) 1.0f else 0f,
            progressText = "$maxPronunScore/1 Score"
        ),
        Badge(
            id = "accent_expert",
            title = "Accent Artisan",
            description = "Achieve a 70+ accuracy rating on pronunciation review",
            icon = "📢",
            tier = "Silver",
            isUnlocked = maxPronunScore >= 70,
            progress = (maxPronunScore / 70f).coerceIn(0f, 1f),
            progressText = "$maxPronunScore/70 Score"
        ),
        Badge(
            id = "accent_titan",
            title = "Perfect Pitch",
            description = "Achieve a 90+ accuracy rating on pronunciation review",
            icon = "🎯",
            tier = "Gold",
            isUnlocked = maxPronunScore >= 90,
            progress = (maxPronunScore / 90f).coerceIn(0f, 1f),
            progressText = "$maxPronunScore/90 Score"
        ),

        // --- XP / POINT BADGES ---
        Badge(
            id = "xp_pioneer",
            title = "XP Novice",
            description = "Unlock 100 total XP points across activity challenges",
            icon = "⭐",
            tier = "Bronze",
            isUnlocked = xpValue >= 100,
            progress = (xpValue / 100f).coerceIn(0f, 1f),
            progressText = "$xpValue/100 XP"
        ),
        Badge(
            id = "xp_veteran",
            title = "XP Veteran",
            description = "Unlock 250 total XP points across activity challenges",
            icon = "🌟",
            tier = "Silver",
            isUnlocked = xpValue >= 250,
            progress = (xpValue / 250f).coerceIn(0f, 1f),
            progressText = "$xpValue/250 XP"
        ),
        Badge(
            id = "xp_legend",
            title = "XP Legend",
            description = "Unlock 1000 total XP points across activity challenges",
            icon = "💎",
            tier = "Gold",
            isUnlocked = xpValue >= 1000,
            progress = (xpValue / 1000f).coerceIn(0f, 1f),
            progressText = "$xpValue/1000 XP"
        )
    )

    var badgeFilter by remember { mutableStateOf("all") } // "all", "unlocked", "locked"
    var selectedBadge by remember { mutableStateOf<Badge?>(null) }

    val filteredBadges = remember(badgeFilter, badgesList) {
        when (badgeFilter) {
            "unlocked" -> badgesList.filter { it.isUnlocked }
            "locked" -> badgesList.filter { !it.isUnlocked }
            else -> badgesList
        }
    }

    val totalUnlocked = remember(badgesList) { badgesList.count { it.isUnlocked } }
    val progressFraction = remember(badgesList) { totalUnlocked.toFloat() / badgesList.size.toFloat() }
    val animatedProgressFraction by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "BadgeProgressOverall"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Showcase Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = WarmCard),
            border = BorderStroke(1.dp, SandyBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Achievement Board",
                            style = MaterialTheme.typography.titleLarge,
                            color = DarkOlive,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Unlocked $totalUnlocked of ${badgesList.size} milestones. Tap any badge to view limits, unlock criteria, and equip visual status icons!",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepGreen,
                            lineHeight = 16.sp
                        )
                    }
                    Text(
                        text = "🏆",
                        fontSize = 44.sp,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                // Overall progress bar
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Collection Completion Progress",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive
                        )
                        Text(
                            text = "${(animatedProgressFraction * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreen
                        )
                    }

                    LinearProgressIndicator(
                        progress = animatedProgressFraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = DeepGreen,
                        trackColor = Color(0xFFEFECE6)
                    )
                }
            }
        }

        // Sub-navigation filter capsules
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEFECE6), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "all" to "All (${badgesList.size})",
                "unlocked" to "Locked 🔒" + " (${badgesList.size - totalUnlocked})", // wait, just show "Unlocked" and "Locked" dynamically or simplified names:
            ).map {
                // Let's create proper clean names
                when (it.first) {
                    "all" -> "all" to "All 📔"
                    "unlocked" -> "unlocked" to "Unlocked 🎉 ($totalUnlocked)"
                    else -> "locked" to "Locked 🔒"
                }
            }.forEach { (tabId, label) ->
                val isSelected = badgeFilter == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { badgeFilter = tabId }
                        .padding(vertical = 10.dp)
                        .testTag("badge_filter_$tabId"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isSelected) DarkOlive else DarkOlive.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Explicitly add "Locked" option
            val isLockedSelected = badgeFilter == "locked"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLockedSelected) Color.White else Color.Transparent)
                    .clickable { badgeFilter = "locked" }
                    .padding(vertical = 10.dp)
                    .testTag("badge_filter_locked"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Locked 🔒",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isLockedSelected) DarkOlive else DarkOlive.copy(alpha = 0.6f)
                )
            }
        }

        // Responsive grid (3-columns) chunk flow pattern
        if (filteredBadges.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(1.dp, SandyBorder, RoundedCornerShape(24.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🛡️", fontSize = 44.sp)
                    Text(
                        text = "No Badges Found",
                        fontWeight = FontWeight.Bold,
                        color = DarkOlive,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Practice vocabulary, test grammar, or converse more to earn corresponding badges here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val rows = filteredBadges.chunked(3)
            rows.forEach { rowBadges ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowBadges.forEach { badge ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedBadge = badge }
                                .testTag("badge_item_${badge.id}"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, if (badge.isUnlocked) SandyBorder else SandyBorder.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Badge icon container
                                val bgContainer = if (badge.isUnlocked) {
                                    val colorAlpha = when (badge.tier) {
                                        "Gold" -> SharpAmber.copy(alpha = 0.15f)
                                        "Silver" -> Color.Gray.copy(alpha = 0.15f)
                                        else -> DeepGreen.copy(alpha = 0.12f)
                                    }
                                    colorAlpha
                                } else {
                                    Color(0xFFF1EDE4)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(bgContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (badge.isUnlocked) badge.icon else "🔒",
                                        fontSize = 20.sp
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = badge.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (badge.isUnlocked) DarkOlive else DarkOlive.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (badge.tier) {
                                                    "Gold" -> SharpAmber.copy(alpha = 0.15f)
                                                    "Silver" -> Color.LightGray.copy(alpha = 0.3f)
                                                    else -> SoftPeach
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badge.tier,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (badge.tier) {
                                                "Gold" -> Color(0xFF856404)
                                                "Silver" -> Color(0xFF383D41)
                                                else -> DarkOlive
                                            }
                                        )
                                    }
                                }

                                // Interactive mini bar
                                val animBadgeProgress by animateFloatAsState(
                                    targetValue = badge.progress,
                                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                                    label = "BadgeMiniProgress_${badge.id}"
                                )
                                val pbColor = if (badge.isUnlocked) DeepGreen else TextGray.copy(alpha = 0.3f)
                                LinearProgressIndicator(
                                    progress = animBadgeProgress,
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp)),
                                    color = pbColor,
                                    trackColor = Color(0xFFECECE6)
                                )
                            }
                        }
                    }

                    // Pad columns if row size is less than 3
                    val missingCols = 3 - rowBadges.size
                    repeat(missingCols) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    // Detail Dialog Popup
    selectedBadge?.let { badge ->
        AlertDialog(
            onDismissRequest = { selectedBadge = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val tierColor = when (badge.tier) {
                        "Gold" -> SharpAmber
                        "Silver" -> Color.Gray
                        else -> DeepGreen
                    }

                    // Huge glowing badge container
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(tierColor.copy(alpha = 0.1f), CircleShape)
                            .border(3.dp, tierColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badge.isUnlocked) badge.icon else "🔒",
                            fontSize = 48.sp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = badge.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive,
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .background(tierColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${badge.tier} Elite Badge",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = tierColor
                            )
                        }
                    }

                    Text(
                        text = badge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    // Progress Section inside Popup
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFBF9F4), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Milestone Achievement",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkOlive
                            )
                            Text(
                                text = badge.progressText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (badge.isUnlocked) DeepGreen else TextGray
                            )
                        }

                        val animPopupProgress by animateFloatAsState(
                            targetValue = badge.progress,
                            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                            label = "BadgePopupProgress"
                        )

                        LinearProgressIndicator(
                            progress = animPopupProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (badge.isUnlocked) DeepGreen else Color.LightGray,
                            trackColor = Color(0xFFEFECE6)
                        )

                        if (badge.isUnlocked) {
                            Text(
                                text = "🎉 Superb! Challenge acquired! You have fully unlocked this dynamic visual badge badge.",
                                fontSize = 10.sp,
                                color = DeepGreen,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            val remainingText = when {
                                badge.id.startsWith("streak") -> {
                                    val req = if (badge.id.endsWith("bronze")) 3 else if (badge.id.endsWith("silver")) 7 else 14
                                    val diff = req - streakValue
                                    "Requires $diff more consecutive active study days to unlock."
                                }
                                badge.id.startsWith("vocab") -> {
                                    if (badge.id.endsWith("titan")) {
                                        "Requires adding ${100 - flashcardCount} more words to your storage deck."
                                    } else {
                                        val req = if (badge.id.endsWith("apprentice")) 5 else 20
                                        "Requires mastering ${req - totalMastered} more words in Leitner challenges."
                                    }
                                }
                                badge.id.startsWith("scholar") -> {
                                    val req = if (badge.id.endsWith("2")) 2 else if (badge.id.endsWith("3")) 3 else 4
                                    "Requires advancing to level $req in your course track."
                                }
                                badge.id.startsWith("chat") -> {
                                    val req = if (badge.id.endsWith("apprentice")) 1 else if (badge.id.endsWith("expert")) 5 else 10
                                    "Requires exchanging ${req - chatCount} more chat conversation lines with the AI."
                                }
                                badge.id.startsWith("accent") -> {
                                    val req = if (badge.id.endsWith("apprentice")) 1 else if (badge.id.endsWith("expert")) 70 else 90
                                    "Requires achieving a score of $req in pronunciation review."
                                }
                                else -> {
                                    val req = if (badge.id.endsWith("pioneer")) 100 else if (badge.id.endsWith("veteran")) 250 else 1000
                                    "Requires gaining ${req - xpValue} more XP to unlock."
                                }
                            }
                            Text(
                                text = "💡 $remainingText",
                                fontSize = 10.sp,
                                color = TextGray,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    if (badge.isUnlocked) {
                        Button(
                            onClick = {
                                // Equip Badge Icon as Profile Avatar Emoji!
                                authViewModel.updateUserProfile(
                                    username = user.username,
                                    email = user.email,
                                    dailyGoalMinutes = user.dailyGoalMinutes,
                                    avatarEmoji = badge.icon,
                                    targetLanguageCode = user.targetLanguageCode
                                )
                                selectedBadge = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("equip_avatar_button")
                        ) {
                            Text(
                                text = "Equip as Profile Avatar ✨",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { selectedBadge = null },
                    modifier = Modifier.testTag("close_badge_dialog")
                ) {
                    Text("Close", color = TextGray, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun StreakCelebrationDialog(
    reward: StreakReward,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("dismiss_streak_dialog")
            ) {
                Text("Let's Go! 🚀", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔥", fontSize = 32.sp)
                Text(
                    text = "Daily Streak Reward!",
                    style = MaterialTheme.typography.titleLarge,
                    color = DarkOlive,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmCard),
                    border = BorderStroke(1.dp, SandyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${reward.dayCount}",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkOlive
                        )
                        Text(
                            text = "DAYS CONSECUTIVE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreen,
                            letterSpacing = 1.5.sp
                        )
                        
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SandyBorder))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("✨", fontSize = 18.sp)
                            Text(
                                text = "Earned +${reward.xpAwarded} XP Daily Bonus!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkOlive
                            )
                            Text("✨", fontSize = 18.sp)
                        }
                    }
                }
                
                Text(
                    text = reward.message,
                    fontSize = 14.sp,
                    color = DeepGreen,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        },
        containerColor = Color(0xFFFAF9F5),
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun RemindersSection(user: UserEntity) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(NotificationHelper.PREFS_NAME, Context.MODE_PRIVATE) }
    
    var remindersEnabled by remember { mutableStateOf(prefs.getBoolean(NotificationHelper.KEY_REMINDERS_ENABLED, true)) }
    var reminderHour by remember { mutableStateOf(prefs.getInt(NotificationHelper.KEY_REMINDER_HOUR, 20)) } // 8 PM default
    var reminderMinute by remember { mutableStateOf(prefs.getInt(NotificationHelper.KEY_REMINDER_MINUTE, 0)) }
    var fcmToken by remember { mutableStateOf(prefs.getString(NotificationHelper.KEY_FCM_TOKEN, "")) }
    var retrievingToken by remember { mutableStateOf(false) }

    var hasPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            prefs.edit().putBoolean(NotificationHelper.KEY_REMINDERS_ENABLED, true).apply()
            remindersEnabled = true
            NotificationHelper.scheduleDailyReminder(context, reminderHour, reminderMinute)
            Toast.makeText(context, "🔔 Permission granted! Reminders scheduled.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "⚠️ Permission denied. Reminders may not show up on screen.", Toast.LENGTH_LONG).show()
        }
    }

    // Clipboard for FCM Token
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Aesthetic Title Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WarmCard),
            border = BorderStroke(1.dp, SandyBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(LimeGreen, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔔", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Streak Reminders",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkOlive
                    )
                    Text(
                        text = "Build small daily study habits that compound.",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }
        }

        // Main Controls Column
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CreamCard),
            border = BorderStroke(1.dp, SandyBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Permission Status Header indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Notification Permission",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive
                        )
                        Text(
                            text = if (hasPermission) "Authorized by Android System" else "Approval needed to show reminders",
                            fontSize = 11.sp,
                            color = if (hasPermission) DeepGreen else TextGray
                        )
                    }

                    if (!hasPermission) {
                        Button(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    hasPermission = true
                                    Toast.makeText(context, "System permission is active.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(DeepGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Active ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepGreen)
                        }
                    }
                }

                Divider(color = SandyBorder.copy(alpha = 0.5f))

                // Toggle Reminders Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(WarmLight, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (remindersEnabled) "🔊" else "🔇", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Daily Lesson Alerts",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkOlive
                            )
                            Text(
                                text = "Trigger consecutive study reminders",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }

                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked && !hasPermission) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    remindersEnabled = true
                                    prefs.edit().putBoolean(NotificationHelper.KEY_REMINDERS_ENABLED, true).apply()
                                    NotificationHelper.scheduleDailyReminder(context, reminderHour, reminderMinute)
                                }
                            } else {
                                remindersEnabled = isChecked
                                prefs.edit().putBoolean(NotificationHelper.KEY_REMINDERS_ENABLED, isChecked).apply()
                                if (isChecked) {
                                    NotificationHelper.scheduleDailyReminder(context, reminderHour, reminderMinute)
                                } else {
                                    NotificationHelper.cancelDailyReminder(context)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DeepGreen,
                            uncheckedThumbColor = TextGray,
                            uncheckedTrackColor = SandyBorder
                        )
                    )
                }

                if (remindersEnabled) {
                    Divider(color = SandyBorder.copy(alpha = 0.5f))

                    // Custom Time Pickers
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Scheduled Daily Reminder Time:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Display time beautifully
                            val timeText = remember(reminderHour, reminderMinute) {
                                val period = if (reminderHour >= 12) "PM" else "AM"
                                val displayHour = if (reminderHour % 12 == 0) 12 else reminderHour % 12
                                String.format("%02d:%02d %s", displayHour, reminderMinute, period)
                            }

                            Text(
                                text = timeText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepGreen
                            )

                            // Step controls
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        reminderHour = (reminderHour + 23) % 24
                                        prefs.edit().putInt(NotificationHelper.KEY_REMINDER_HOUR, reminderHour).apply()
                                        NotificationHelper.scheduleDailyReminder(context, reminderHour, reminderMinute)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmLight, contentColor = DarkOlive),
                                    border = BorderStroke(1.dp, SandyBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Hr -", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        reminderHour = (reminderHour + 1) % 24
                                        prefs.edit().putInt(NotificationHelper.KEY_REMINDER_HOUR, reminderHour).apply()
                                        NotificationHelper.scheduleDailyReminder(context, reminderHour, reminderMinute)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmLight, contentColor = DarkOlive),
                                    border = BorderStroke(1.dp, SandyBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Hr +", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        reminderMinute = (reminderMinute + 55) % 60
                                        prefs.edit().putInt(NotificationHelper.KEY_REMINDER_MINUTE, reminderMinute).apply()
                                        NotificationHelper.scheduleDailyReminder(context, reminderHour, reminderMinute)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmLight, contentColor = DarkOlive),
                                    border = BorderStroke(1.dp, SandyBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Min -", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        reminderMinute = (reminderMinute + 5) % 60
                                        prefs.edit().putInt(NotificationHelper.KEY_REMINDER_MINUTE, reminderMinute).apply()
                                        NotificationHelper.scheduleDailyReminder(context, reminderHour, reminderMinute)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmLight, contentColor = DarkOlive),
                                    border = BorderStroke(1.dp, SandyBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Min +", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Presets Row
                        Text(
                            text = "Time Presets:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(7, 30, "☀️ 7:30 AM"),
                                Triple(12, 15, "🥪 12:15 PM"),
                                Triple(20, 0, "🌙 8:00 PM"),
                                Triple(22, 15, "🛌 10:15 PM")
                            ).forEach { (h, m, label) ->
                                val isSelectedPreset = reminderHour == h && reminderMinute == m
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelectedPreset) LimeGreen else WarmLight)
                                        .border(1.dp, if (isSelectedPreset) DeepGreen else SandyBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            reminderHour = h
                                            reminderMinute = m
                                            prefs.edit().putInt(NotificationHelper.KEY_REMINDER_HOUR, h).apply()
                                            prefs.edit().putInt(NotificationHelper.KEY_REMINDER_MINUTE, m).apply()
                                            NotificationHelper.scheduleDailyReminder(context, h, m)
                                            Toast.makeText(context, "$label scheduled!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkOlive)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tactile Live Test Trigger
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WarmLight),
            border = BorderStroke(1.dp, SandyBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Verify Notification Styling",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkOlive
                )
                Text(
                    text = "Instantly trigger a simulated streak reminder to preview how notifications look on your screen. Keeps testing effortless!",
                    fontSize = 11.sp,
                    color = TextGray
                )

                Button(
                    onClick = {
                        if (hasPermission) {
                            NotificationHelper.showStreakReminderNotification(context, user.streak)
                            Toast.makeText(context, "Streak Reminder triggered!", Toast.LENGTH_SHORT).show()
                        } else {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                NotificationHelper.showStreakReminderNotification(context, user.streak)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimeGreen, contentColor = DarkOlive),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Trigger Test Streak Reminder 🚀", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Cloud Push Notification Credentials (FCM Token Diagnostic Panel)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CreamCard),
            border = BorderStroke(1.dp, SandyBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cloud Diagnostics (FCM)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkOlive
                    )

                    // Retrieve button code
                    TextButton(
                        onClick = {
                            retrievingToken = true
                            try {
                                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                    retrievingToken = false
                                    if (task.isSuccessful) {
                                        fcmToken = task.result ?: ""
                                        prefs.edit().putString(NotificationHelper.KEY_FCM_TOKEN, fcmToken).apply()
                                        Toast.makeText(context, "FCM Token refreshed!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Log.w("RemindersSection", "FCM token lookup failed", task.exception)
                                        Toast.makeText(context, "FCM token lookup failed or credentials missing.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Exception) {
                                retrievingToken = false
                                Log.e("RemindersSection", "Firebase Messaging not configured", e)
                                Toast.makeText(context, "Firebase not initialized. Ensure google-services.json is added.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = DeepGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(if (retrievingToken) "Fetching..." else "Sync Token 🔄", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "If downstream push campaigns are set up on your Firebase Console, notifications can target this device directly using its token.",
                    fontSize = 11.sp,
                    color = TextGray
                )

                // Render token or helpful guide
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmLight, RoundedCornerShape(12.dp))
                        .border(1.dp, SandyBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (fcmToken.isNullOrBlank()) {
                                "Token Status: Preserved/Null (Tap Sync Token to load from Firebase)"
                            } else {
                                fcmToken ?: ""
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (fcmToken.isNullOrBlank()) TextGray else DarkOlive,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!fcmToken.isNullOrBlank()) {
                            Button(
                                onClick = {
                                    fcmToken?.let { token ->
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(token))
                                        Toast.makeText(context, "FCM Device Token copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.align(Alignment.End),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Copy Token 📋", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsDashboardSection(
    user: UserEntity,
    studyLogs: List<StudyLogEntity>
) {
    val context = LocalContext.current
    
    // Calculate historical daily aggregates for the last 7 calendar days
    val calendar = Calendar.getInstance()
    val now = Calendar.getInstance()
    
    val dayLabels = List(7) { index ->
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -index)
        }
        val dayNum = cal.get(Calendar.DAY_OF_WEEK)
        when (dayNum) {
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "?"
        }
    }.reversed() // Oldest (6 days ago) to newest (today)
    
    val completionCounts = remember(studyLogs) {
        val counts = IntArray(7) { 0 }
        // Simple day matching
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val last7DaysStrings = List(7) { index ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -index)
            }
            sdf.format(cal.time)
        }.reversed() // Oldest to newest
        
        studyLogs.forEach { log ->
            val logDateStr = sdf.format(Date(log.timestamp))
            val index = last7DaysStrings.indexOf(logDateStr)
            if (index in 0..6) {
                counts[index]++
            }
        }
        counts
    }
    
    val maxCount = remember(completionCounts) {
        completionCounts.maxOrNull()?.coerceAtLeast(4) ?: 4
    }
    
    // CSS transition / Compose equivalent state variables for milestones and progress updates
    var animationTriggered by remember { mutableStateOf(false) }
    
    // Smooth number sweepers for streak and xp
    var streakTarget by remember { mutableStateOf(0) }
    var xpTarget by remember { mutableStateOf(0) }
    
    val animatedStreak by animateIntAsState(
        targetValue = streakTarget,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "StreakAnimation"
    )
    
    val animatedXp by animateIntAsState(
        targetValue = xpTarget,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "XpAnimation"
    )

    LaunchedEffect(user.streak, user.xp) {
        streakTarget = user.streak
        xpTarget = user.xp
        animationTriggered = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Statistics Cards with CSS-transition equivalent value rollwards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = WarmCard),
                border = BorderStroke(1.dp, SandyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔥 STREAK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$animatedStreak Days", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DarkOlive)
                }
            }
            
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = WarmCard),
                border = BorderStroke(1.dp, SandyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚡ TOTAL XP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$animatedXp XP", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DarkOlive)
                }
            }
        }

        // Celebrate learning updates (Milestone notification UI)
        if (user.xp > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LimeGreen.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, LimeGreen.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🏆", fontSize = 24.sp)
                    Column {
                        Text(
                            text = "Milestone Status: Active Developer Check-in",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive
                        )
                        Text(
                            text = "Next reward tier unlocks at ${((user.xp / 100) + 1) * 100} XP (Currently Level ${(user.xp / 100) + 1})",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }
            }
        }

        // Daily Lesson Completion Counts (Responsive Bar Chart)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CreamCard),
            border = BorderStroke(1.dp, SandyBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lessons & Check-ins",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive
                        )
                        Text(
                            text = "Daily completions over the past 7 days",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(LimeGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "7-Day Stats",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive
                        )
                    }
                }
                
                // Pure Responsive Compose styled Bar chart (with smooth transitions!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dayLabels.forEachIndexed { idx, label ->
                        val count = completionCounts[idx]
                        val fraction = count.toFloat() / maxCount.toFloat()
                        
                        // Local state backing height anim targets
                        var barFractionTarget by remember { mutableStateOf(0f) }
                        LaunchedEffect(fraction, animationTriggered) {
                            barFractionTarget = fraction
                        }
                        
                        val animatedFraction by animateFloatAsState(
                            targetValue = barFractionTarget,
                            animationSpec = tween(
                                durationMillis = 800,
                                delayMillis = idx * 100, // Elegant staggered CSS-like delay
                                easing = FastOutSlowInEasing
                            ),
                            label = "BarHeightAnimation_$idx"
                        )
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Count text
                            Text(
                                text = if (count > 0) "$count" else "-",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (count > 0) DarkOlive else TextGray.copy(alpha = 0.5f)
                            )
                            
                            // Bar box column
                            Box(
                                modifier = Modifier
                                    .height(115.dp)
                                    .width(16.dp)
                                    .background(SandyBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                if (animatedFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(animatedFraction)
                                            .background(DeepGreen, RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                            
                            // Label
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkOlive
                            )
                        }
                    }
                }
            }
        }

        // Streak History Calendar Grid Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WarmCard),
            border = BorderStroke(1.dp, SandyBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Weekly Streak History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkOlive
                )
                Text(
                    text = "Logs completed consecutively fuel your progress seed!",
                    fontSize = 11.sp,
                    color = TextGray
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayLabels.forEachIndexed { idx, label ->
                        val hasCompleted = completionCounts[idx] > 0
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (hasCompleted) LimeGreen else Color.White,
                                        CircleShape
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (hasCompleted) DeepGreen else SandyBorder,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasCompleted) {
                                    Text("🔥", fontSize = 16.sp)
                                } else {
                                    Text("🌱", fontSize = 14.sp)
                                }
                            }
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = DarkOlive)
                        }
                    }
                }
            }
        }

        // Activity Breakdown and Types
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CreamCard),
            border = BorderStroke(1.dp, SandyBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Activity Breakdown",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkOlive
                )
                
                // Categorized Activity distribution list
                val activities = remember(studyLogs) {
                    val actMap = mutableMapOf(
                        "Flashcard" to 0,
                        "Quiz" to 0,
                        "Chat" to 0,
                        "Pronunciation" to 0,
                        "Daily Checkin" to 0
                    )
                    studyLogs.forEach { log ->
                        val type = log.activityType
                        actMap[type] = (actMap[type] ?: 0) + 1
                    }
                    actMap.toList().sortedByDescending { it.second }
                }
                
                val totalActivities = remember(activities) {
                    activities.map { it.second }.sum().coerceAtLeast(1)
                }

                activities.forEach { (type, count) ->
                    val typeFraction = count.toFloat() / totalActivities.toFloat()
                    val icon = when (type) {
                        "Flashcard" -> "🏷️"
                        "Quiz" -> "📝"
                        "Chat" -> "💬"
                        "Pronunciation" -> "🗣️"
                        else -> "🔥"
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(icon, fontSize = 16.sp)
                                Text(type, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkOlive)
                            }
                            Text("$count logs", fontSize = 11.sp, color = TextGray)
                        }
                        
                        // Progressive Distribution Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(SandyBorder.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        ) {
                            if (typeFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(typeFraction)
                                        .fillMaxHeight()
                                        .background(
                                            if (type == "Daily Checkin") LimeGreen else DeepGreen,
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

