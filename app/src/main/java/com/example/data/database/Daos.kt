package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET targetLanguageCode = :langCode WHERE id = :userId")
    suspend fun updateTargetLanguage(userId: Long, langCode: String)

    @Query("UPDATE users SET xp = xp + :xpDelta WHERE id = :userId")
    suspend fun addXp(userId: Long, xpDelta: Int)

    // Simply increment streak
    @Query("UPDATE users SET streak = :streak WHERE id = :userId")
    suspend fun updateStreak(userId: Long, streak: Int)

    @Query("SELECT * FROM users ORDER BY xp DESC")
    fun getLeaderboardFlow(): Flow<List<UserEntity>>
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE userId = :userId AND languageCode = :languageCode ORDER BY id DESC")
    fun getFlashcardsFlow(userId: Long, languageCode: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE userId = :userId AND languageCode = :languageCode AND nextReviewTime <= :now ORDER BY box ASC, id ASC")
    suspend fun getDueFlashcards(userId: Long, languageCode: String, now: Long): List<FlashcardEntity>

    @Query("SELECT COUNT(*) FROM flashcards WHERE userId = :userId AND languageCode = :languageCode")
    fun getFlashcardCountFlow(userId: Long, languageCode: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE userId = :userId AND languageCode = :languageCode AND isMastered = 1")
    fun getMasteredCountFlow(userId: Long, languageCode: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE userId = :userId AND languageCode = :languageCode")
    suspend fun clearFlashcards(userId: Long, languageCode: String)
}

@Dao
interface StudyLogDao {
    @Query("SELECT * FROM study_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getStudyLogsFlow(userId: Long): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_logs WHERE userId = :userId")
    suspend fun getStudyLogs(userId: Long): List<StudyLogEntity>

    @Query("SELECT SUM(xpEarned) FROM study_logs WHERE userId = :userId")
    fun getTotalXpFlow(userId: Long): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyLog(log: StudyLogEntity)
}

@Dao
interface ChatHistoryDao {
    @Query("SELECT * FROM chat_history WHERE userId = :userId AND languageCode = :languageCode ORDER BY timestamp ASC")
    fun getChatHistoryFlow(userId: Long, languageCode: String): Flow<List<ChatHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatHistoryEntity): Long

    @Query("DELETE FROM chat_history WHERE userId = :userId AND languageCode = :languageCode")
    suspend fun clearChatHistory(userId: Long, languageCode: String)
}
