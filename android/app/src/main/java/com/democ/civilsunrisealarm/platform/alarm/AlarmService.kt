package com.democ.civilsunrisealarm.platform.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.democ.civilsunrisealarm.R

/**
 * Foreground service that maintains process priority during alarm.
 * The AlarmActivity is launched by the full-screen notification posted by AlarmReceiver.
 * This service keeps the app alive and maintains foreground priority.
 */
class AlarmService : Service() {

    companion object {
        private const val CHANNEL_ID = "alarm_service_channel"
        private const val NOTIFICATION_ID = 1002
        const val EXTRA_IS_SNOOZE = "is_snooze"
        const val EXTRA_TRIGGERED = "triggered"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AlarmService", "🔔 AlarmService started")

        // Start as foreground service (required for Android 8.0+)
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // The AlarmActivity will be launched by the full-screen notification posted by AlarmReceiver.
        // This service maintains process priority and keeps the app alive during the alarm.
        // On Android 14/15, foreground services cannot launch activities directly - only full-screen
        // notifications with proper BAL opt-in can launch activities.
        Log.d("AlarmService", "✅ AlarmService running to maintain process priority")

        // Stop the service after a short delay to allow activity to start
        android.os.Handler(mainLooper).postDelayed({
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }, 1000)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alarm Service"
            val descriptionText = "Service for launching alarm screen"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setBypassDnd(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alarm")
            .setContentText("Starting alarm...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }
}

