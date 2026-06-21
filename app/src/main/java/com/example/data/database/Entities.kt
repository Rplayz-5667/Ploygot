package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val username: String,
    val passwordHash: String? = null, // null for Google SSO
    val isGoogleUser: Boolean = false,
    val targetLanguageCode: String = "ja",
    val streak: Int = 1,
    val xp: Int = 0,
    val level: Int = 1,
    val dailyGoalMinutes: Int = 15,
    val avatarEmoji: String = "🐼",
    val lastActiveDate: String? = null,
    val pronunciationScore: Int = 0
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val languageCode: String,
    val phrase: String,
    val translation: String,
    val phonetic: String? = null,
    val usageExample: String? = null,
    val usageTranslation: String? = null,
    val box: Int = 1, // Leap / Leitner box system (1 to 5)
    val nextReviewTime: Long = System.currentTimeMillis(),
    val isMastered: Boolean = false,
    val lastTestedTime: Long? = null
)

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val languageCode: String,
    val activityType: String, // "Quiz", "Flashcard", "Chat", "Pronunciation"
    val durationSeconds: Int,
    val xpEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_history")
data class ChatHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val languageCode: String,
    val sender: String, // "user" or "ai"
    val content: String,
    val translation: String? = null,
    val audioPath: String? = null,
    val feedback: String? = null, // Stores JSON or text of pronunciation correction
    val timestamp: Long = System.currentTimeMillis()
)
