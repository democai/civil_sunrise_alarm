package com.democ.civilsunrisealarm.domain.model

import java.time.DayOfWeek

data class UserSettings(
    val isEnabled: Boolean = false,
    val enabledDaysOfWeek: Set<DayOfWeek> = emptySet(),
    val offsetMinutesFromCivilDawn: Int = 0,
    val skipNextAlarm: Boolean = false
)

