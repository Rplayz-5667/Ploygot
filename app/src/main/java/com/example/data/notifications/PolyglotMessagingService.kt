package com.example.data.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PolyglotMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token generated: $token")
        
        // Save token in shared preferences so we can display/use it inside the app
        val prefs = getSharedPreferences(NotificationHelper.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(NotificationHelper.KEY_FCM_TOKEN, token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Incoming FCM message received from: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

        // Trigger streak reminder notification matching FCM content
        val title = remoteMessage.notification?.title ?: "🔥 Polyglot Streak Reminder!"
        val body = remoteMessage.notification?.body ?: "Time for your daily language lesson. Keep your streak active!"
        
        // Trigger a native system notification
        val prefs = getSharedPreferences(NotificationHelper.PREFS_NAME, Context.MODE_PRIVATE)
        val remindersEnabled = prefs.getBoolean(NotificationHelper.KEY_REMINDERS_ENABLED, true)
        
        if (remindersEnabled) {
            NotificationHelper.showStreakReminderNotification(
                context = this,
                streakValue = 14, // Use a representative value or extract from payload if present
                isFCM = true
            )
        }
    }

    companion object {
        private const val TAG = "PolyglotMessaging"
    }
}
