package com.democ.civilsunrisealarm.platform.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.democ.civilsunrisealarm.data.repository.AlarmStateRepository
import com.democ.civilsunrisealarm.domain.scheduler.AlarmScheduler
import com.democ.civilsunrisealarm.domain.scheduler.NextAlarmResult
import com.democ.civilsunrisealarm.platform.alarm.AlarmManagerWrapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Periodic background worker that checks alarm state and reschedules if needed.
 * This is the "cron" job that runs periodically to ensure alarms are properly scheduled.
 */
@HiltWorker
class AlarmCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val alarmScheduler: AlarmScheduler,
    private val alarmStateRepository: AlarmStateRepository,
    private val alarmManagerWrapper: AlarmManagerWrapper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val alarmState = alarmStateRepository.getAlarmStateOnce()
            val now = System.currentTimeMillis()

            // Check if alarm needs rescheduling:
            // 1. No alarm scheduled
            // 2. Alarm time is in the past
            // 3. Alarm state is inconsistent
            val needsRescheduling = alarmState.nextAlarmTimeMillis == null ||
                    alarmState.nextAlarmTimeMillis <= now

            if (needsRescheduling) {
                when (val result = alarmScheduler.computeNextAlarm()) {
                    is NextAlarmResult.Success -> {
                        alarmManagerWrapper.scheduleAlarm(result.alarmTimeMillis)
                        Result.success()
                    }
                    else -> {
                        // Alarm disabled, no location, or no valid date
                        alarmManagerWrapper.cancelAlarm()
                        Result.success()
                    }
                }
            } else {
                // Alarm is properly scheduled, no action needed
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

