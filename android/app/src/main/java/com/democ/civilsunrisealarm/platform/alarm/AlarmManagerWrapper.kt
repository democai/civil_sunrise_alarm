package com.democ.civilsunrisealarm.platform.alarm

import android.app.ActivityOptions
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.democ.civilsunrisealarm.platform.alarm.AlarmReceiver
import com.democ.civilsunrisealarm.ui.alarm.AlarmActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmManagerWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Checks if the app can schedule exact alarms.
     * On Android 12+ (API 31+), this requires SCHEDULE_EXACT_ALARM permission.
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // On older versions, exact alarms are always available
        }
    }

    /**
     * Pre-creates a BAL-enabled PendingIntent for the alarm activity.
     * This MUST be called from a foreground context (e.g., when user sets an alarm in the UI).
     * 
     * On Android 14+, BroadcastReceivers cannot opt-in to BAL creator privileges.
     * By pre-creating the PendingIntent from a foreground context, we ensure:
     * - balAllowedByPiCreator: BSP.ALLOWED (instead of BSP.NONE)
     * - The full-screen intent will actually launch the activity
     */
    fun precreateAlarmPendingIntent(isSnooze: Boolean = false) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // BAL opt-in not needed on older versions
            return
        }

        try {
            val alarmActivityIntent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("from_alarm_manager", true)
                putExtra("triggered", !isSnooze)
                putExtra("isSnooze", isSnooze)
            }

            val activityOptions = ActivityOptions.makeBasic().apply {
                setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            }.toBundle()

            val requestCode = if (isSnooze) REQUEST_CODE_FSI_SNOOZE else REQUEST_CODE_FSI_ALARM
            val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE

            val pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                alarmActivityIntent,
                piFlags,
                activityOptions
            )

            if (isSnooze) {
                precreatedSnoozePendingIntent = pendingIntent
                Log.d("AlarmManagerWrapper", "✅ Pre-created BAL-enabled snooze PendingIntent")
            } else {
                precreatedAlarmPendingIntent = pendingIntent
                Log.d("AlarmManagerWrapper", "✅ Pre-created BAL-enabled alarm PendingIntent")
            }
        } catch (e: Exception) {
            Log.e("AlarmManagerWrapper", "❌ Failed to pre-create BAL PendingIntent", e)
        }
    }

    companion object {
        private const val REQUEST_CODE_ALARM = 1001
        private const val REQUEST_CODE_SNOOZE = 1002
        private const val REQUEST_CODE_FSI_ALARM = 999999
        private const val REQUEST_CODE_FSI_SNOOZE = 999998
        private const val ACTION_ALARM = "com.democ.civilsunrisealarm.ACTION_ALARM"
        private const val ACTION_SNOOZE = "com.democ.civilsunrisealarm.ACTION_SNOOZE"
        
        // Pre-created BAL-enabled PendingIntents (created from foreground context)
        @Volatile
        private var precreatedAlarmPendingIntent: PendingIntent? = null
        
        @Volatile
        private var precreatedSnoozePendingIntent: PendingIntent? = null
        
        /**
         * Gets the pre-created BAL-enabled PendingIntent for use in AlarmReceiver.
         * Returns null if not yet created (fallback to creating in receiver for older Android versions).
         */
        @JvmStatic
        fun getPrecreatedAlarmPendingIntent(isSnooze: Boolean = false): PendingIntent? {
            return if (isSnooze) {
                precreatedSnoozePendingIntent
            } else {
                precreatedAlarmPendingIntent
            }
        }
    }

    fun scheduleAlarm(alarmTimeMillis: Long) {
        try {
            // Pre-create BAL-enabled PendingIntent from foreground context (required for Android 14+)
            precreateAlarmPendingIntent(isSnooze = false)
            
            // Use broadcast PendingIntent for Android 14/15 compatibility.
            // The AlarmReceiver will handle launching the activity via full-screen notification.
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ALARM,
                intent,
                flags
            )

            val alarmTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(alarmTimeMillis))
            Log.d("AlarmManagerWrapper", "🔔 Scheduling alarm for: $alarmTime (${alarmTimeMillis}ms)")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (canScheduleExactAlarms()) {
                    Log.d("AlarmManagerWrapper", "✅ Using setExactAndAllowWhileIdle (API ${Build.VERSION.SDK_INT})")
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm if exact alarms not available
                    Log.w("AlarmManagerWrapper", "⚠️ Exact alarms not available, using inexact alarm")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Log.d("AlarmManagerWrapper", "✅ Using setExact (API ${Build.VERSION.SDK_INT})")
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmTimeMillis,
                    pendingIntent
                )
            } else {
                Log.d("AlarmManagerWrapper", "✅ Using set (API ${Build.VERSION.SDK_INT})")
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    alarmTimeMillis,
                    pendingIntent
                )
            }
            Log.d("AlarmManagerWrapper", "✅ Alarm scheduled successfully!")
        } catch (e: SecurityException) {
            Log.e("AlarmManagerWrapper", "❌ SecurityException scheduling alarm", e)
            throw e
        } catch (e: Exception) {
            Log.e("AlarmManagerWrapper", "❌ Error scheduling alarm", e)
            throw e
        }
    }

    fun cancelAlarm() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Schedules a snooze alarm for the specified time.
     * Uses a different request code and action to avoid interfering with the main alarm.
     */
    fun scheduleSnoozeAlarm(snoozeTimeMillis: Long) {
        try {
            // Pre-create BAL-enabled PendingIntent from foreground context (required for Android 14+)
            precreateAlarmPendingIntent(isSnooze = true)
            
            // Use broadcast PendingIntent for Android 14/15 compatibility.
            // The AlarmReceiver will handle launching the activity via full-screen notification.
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_SNOOZE
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_SNOOZE,
                intent,
                flags
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTimeMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm if exact alarms not available
                    Log.w("AlarmManagerWrapper", "Exact alarms not available for snooze, using inexact alarm")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTimeMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("AlarmManagerWrapper", "SecurityException scheduling snooze alarm", e)
            throw e
        } catch (e: Exception) {
            Log.e("AlarmManagerWrapper", "Error scheduling snooze alarm", e)
            throw e
        }
    }

    /**
     * Cancels any scheduled snooze alarm.
     */
    fun cancelSnoozeAlarm() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SNOOZE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}

