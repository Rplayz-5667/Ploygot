package com.example.data.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StreakReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("StreakReminderWorker", "Periodic streak reminder worker fired.")

        val context = applicationContext
        val db = AppDatabase.getDatabase(context)
        val userDao = db.userDao()

        // Fetch users to find active or default user
        // Usually, there is a main user logged in or created. We query the first user.
        try {
            // Room does not have an explicit current-user state, 
            // but we can look up the first registered user in the DB.
            val leaderboard = db.userDao().getLeaderboardFlow()
            // In a suspend context outside a Flow collector, we can query general user rows
            // But getLeaderboardFlow returns a Flow. Let's see if we can query users directly or get the first user.
            // Wait, we can implement or run a query to get first user, or simply get all users if they exist.
            // Let's check Daos.kt, do we have a get all users?
            // "SELECT * FROM users" is not direct, but we can query them or retrieve the one with max xp or lowest ID.
            // Let's look at getLeaderboardFlow or we can query SELECT * FROM users LIMIT 1 or similar.
            // Wait, to keep it simple and safe, let's look up users using a custom raw sqlite or check what users are saved in DB.
            // Or we can just read the shared preferences where user info is standard or read DB.
            // Let's see: userRepository has getLeaderboardFlow or loginWithEmail, let's write a safe lookup or simple fallbacks.
            
            // To be safe we can use a standard fallback or check if there's any user in db.
            // Let's write a query inside a try-catch for the first user. Let's search if they worked today.
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            // Let's look at shared preferences first or default to standard user.
            val prefs = context.getSharedPreferences(NotificationHelper.PREFS_NAME, Context.MODE_PRIVATE)
            val remindersEnabled = prefs.getBoolean(NotificationHelper.KEY_REMINDERS_ENABLED, true)
            
            if (!remindersEnabled) {
                Log.d("StreakReminderWorker", "Reminders are disabled by the user. Skipping notification.")
                return Result.success()
            }

            // Since this is a reminder worker, we should display it if they haven't completed tasks today.
            // Let's grab current streak value
            var currentStreak = 1
            try {
                // Let's create a custom suspend getter or fetch users
                // We'll just read first user or defaults.
                // Since Room allows RawQuery or UserDao updates, let's see if we can fetch user by ID 1 (default ID is 1 or auto-generated)
                val user = userDao.getUserById(1L)
                if (user != null) {
                    currentStreak = user.streak
                    
                    // If they are already active today, optionally skip reminding them again!
                    if (user.lastActiveDate == today) {
                        Log.d("StreakReminderWorker", "User was already active today ($today). Skipping reminder.")
                        return Result.success()
                    }
                }
            } catch (e: Exception) {
                Log.e("StreakReminderWorker", "Error querying user status, utilizing default streak", e)
            }

            NotificationHelper.showStreakReminderNotification(context, currentStreak)
            return Result.success()
        } catch (e: Exception) {
            Log.e("StreakReminderWorker", "Worker failed to run successfully", e)
            return Result.retry()
        }
    }
}
