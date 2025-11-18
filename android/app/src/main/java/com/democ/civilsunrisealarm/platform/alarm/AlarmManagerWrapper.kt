package com.democ.civilsunrisealarm.platform.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.democ.civilsunrisealarm.platform.alarm.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmManagerWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val REQUEST_CODE_ALARM = 1001
        private const val REQUEST_CODE_SNOOZE = 1002
        private const val ACTION_ALARM = "com.democ.civilsunrisealarm.ACTION_ALARM"
        private const val ACTION_SNOOZE = "com.democ.civilsunrisealarm.ACTION_SNOOZE"
    }

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

    fun scheduleAlarm(alarmTimeMillis: Long) {
        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm if exact alarms not available
                    Log.w("AlarmManagerWrapper", "Exact alarms not available, using inexact alarm")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    alarmTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("AlarmManagerWrapper", "SecurityException scheduling alarm", e)
            throw e
        } catch (e: Exception) {
            Log.e("AlarmManagerWrapper", "Error scheduling alarm", e)
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
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_SNOOZE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_SNOOZE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

