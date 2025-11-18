package com.democ.civilsunrisealarm.platform.alarm

import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.democ.civilsunrisealarm.R
import com.democ.civilsunrisealarm.ui.alarm.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        // Use v2 to force-recreate channel (emulators often corrupt channels)
        private const val CHANNEL_ID = "alarm_channel_v2"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_ALARM = "com.democ.civilsunrisealarm.ACTION_ALARM"
        private const val ACTION_SNOOZE = "com.democ.civilsunrisealarm.ACTION_SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "🔔 onReceive called! Action: ${intent.action}")

        // Diagnostic: Check process state to verify app has backgrounded (required for FSI)
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val myProc = am.runningAppProcesses?.firstOrNull { it.pid == android.os.Process.myPid() }
            val importance = myProc?.importance
            Log.d("AlarmReceiver", "📊 Process state = $importance (${processImportanceToString(importance)})")
            if (importance != null && importance < ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED) {
                Log.w("AlarmReceiver", "⚠️ App still in foreground state ($importance). FSI may not fire until app backgrounds.")
            }
        } catch (e: Exception) {
            Log.w("AlarmReceiver", "Could not check process state", e)
        }

        val pendingResult = goAsync() // Keep receiver alive for async work

        // Acquire wake lock to keep CPU on during receiver → service → notification handoff
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "CivilSunriseAlarm::WakeLock"
        )

        try {
            wakeLock.acquire(10_000L) // 10 seconds

            when (intent.action) {
                ACTION_ALARM -> {
                    Log.d("AlarmReceiver", "✅ Handling ACTION_ALARM - showing full-screen notification")
                    handleAlarm(context, isSnooze = false, triggered = true, pendingResult, wakeLock)
                }
                ACTION_SNOOZE -> {
                    Log.d("AlarmReceiver", "✅ Handling ACTION_SNOOZE - showing full-screen notification")
                    handleAlarm(context, isSnooze = true, triggered = false, pendingResult, wakeLock)
                }
                else -> {
                    Log.w("AlarmReceiver", "⚠️ Unknown action: ${intent.action}")
                    wakeLock.release()
                    pendingResult.finish()
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "❌ Error in onReceive", e)
            wakeLock.release()
            pendingResult.finish()
        }
    }

    private fun handleAlarm(
        context: Context,
        isSnooze: Boolean,
        triggered: Boolean,
        pendingResult: PendingResult,
        wakeLock: PowerManager.WakeLock
    ) {
        try {
            // 1. Create notification channel
            createNotificationChannel(context)

            // 2. Get pre-created BAL-enabled PendingIntent (created from foreground context)
            // On Android 14+, BroadcastReceivers cannot opt-in to BAL creator privileges.
            // The PendingIntent must be pre-created from a foreground context to get
            // balAllowedByPiCreator: BSP.ALLOWED instead of BSP.NONE.
            var fullScreenPendingIntent = AlarmManagerWrapper.getPrecreatedAlarmPendingIntent(isSnooze)

            if (fullScreenPendingIntent == null) {
                // Fallback: Create PendingIntent in receiver (for older Android versions or if pre-creation failed)
                Log.w("AlarmReceiver", "⚠️ Pre-created PendingIntent not available, creating fallback (may not work on Android 14+)")
                val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("triggered", triggered)
                    putExtra("isSnooze", isSnooze)
                    putExtra("from_alarm_manager", true)
                }

                val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val requestCode = (System.nanoTime() % Int.MAX_VALUE).toInt()
                fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    requestCode,
                    fullScreenIntent,
                    piFlags
                )
            } else {
                Log.d("AlarmReceiver", "✅ Using pre-created BAL-enabled PendingIntent")
            }

            // 3. Verify full-screen intent permission and channel importance (Android 14+)
            val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            if (notificationManager != null) {
                // Check FSI permission (critical diagnostic)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val canUseFSI = notificationManager.canUseFullScreenIntent()
                    if (!canUseFSI) {
                        Log.e("AlarmReceiver", "❌ NO FULL SCREEN INTENT PERMISSION - Alarm UI will NOT launch!")
                        Log.e("AlarmReceiver", "❌ User must enable: Settings → Apps → Special app access → Full-screen intents")
                    } else {
                        Log.d("AlarmReceiver", "✅ Full-screen intent permission granted")
                    }
                }

                // Check notification channel importance (critical diagnostic)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
                    val importance = channel?.importance ?: NotificationManager.IMPORTANCE_UNSPECIFIED
                    Log.d("AlarmReceiver", "📊 Channel importance = $importance (${importanceToString(importance)})")

                    // IMPORTANCE_HIGH (4) is required for full-screen intents
                    if (importance < NotificationManager.IMPORTANCE_HIGH) {
                        Log.e("AlarmReceiver", "❌ Channel importance too low! FSI will not fire. Expected: IMPORTANCE_HIGH (4), Got: $importance")
                    }
                }
            }

            // 4. Build notification with full-screen intent
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(if (isSnooze) "Snooze Alarm" else "Alarm")
                .setContentText(if (isSnooze) "Snooze alarm is ringing" else "Alarm is ringing")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .build()

            // 5. CRITICAL: Delay posting the notification to ensure the app has fully backgrounded.
            // Samsung OneUI 6/7 and Android 14/15 require a stable backgrounded window before FSI will launch.
            // On Samsung devices, if the FSI is posted < 1500-2500ms after the activity stops,
            // Samsung will demote it to a heads-up banner instead of launching the full-screen activity.
            // A 2000ms delay ensures the app has fully transitioned to CACHED state.
            Handler(Looper.getMainLooper()).postDelayed({
                // Post the notification after delay
                notificationManager?.notify(NOTIFICATION_ID, notification)
                Log.d("AlarmReceiver", "✅ Full-screen notification posted after 2s delay (app should be fully backgrounded)")

                // 6. Start foreground service AFTER another delay to avoid deprioritizing the FSI launch.
                // Starting the service too early (even 200ms after FSI) can suppress the full-screen activity launch.
                // On API 34/35, services must start at least 1500ms AFTER the FSI is posted to avoid conflicts.
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra(AlarmService.EXTRA_IS_SNOOZE, isSnooze)
                    putExtra(AlarmService.EXTRA_TRIGGERED, triggered)
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    ContextCompat.startForegroundService(context, serviceIntent)
                    Log.d("AlarmReceiver", "✅ AlarmService started after FSI launch window (1.5s after notification)")
                    // Release wake lock after service starts (service will maintain its own if needed)
                    wakeLock.release()
                    pendingResult.finish()
                }, 1500) // 1500ms delay after notification posting to allow FSI to fully launch
            }, 2000) // 2000ms delay to allow app to fully background before posting FSI notification
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "❌ Failed to handle alarm", e)
            wakeLock.release()
            pendingResult.finish()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            if (notificationManager != null) {
                // Note: We do NOT delete old channels here. Samsung treats channel deletion
                // as a personalization reset and may apply "deny" policies automatically.
                // If channel recreation is needed, it should be done manually by the user.
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Alarms",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alarm alerts"
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    setBypassDnd(true)
                    enableVibration(true)
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
                Log.d("AlarmReceiver", "✅ Notification channel created: $CHANNEL_ID with IMPORTANCE_HIGH")
            }
        }
    }

    private fun importanceToString(importance: Int): String {
        return when (importance) {
            NotificationManager.IMPORTANCE_NONE -> "NONE (0)"
            NotificationManager.IMPORTANCE_MIN -> "MIN (1)"
            NotificationManager.IMPORTANCE_LOW -> "LOW (2)"
            NotificationManager.IMPORTANCE_DEFAULT -> "DEFAULT (3) ⚠️ TOO LOW FOR FSI"
            NotificationManager.IMPORTANCE_HIGH -> "HIGH (4) ✓"
            NotificationManager.IMPORTANCE_MAX -> "MAX (5) ✓"
            else -> "UNKNOWN ($importance)"
        }
    }

    private fun processImportanceToString(importance: Int?): String {
        if (importance == null) return "NULL"
        return when (importance) {
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FOREGROUND (100) ⚠️ FSI WON'T FIRE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE -> "FOREGROUND_SERVICE (125) ⚠️ FSI WON'T FIRE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "VISIBLE (200) ⚠️ FSI WON'T FIRE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> "PERCEPTIBLE (300) ✓ FSI CAN FIRE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "CACHED (400) ✓ FSI CAN FIRE"
            @Suppress("DEPRECATION")
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_EMPTY -> "EMPTY (500) ✓ FSI CAN FIRE"
            else -> "UNKNOWN ($importance)"
        }
    }
}
