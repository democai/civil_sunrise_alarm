package com.democ.civilsunrisealarm.domain.scheduler

import com.democ.civilsunrisealarm.data.repository.AlarmStateRepository
import com.democ.civilsunrisealarm.data.repository.LocationRepository
import com.democ.civilsunrisealarm.data.repository.SettingsRepository
import com.democ.civilsunrisealarm.domain.calculator.DawnCalculator
import com.democ.civilsunrisealarm.domain.model.AlarmState
import com.democ.civilsunrisealarm.domain.model.LocationState
import com.democ.civilsunrisealarm.domain.model.UserSettings
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core business logic for scheduling alarms based on civil dawn calculations.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
    private val alarmStateRepository: AlarmStateRepository,
    private val dawnCalculator: DawnCalculator
) {

    /**
     * Computes and returns the next alarm time based on current settings and location.
     * Returns null if alarm cannot be scheduled (disabled, no location, etc.)
     */
    suspend fun computeNextAlarm(): NextAlarmResult {
        val settings = settingsRepository.getSettingsOnce()
        val locationState = locationRepository.getLocationStateOnce()
        val currentAlarmState = alarmStateRepository.getAlarmStateOnce()
        val now = System.currentTimeMillis()

        // If disabled, clear alarm state and return
        if (!settings.isEnabled) {
            alarmStateRepository.updateAlarmState(
                currentAlarmState.copy(nextAlarmTimeMillis = null)
            )
            return NextAlarmResult.Disabled
        }

        // Check location availability
        if (!locationState.hasLocation()) {
            alarmStateRepository.updateAlarmState(
                currentAlarmState.copy(nextAlarmTimeMillis = null)
            )
            return NextAlarmResult.NoLocation
        }

        val latitude = locationState.latitude!!
        val longitude = locationState.longitude!!
        val timeZone = ZoneId.systemDefault()

        // Find next valid date and ensure alarm time is in the future
        var currentDate = findNextValidDate(
            settings = settings,
            currentAlarmState = currentAlarmState,
            timeZone = timeZone
        ) ?: return NextAlarmResult.NoValidDate

        var alarmTimeMillis: Long? = null
        var finalDate: LocalDate? = null
        val maxAttempts = 365 // Prevent infinite loops

        // Loop to find a date with alarm time in the future
        var debugSolarNoon: Long? = null
        var debugCivilDawn: Long? = null
        var debugHourAngle: Double? = null
        var debugHoursFromNoon: Double? = null
        
        for (attempt in 0 until maxAttempts) {
            // Calculate civil dawn for this date with debug info
            val dawnResult = dawnCalculator.calculateCivilDawnWithDebug(
                latitude = latitude,
                longitude = longitude,
                date = currentDate,
                timeZone = timeZone
            )
            
            if (dawnResult.dawnTime == null) {
                // If calculation failed, try next date
                currentDate = findNextValidDate(
                    settings = settings,
                    currentAlarmState = currentAlarmState,
                    timeZone = timeZone,
                    startFrom = currentDate.plusDays(1)
                ) ?: return NextAlarmResult.CalculationFailed
                continue
            }

            // Store debug values from first successful calculation
            if (debugSolarNoon == null) {
                debugSolarNoon = dawnResult.solarNoonMillis
                debugCivilDawn = dawnResult.dawnTime.atZone(timeZone).toInstant().toEpochMilli()
                debugHourAngle = dawnResult.hourAngle
                debugHoursFromNoon = dawnResult.hoursFromNoon
            }

            // Apply offset
            val alarmTime = dawnResult.dawnTime.plusMinutes(settings.offsetMinutesFromCivilDawn.toLong())
            val calculatedMillis = alarmTime.atZone(timeZone).toInstant().toEpochMilli()

            // Check if alarm time is in the future
            if (calculatedMillis > now) {
                alarmTimeMillis = calculatedMillis
                finalDate = currentDate
                break
            } else {
                // Alarm time is in the past, try next valid date
                currentDate = findNextValidDate(
                    settings = settings,
                    currentAlarmState = currentAlarmState,
                    timeZone = timeZone,
                    startFrom = currentDate.plusDays(1)
                ) ?: return NextAlarmResult.NoValidDate
            }
        }

        // If we couldn't find a valid alarm time, return error
        if (alarmTimeMillis == null || finalDate == null) {
            return NextAlarmResult.NoValidDate
        }

        // Handle skip logic: skip next alarm only once per user action
        // Once a date has been marked as skipped, keep skipping disabled until user explicitly unskips
        if (settings.skipNextAlarm && 
            currentAlarmState.skipNextAlarmAppliedForDate == null) {
            // First time skipping - mark this date as skipped
            val updatedState = currentAlarmState.copy(
                skipNextAlarmAppliedForDate = finalDate
            )
            alarmStateRepository.updateAlarmState(updatedState)
            
            // DO NOT clear skip flag here - let user explicitly unskip if they want
            // Find next date after this one
            findNextValidDate(
                settings = settings.copy(skipNextAlarm = false),
                currentAlarmState = updatedState,
                timeZone = timeZone,
                startFrom = finalDate.plusDays(1)
            )?.let { skippedDate ->
                // Calculate alarm time for the skipped date
                var skipDate = skippedDate
                var skipAlarmTimeMillis: Long? = null
                var skipFinalDate: LocalDate? = null
                var skipDebugSolarNoon: Long? = null
                var skipDebugCivilDawn: Long? = null
                var skipDebugHourAngle: Double? = null
                var skipDebugHoursFromNoon: Double? = null
                
                for (attempt in 0 until maxAttempts) {
                    val skipDawnResult = dawnCalculator.calculateCivilDawnWithDebug(
                        latitude, longitude, skipDate, timeZone
                    )
                    
                    if (skipDawnResult.dawnTime == null) {
                        skipDate = findNextValidDate(
                            settings = settings.copy(skipNextAlarm = false),
                            currentAlarmState = updatedState,
                            timeZone = timeZone,
                            startFrom = skipDate.plusDays(1)
                        ) ?: return NextAlarmResult.NoValidDate
                        continue
                    }
                    
                    // Store debug values from first successful calculation
                    if (skipDebugSolarNoon == null) {
                        skipDebugSolarNoon = skipDawnResult.solarNoonMillis
                        skipDebugCivilDawn = skipDawnResult.dawnTime.atZone(timeZone).toInstant().toEpochMilli()
                        skipDebugHourAngle = skipDawnResult.hourAngle
                        skipDebugHoursFromNoon = skipDawnResult.hoursFromNoon
                    }
                    
                    val calculatedSkipMillis = skipDawnResult.dawnTime
                        .plusMinutes(settings.offsetMinutesFromCivilDawn.toLong())
                        .atZone(timeZone)
                        .toInstant()
                        .toEpochMilli()
                    
                    if (calculatedSkipMillis > now) {
                        skipAlarmTimeMillis = calculatedSkipMillis
                        skipFinalDate = skipDate
                        break
                    } else {
                        skipDate = findNextValidDate(
                            settings = settings.copy(skipNextAlarm = false),
                            currentAlarmState = updatedState,
                            timeZone = timeZone,
                            startFrom = skipDate.plusDays(1)
                        ) ?: return NextAlarmResult.NoValidDate
                    }
                }
                
                if (skipAlarmTimeMillis != null && skipFinalDate != null) {
                    alarmStateRepository.updateAlarmState(
                        updatedState.copy(
                            nextAlarmTimeMillis = skipAlarmTimeMillis,
                            lastComputationDate = skipFinalDate,
                            debugSolarNoonMillis = skipDebugSolarNoon,
                            debugCivilDawnMillis = skipDebugCivilDawn,
                            debugHourAngle = skipDebugHourAngle,
                            debugHoursFromNoon = skipDebugHoursFromNoon
                        )
                    )
                    return NextAlarmResult.Success(skipAlarmTimeMillis)
                }
            }
            
            // If couldn't find next valid date, return no valid date
            return NextAlarmResult.NoValidDate
        } else {
            // Update alarm state with debug info
            alarmStateRepository.updateAlarmState(
                currentAlarmState.copy(
                    nextAlarmTimeMillis = alarmTimeMillis,
                    lastComputationDate = finalDate,
                    debugSolarNoonMillis = debugSolarNoon,
                    debugCivilDawnMillis = debugCivilDawn,
                    debugHourAngle = debugHourAngle,
                    debugHoursFromNoon = debugHoursFromNoon
                )
            )
            return NextAlarmResult.Success(alarmTimeMillis)
        }
    }

    private fun findNextValidDate(
        settings: UserSettings,
        currentAlarmState: AlarmState,
        timeZone: ZoneId,
        startFrom: LocalDate? = null
    ): LocalDate? {
        if (settings.enabledDaysOfWeek.isEmpty()) {
            return null
        }

        val today = LocalDate.now(timeZone)
        var currentDate = startFrom ?: today
        val maxDaysToCheck = 365 // Prevent infinite loops

        for (i in 0 until maxDaysToCheck) {
            val dayOfWeek = currentDate.dayOfWeek

            // Check if this day is enabled
            if (dayOfWeek in settings.enabledDaysOfWeek) {
                // Check if this date should be skipped
                if (!settings.skipNextAlarm || 
                    currentAlarmState.skipNextAlarmAppliedForDate != currentDate) {
                    return currentDate
                }
            }

            currentDate = currentDate.plusDays(1)
        }

        return null
    }
}

sealed class NextAlarmResult {
    data class Success(val alarmTimeMillis: Long) : NextAlarmResult()
    object Disabled : NextAlarmResult()
    object NoLocation : NextAlarmResult()
    object NoValidDate : NextAlarmResult()
    object CalculationFailed : NextAlarmResult()
}

