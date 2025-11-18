package com.democ.civilsunrisealarm.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.democ.civilsunrisealarm.data.repository.AlarmStateRepository
import com.democ.civilsunrisealarm.data.repository.LocationRepository
import com.democ.civilsunrisealarm.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    locationRepository: LocationRepository,
    alarmStateRepository: AlarmStateRepository
) : ViewModel() {

    val uiState: StateFlow<StatusUiState> = combine(
        settingsRepository.getSettings(),
        locationRepository.getLocationState(),
        alarmStateRepository.getAlarmState()
    ) { settings, location, alarmState ->
        StatusUiState(
            isEnabled = settings.isEnabled,
            skipNextAlarm = settings.skipNextAlarm,
            enabledDays = settings.enabledDaysOfWeek,
            offsetMinutes = settings.offsetMinutesFromCivilDawn,
            locationState = location,
            nextAlarmTimeMillis = alarmState.nextAlarmTimeMillis,
            debugSolarNoonMillis = alarmState.debugSolarNoonMillis,
            debugCivilDawnMillis = alarmState.debugCivilDawnMillis,
            debugHourAngle = alarmState.debugHourAngle,
            debugHoursFromNoon = alarmState.debugHoursFromNoon
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = StatusUiState()
    )
}

data class StatusUiState(
    val isEnabled: Boolean = false,
    val skipNextAlarm: Boolean = false,
    val enabledDays: Set<DayOfWeek> = emptySet(),
    val offsetMinutes: Int = 0,
    val locationState: com.democ.civilsunrisealarm.domain.model.LocationState = com.democ.civilsunrisealarm.domain.model.LocationState(),
    val nextAlarmTimeMillis: Long? = null,
    val debugSolarNoonMillis: Long? = null,
    val debugCivilDawnMillis: Long? = null,
    val debugHourAngle: Double? = null,
    val debugHoursFromNoon: Double? = null
)

