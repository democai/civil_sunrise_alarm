package com.democ.civilsunrisealarm.platform.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.democ.civilsunrisealarm.CivilSunriseAlarmApplication
import com.democ.civilsunrisealarm.domain.scheduler.AlarmScheduler
import com.democ.civilsunrisealarm.domain.scheduler.NextAlarmResult
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // Get dependencies using EntryPointAccessors
            val application = context.applicationContext as CivilSunriseAlarmApplication
            val entryPoint = EntryPointAccessors.fromApplication(
                application,
                BootReceiverEntryPoint::class.java
            )
            
            val alarmScheduler = entryPoint.alarmScheduler()
            val alarmManagerWrapper = entryPoint.alarmManagerWrapper()

            // Reschedule alarms after boot
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                when (val result = alarmScheduler.computeNextAlarm()) {
                    is NextAlarmResult.Success -> {
                        alarmManagerWrapper.scheduleAlarm(result.alarmTimeMillis)
                    }
                    else -> {
                        // Alarm not scheduled or disabled
                    }
                }
            }
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface BootReceiverEntryPoint {
    fun alarmScheduler(): AlarmScheduler
    fun alarmManagerWrapper(): AlarmManagerWrapper
}

