package com.democ.civilsunrisealarm.platform.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.democ.civilsunrisealarm.ui.alarm.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.democ.civilsunrisealarm.ACTION_ALARM" -> {
                // Start the alarm activity for main alarm
                val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("triggered", true)
                    putExtra("isSnooze", false)
                }
                context.startActivity(alarmIntent)
            }
            "com.democ.civilsunrisealarm.ACTION_SNOOZE" -> {
                // Start the alarm activity for snooze
                val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("triggered", false) // Don't trigger next alarm scheduling for snooze
                    putExtra("isSnooze", true)
                }
                context.startActivity(alarmIntent)
            }
        }
    }
}

