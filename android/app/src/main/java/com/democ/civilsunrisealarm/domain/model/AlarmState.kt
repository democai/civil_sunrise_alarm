package com.democ.civilsunrisealarm.domain.model

import java.time.LocalDate

data class AlarmState(
    val nextAlarmTimeMillis: Long? = null,
    val lastAlarmTriggeredAtMillis: Long? = null,
    val lastComputationDate: LocalDate? = null,
    val skipNextAlarmAppliedForDate: LocalDate? = null,
    // Debug fields for dawn calculation
    val debugSolarNoonMillis: Long? = null,
    val debugCivilDawnMillis: Long? = null,
    val debugHourAngle: Double? = null,
    val debugHoursFromNoon: Double? = null
)

