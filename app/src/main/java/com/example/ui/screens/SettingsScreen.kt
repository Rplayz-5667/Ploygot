package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LanguageRegistry
import com.example.data.database.UserEntity
import com.example.data.notifications.NotificationHelper
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: UserEntity,
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Read initial Notification values from Shared Preferences
    val prefs = remember { context.getSharedPreferences(NotificationHelper.PREFS_NAME, Context.MODE_PRIVATE) }
    var remindersEnabled by remember { mutableStateOf(prefs.getBoolean(NotificationHelper.KEY_REMINDERS_ENABLED, true)) }
    var reminderHour by remember { mutableIntStateOf(prefs.getInt(NotificationHelper.KEY_REMINDER_HOUR, 19)) } // default 7 PM
    var reminderMinute by remember { mutableIntStateOf(prefs.getInt(NotificationHelper.KEY_REMINDER_MINUTE, 0)) }
    var reminderFrequency by remember { mutableStateOf(prefs.getString(NotificationHelper.KEY_REMINDER_FREQUENCY, "daily") ?: "daily") }

    // User Account Input state
    var username by remember { mutableStateOf(user.username) }
    var email by remember { mutableStateOf(user.email) }
    var selectedAvatar by remember { mutableStateOf(user.avatarEmoji) }
    var dailyGoalMinutes by remember { mutableIntStateOf(user.dailyGoalMinutes) }
    
    // Dialog control for Language registry
    var showLanguagePicker by remember { mutableStateOf(false) }

    // Language list details
    val currentLang = remember(user.targetLanguageCode) {
        LanguageRegistry.getByCode(user.targetLanguageCode) ?: LanguageRegistry.languages.first()
    }

    val avatars = listOf("🔬", "🎓", "🐼", "🦊", "🦉", "🐰", "🥷", "🐨", "🐧", "🚀", "✨", "🍕")
    val goals = listOf(5, 10, 20, 30, 45, 60)

    if (showLanguagePicker) {
        LanguagePickerDialog(
            mainViewModel = mainViewModel,
            authViewModel = authViewModel,
            onDismiss = { showLanguagePicker = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWarm)
            .verticalScroll(scrollState)
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Upper Title Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "PREFERENCES & PROFILE",
                style = MaterialTheme.typography.labelMedium,
                color = DeepGreen,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.displayMedium,
                color = DarkOlive,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp
            )
            Text(
                text = "Configure your target language path, custom reminders, and avatar badges.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
        }

        Divider(color = SandyBorder, modifier = Modifier.padding(vertical = 4.dp))

        // 1. LANGUAGE FOCUS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CreamCard),
            border = BorderStroke(1.dp, SandyBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Language focus",
                        tint = DeepGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Active Language Focus",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkOlive,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, SandyBorder, RoundedCornerShape(16.dp))
                        .clickable { showLanguagePicker = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(BgWarm, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(currentLang.flag, fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentLang.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive
                        )
                        Text(
                            text = "Native Name: ${currentLang.nativeName}",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(LimeGreen, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Change 🌍",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive
                        )
                    }
                }
            }
        }

        // 2. ACCOUNT INFORMATION CARD
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile information",
                        tint = DeepGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Profile Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkOlive,
                        fontWeight = FontWeight.Bold
                    )
                }

                // AVATAR CHOOSER
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Personal Avatar Emoji",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, SandyBorder, RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        avatars.take(6).forEach { emoji ->
                            val isSelected = selectedAvatar == emoji
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) LimeGreen else Color.Transparent)
                                    .clickable { selectedAvatar = emoji }
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) DeepGreen else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // Username field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Display Name") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = "Name Icon", tint = DeepGreen) },
                    modifier = Modifier.fillMaxWidth().testTag("settings_username"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepGreen,
                        unfocusedBorderColor = SandyBorder,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = DeepGreen) },
                    modifier = Modifier.fillMaxWidth().testTag("settings_email"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepGreen,
                        unfocusedBorderColor = SandyBorder,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                // Daily study goals
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Daily Study Goal (Minutes)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        goals.forEach { m ->
                            val isSelected = dailyGoalMinutes == m
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) DeepGreen else Color.White)
                                    .border(BorderStroke(1.dp, SandyBorder), RoundedCornerShape(12.dp))
                                    .clickable { dailyGoalMinutes = m }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$m m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else DarkOlive
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. SMART STREAK REMINDERS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CreamCard),
            border = BorderStroke(1.dp, SandyBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Notifications",
                        tint = DeepGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Reminders & Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkOlive,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Streak Alerts",
                            fontWeight = FontWeight.Bold,
                            color = DarkOlive,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Receive notifications to protect your level progress.",
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { remindersEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DeepGreen,
                            uncheckedThumbColor = TextGray,
                            uncheckedTrackColor = SandyBorder
                        ),
                        modifier = Modifier.testTag("reminders_toggle")
                    )
                }

                AnimatedVisibility(
                    visible = remindersEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Frequency Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Reminder Interval Frequency",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGray
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "twice_daily" to "Twice Daily ⚡",
                                    "daily" to "Daily ⏰",
                                    "weekly" to "Weekly 📅"
                                ).forEach { (freqId, label) ->
                                    val isSelected = reminderFrequency == freqId
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) DeepGreen else Color.White)
                                            .border(1.dp, SandyBorder, RoundedCornerShape(12.dp))
                                            .clickable { reminderFrequency = freqId }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else DarkOlive,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // Time Picker Preset Rows
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Preferred Scheduled Hour",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGray
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    8 to "08:00 AM 🌅",
                                    12 to "12:00 PM ☀️",
                                    18 to "06:00 PM 🌇",
                                    20 to "08:00 PM 🌙",
                                    22 to "10:00 PM 💤"
                                ).forEach { (hour, label) ->
                                    val isSelected = reminderHour == hour
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) LimeGreen else Color.White)
                                            .border(1.dp, SandyBorder, RoundedCornerShape(10.dp))
                                            .clickable {
                                                reminderHour = hour
                                                reminderMinute = 0
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkOlive,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ACTIONS AND SAVE BAR
        Button(
            onClick = {
                if (username.isBlank() || email.isBlank()) {
                    Toast.makeText(context, "Please complete your name and email settings.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // 1) Save changes to SharedPreferences
                prefs.edit().apply {
                    putBoolean(NotificationHelper.KEY_REMINDERS_ENABLED, remindersEnabled)
                    putInt(NotificationHelper.KEY_REMINDER_HOUR, reminderHour)
                    putInt(NotificationHelper.KEY_REMINDER_MINUTE, reminderMinute)
                    putString(NotificationHelper.KEY_REMINDER_FREQUENCY, reminderFrequency)
                }.apply()

                // 2) Save changes into the main SQL Room DB and update viewModel user references
                authViewModel.updateUserProfile(
                    username = username.trim(),
                    email = email.trim(),
                    dailyGoalMinutes = dailyGoalMinutes,
                    avatarEmoji = selectedAvatar,
                    targetLanguageCode = user.targetLanguageCode
                )

                // 3) Re-schedule or cancel workers dynamically
                if (remindersEnabled) {
                    NotificationHelper.scheduleDailyReminder(
                        context,
                        reminderHour,
                        reminderMinute,
                        reminderFrequency
                    )
                } else {
                    NotificationHelper.cancelDailyReminder(context)
                }

                Toast.makeText(context, "Preferences successfully updated! 🎉", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_settings_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = DeepGreen),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = "Save settings", tint = Color.White)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Save Profile & Preferences",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
        }

        OutlinedButton(
            onClick = { authViewModel.logout() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_btn"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
            border = BorderStroke(1.dp, SoftRed)
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Log out", tint = Color(0xFFC62828))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Log Out of Session",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
