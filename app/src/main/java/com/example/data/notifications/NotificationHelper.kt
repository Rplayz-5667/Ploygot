package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.MainActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "polyglot_daily_reminders"
    private const val CHANNEL_NAME = "Daily Streak Reminders"
    private const val CHANNEL_DESC = "Reminders to complete daily lessons and keep your language streak active!"
    const val PREFS_NAME = "polyglot_notification_prefs"
    const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    const val KEY_REMINDER_HOUR = "reminder_hour"
    const val KEY_REMINDER_MINUTE = "reminder_minute"
    const val KEY_REMINDER_FREQUENCY = "reminder_frequency"
    const val KEY_FCM_TOKEN = "fcm_token"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel initialized.")
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showStreakReminderNotification(context: Context, streakValue: Int, isFCM: Boolean = false) {
        // Create intent to open MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Custom, engaging, encouraging title & text based on current streak days
        val title = if (streakValue > 0) {
            "🔥 Your $streakValue-Day Streak is Calling!"
        } else {
            "🌱 Let's start your language learning streak!"
        }

        val text = if (streakValue > 0) {
            "Don't lose your hard work! Keep it going with just 5 minutes of practice today."
        } else {
            "Unlock new worlds today! Tap to start your first interactive lesson."
        }

        val footerText = if (isFCM) "Received via Polyglot Cloud Messaging" else "Polyglot Lesson Reminder"

        // Build notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(footerText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n\nDaily practice builds vocabulary and builds cognitive muscle. Speak with the AI Chat companion or learn cards now!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001, builder.build())
        Log.d(TAG, "Streak reminder notification fired.")
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val frequency = prefs.getString(KEY_REMINDER_FREQUENCY, "daily") ?: "daily"
        scheduleDailyReminder(context, hour, minute, frequency)
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int, frequency: String) {
        val workManager = WorkManager.getInstance(context)

        // Calculate initial delay
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = Calendar.getInstance()
        if (calendar.before(now)) {
            // If the time has passed today, schedule it for tomorrow
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelaySecs = (calendar.timeInMillis - now.timeInMillis) / 1000

        val (interval, timeUnit) = when (frequency) {
            "twice_daily" -> Pair(12L, TimeUnit.HOURS)
            "weekly" -> Pair(7L, TimeUnit.DAYS)
            else -> Pair(24L, TimeUnit.HOURS) // "daily"
        }

        // Create periodic work request for selected frequency
        val reminderRequest = PeriodicWorkRequestBuilder<StreakReminderWorker>(interval, timeUnit)
            .setInitialDelay(initialDelaySecs, TimeUnit.SECONDS)
            .addTag("streak_reminder")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "streak_reminder_unique",
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
        Log.d(TAG, "Scheduled unique periodic daily reminder work at $hour:$minute with frequency $frequency (interval: $interval, unit: $timeUnit) and initial delay of $initialDelaySecs seconds")
    }

    fun cancelDailyReminder(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("streak_reminder_unique")
        Log.d(TAG, "Cancelled unique periodic daily reminder work.")
    }
}
