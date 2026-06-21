package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

data class StreakReward(
    val dayCount: Int,
    val xpAwarded: Int,
    val isNewStreak: Boolean,
    val isConsecutive: Boolean,
    val message: String
)

class UserRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val userDao = db.userDao()
    private val studyLogDao = db.studyLogDao()

    private val _currentUserState = MutableStateFlow<UserEntity?>(null)
    val currentUserState: StateFlow<UserEntity?> = _currentUserState.asStateFlow()

    private val _streakRewardState = MutableStateFlow<StreakReward?>(null)
    val streakRewardState: StateFlow<StreakReward?> = _streakRewardState.asStateFlow()

    fun clearStreakReward() {
        _streakRewardState.value = null
    }

    suspend fun loadInitialUser() {
        // Look up default guest user or last active user
        val defaultEmail = "learner@polyglot.ai"
        var user = userDao.getUserByEmail(defaultEmail)
        if (user == null) {
            // Seed premium learner account
            user = UserEntity(
                email = defaultEmail,
                username = "Global Scholar",
                passwordHash = "polyglot123",
                isGoogleUser = false,
                streak = 14, // seed streak as shown in preview design
                xp = 340,
                level = 3,
                targetLanguageCode = "ja",
                avatarEmoji = "🎓",
                pronunciationScore = 75
            )
            val id = userDao.insertUser(user)
            user = user.copy(id = id)
        }
        
        // Seed friendly global leaderboard rivals!
        val rivals = listOf(
            UserEntity(email = "soraneko@polyglot.ai", username = "Sora Neko", passwordHash = "rival1", streak = 18, xp = 520, level = 4, avatarEmoji = "🦊", pronunciationScore = 94),
            UserEntity(email = "panda@polyglot.ai", username = "Polyglot Panda", passwordHash = "rival2", streak = 9, xp = 420, level = 3, avatarEmoji = "🐼", pronunciationScore = 88),
            UserEntity(email = "owl@polyglot.ai", username = "Wise Owl", passwordHash = "rival3", streak = 30, xp = 280, level = 2, avatarEmoji = "🦉", pronunciationScore = 92),
            UserEntity(email = "bunny@polyglot.ai", username = "Cafe Bunny", passwordHash = "rival4", streak = 5, xp = 160, level = 2, avatarEmoji = "🐰", pronunciationScore = 72),
            UserEntity(email = "ninja@polyglot.ai", username = "Lingo Ninja", passwordHash = "rival5", streak = 2, xp = 80, level = 1, avatarEmoji = "🥷", pronunciationScore = 60)
        )
        for (rival in rivals) {
            if (userDao.getUserByEmail(rival.email) == null) {
                userDao.insertUser(rival)
            }
        }

        checkAndUpdateDailyStreak(user)
    }

    fun getLeaderboardFlow(): Flow<List<UserEntity>> {
        return userDao.getLeaderboardFlow()
    }

    suspend fun loginWithEmail(email: String, passwordText: String): Boolean {
        val user = userDao.getUserByEmail(email)
        return if (user != null && user.passwordHash == passwordText) {
            checkAndUpdateDailyStreak(user)
            true
        } else {
            false
        }
    }

    suspend fun signUpWithEmail(email: String, username: String, passwordText: String): Boolean {
        val existing = userDao.getUserByEmail(email)
        if (existing != null) return false

        val newUser = UserEntity(
            email = email,
            username = username,
            passwordHash = passwordText,
            isGoogleUser = false,
            streak = 1,
            xp = 0,
            level = 1,
            targetLanguageCode = "en"
        )
        val id = userDao.insertUser(newUser)
        val insertedUser = newUser.copy(id = id)
        checkAndUpdateDailyStreak(insertedUser)
        return true
    }

    suspend fun loginOrSignUpWithGoogle(email: String, displayName: String, profilePicEmoji: String): Boolean {
        var user = userDao.getUserByEmail(email)
        if (user == null) {
            // New user signed up using Google!
            user = UserEntity(
                email = email,
                username = displayName,
                passwordHash = null, // SSO
                isGoogleUser = true,
                streak = 1,
                xp = 20, // free starter points
                level = 1,
                targetLanguageCode = "ja",
                avatarEmoji = profilePicEmoji
            )
            val id = userDao.insertUser(user)
            user = user.copy(id = id)
        }
        checkAndUpdateDailyStreak(user)
        return true
    }

    suspend fun updateTargetLanguage(langCode: String) {
        val current = _currentUserState.value ?: return
        val updated = current.copy(targetLanguageCode = langCode)
        userDao.updateUser(updated)
        _currentUserState.value = updated
    }

    suspend fun updateUserProfile(username: String, email: String, dailyGoalMinutes: Int, avatarEmoji: String, targetLanguageCode: String) {
        val current = _currentUserState.value ?: return
        val updated = current.copy(
            username = username,
            email = email,
            dailyGoalMinutes = dailyGoalMinutes,
            avatarEmoji = avatarEmoji,
            targetLanguageCode = targetLanguageCode
        )
        userDao.updateUser(updated)
        _currentUserState.value = updated
    }

    suspend fun updatePronunciationScore(newScore: Int) {
        val current = _currentUserState.value ?: return
        if (newScore > current.pronunciationScore) {
            val updated = current.copy(pronunciationScore = newScore)
            userDao.updateUser(updated)
            _currentUserState.value = updated
        }
    }

    suspend fun awardXp(xpEarned: Int, activityType: String) {
        val current = _currentUserState.value ?: return
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        val yesterday = getYesterdayDateString()

        // 1. Calculate realistic session duration based on activity
        val sessionDurationSec = when (activityType) {
            "Flashcard" -> 60
            "Flashcard Mastery" -> 120
            "Chat" -> 180
            "Conversation Practice" -> 180
            "Pronunciation" -> 90
            "Grammar Mastery Study Set" -> 300
            "Custom Card Creation" -> 60
            else -> 60
        }

        // 2. Fetch all logs today before inserting this one to sum up total minutes
        var minutesAlreadyToday = 0.0
        try {
            val allLogs = studyLogDao.getStudyLogs(current.id)
            minutesAlreadyToday = allLogs.filter { log ->
                sdf.format(Date(log.timestamp)) == today
            }.sumOf { it.durationSeconds } / 60.0
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed fetching study logs for today", e)
        }

        val totalMinutesWithCurrent = minutesAlreadyToday + (sessionDurationSec / 60.0)

        // 3. Keep track of user state changes
        var updatedXp = current.xp + xpEarned
        var updatedLevel = (updatedXp / 150) + 1
        var updatedStreak = current.streak
        var updatedLastActive = current.lastActiveDate

        // Did we meet the daily goal just now?
        val studyGoalMetToday = totalMinutesWithCurrent >= current.dailyGoalMinutes
        val goalAlreadyCompletedBeforeThisActivity = minutesAlreadyToday >= current.dailyGoalMinutes

        if (studyGoalMetToday && !goalAlreadyCompletedBeforeThisActivity && current.lastActiveDate != today) {
            // First time reaching daily goal today! Unlock/increment streak celebration pop up
            if (current.lastActiveDate == yesterday) {
                updatedStreak = current.streak + 1
                val streakBonus = (updatedStreak * 5).coerceAtMost(50)
                updatedXp += (20 + streakBonus)
                updatedLevel = (updatedXp / 150) + 1
                updatedLastActive = today

                _streakRewardState.value = StreakReward(
                    dayCount = updatedStreak,
                    xpAwarded = 20 + streakBonus,
                    isNewStreak = false,
                    isConsecutive = true,
                    message = "Consecutive day study goal unlocked! Day $updatedStreak bonus: +${20 + streakBonus} XP 🔥"
                )
            } else {
                updatedStreak = 1
                updatedXp += 15
                updatedLevel = (updatedXp / 150) + 1
                updatedLastActive = today

                _streakRewardState.value = StreakReward(
                    dayCount = 1,
                    xpAwarded = 15,
                    isNewStreak = true,
                    isConsecutive = false,
                    message = "Awesome! You reached your daily learning goal of ${current.dailyGoalMinutes} min! Streak started: Day 1 (+15 XP) ⚡"
                )
            }
        }

        val updatedUser = current.copy(
            xp = updatedXp,
            level = updatedLevel,
            streak = updatedStreak,
            lastActiveDate = updatedLastActive
        )

        userDao.updateUser(updatedUser)
        _currentUserState.value = updatedUser

        // Simulated concurrent study activity by peer rivals
        if (Math.random() < 0.35) {
            val rivals = listOf("soraneko@polyglot.ai", "panda@polyglot.ai", "owl@polyglot.ai", "bunny@polyglot.ai")
            val selectedEmail = rivals.random()
            userDao.getUserByEmail(selectedEmail)?.let { peer ->
                val peerXpBump = listOf(10, 15, 20).random()
                val peerNewXp = peer.xp + peerXpBump
                val peerNewLevel = (peerNewXp / 150) + 1
                userDao.updateUser(peer.copy(xp = peerNewXp, level = peerNewLevel))
            }
        }

        // Record todays study log entry
        try {
            studyLogDao.insertStudyLog(
                StudyLogEntity(
                    userId = current.id,
                    languageCode = current.targetLanguageCode,
                    activityType = activityType,
                    durationSeconds = sessionDurationSec,
                    xpEarned = xpEarned
                )
            )
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed reporting study log", e)
        }
    }

    fun getTotalXpFlow(): Flow<Int?> {
        val currentId = _currentUserState.value?.id ?: 1L
        return studyLogDao.getTotalXpFlow(currentId)
    }

    fun getStudyLogsFlow(): Flow<List<StudyLogEntity>> {
        val currentId = _currentUserState.value?.id ?: 1L
        return studyLogDao.getStudyLogsFlow(currentId)
    }

    fun logout() {
        _currentUserState.value = null
    }

    suspend fun checkAndUpdateDailyStreak(user: UserEntity) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        val lastActive = user.lastActiveDate

        if (lastActive == today) {
            _currentUserState.value = user
            return
        }

        val yesterday = getYesterdayDateString()
        
        // If they did not complete their daily goal yesterday (meaning their lastActiveDate is not yesterday and they haven't achieved goal today), their streak resets to 0.
        val keepStreak = lastActive == yesterday || lastActive == today || lastActive == null
        val updatedStreak = if (keepStreak) user.streak else 0

        val updated = user.copy(
            streak = updatedStreak
        )
        userDao.updateUser(updated)
        _currentUserState.value = updated
    }

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(cal.time)
    }
}
