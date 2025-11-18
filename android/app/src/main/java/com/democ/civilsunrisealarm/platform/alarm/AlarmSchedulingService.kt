package com.democ.civilsunrisealarm.platform.alarm

import com.democ.civilsunrisealarm.domain.scheduler.AlarmScheduler
import com.democ.civilsunrisealarm.domain.scheduler.NextAlarmResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for scheduling alarms. Used by Settings and other components
 * to trigger alarm recalculation and scheduling.
 */
@Singleton
class AlarmSchedulingService @Inject constructor(
    private val alarmScheduler: AlarmScheduler,
    private val alarmManagerWrapper: AlarmManagerWrapper
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Recalculates and schedules the next alarm.
     * Should be called when settings change, location updates, or after alarm fires.
     */
    fun rescheduleAlarm() {
        scope.launch {
            when (val result = alarmScheduler.computeNextAlarm()) {
                is NextAlarmResult.Success -> {
                    alarmManagerWrapper.scheduleAlarm(result.alarmTimeMillis)
                }
                is NextAlarmResult.Disabled -> {
                    alarmManagerWrapper.cancelAlarm()
                }
                else -> {
                    // No location, no valid date, or calculation failed
                    alarmManagerWrapper.cancelAlarm()
                }
            }
        }
    }
}

