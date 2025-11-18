package com.democ.civilsunrisealarm.platform.alarm

import com.democ.civilsunrisealarm.data.repository.AlarmStateRepository
import com.democ.civilsunrisealarm.domain.scheduler.AlarmScheduler
import com.democ.civilsunrisealarm.domain.scheduler.NextAlarmResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmTriggerHandler @Inject constructor(
    private val alarmStateRepository: AlarmStateRepository,
    private val alarmScheduler: AlarmScheduler,
    private val alarmManagerWrapper: AlarmManagerWrapper
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun onAlarmTriggered() {
        scope.launch {
            // Record trigger time
            val currentState = alarmStateRepository.getAlarmStateOnce()
            alarmStateRepository.updateAlarmState(
                currentState.copy(
                    lastAlarmTriggeredAtMillis = System.currentTimeMillis()
                )
            )

            // Reschedule next alarm
            when (val result = alarmScheduler.computeNextAlarm()) {
                is NextAlarmResult.Success -> {
                    alarmManagerWrapper.scheduleAlarm(result.alarmTimeMillis)
                }
                else -> {
                    alarmManagerWrapper.cancelAlarm()
                }
            }
        }
    }
}

