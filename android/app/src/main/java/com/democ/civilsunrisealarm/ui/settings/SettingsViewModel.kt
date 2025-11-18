package com.democ.civilsunrisealarm.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.democ.civilsunrisealarm.data.repository.AlarmStateRepository
import com.democ.civilsunrisealarm.data.repository.LocationRepository
import com.democ.civilsunrisealarm.data.repository.SettingsRepository
import com.democ.civilsunrisealarm.domain.model.AlarmState
import com.democ.civilsunrisealarm.domain.model.UserSettings
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.democ.civilsunrisealarm.platform.alarm.AlarmManagerWrapper
import dagger.hilt.android.qualifiers.ApplicationContext
import com.democ.civilsunrisealarm.platform.alarm.AlarmSchedulingService
import com.democ.civilsunrisealarm.platform.location.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
    private val alarmStateRepository: AlarmStateRepository,
    private val locationProvider: LocationProvider,
    private val alarmSchedulingService: AlarmSchedulingService,
    private val alarmManagerWrapper: AlarmManagerWrapper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val alarmState: StateFlow<AlarmState> = alarmStateRepository.getAlarmState()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = AlarmState()
        )

    init {
        // Load initial state
        viewModelScope.launch {
            combine(
                settingsRepository.getSettings(),
                locationRepository.getLocationState(),
                alarmStateRepository.getAlarmState()
            ) { settings, location, alarmState ->
                SettingsUiState(
                    isEnabled = settings.isEnabled,
                    enabledDays = settings.enabledDaysOfWeek,
                    offsetMinutes = settings.offsetMinutesFromCivilDawn,
                    skipNextAlarm = settings.skipNextAlarm,
                    locationState = location,
                    hasLocationPermission = locationProvider.hasLocationPermission(),
                    skipNextAlarmAppliedForDate = alarmState.skipNextAlarmAppliedForDate,
                    canScheduleExactAlarms = alarmManagerWrapper.canScheduleExactAlarms(),
                    canUseFullScreenIntent = canUseFullScreenIntent()
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        // Fetch location when screen opens (if permission granted)
        fetchLocationIfPermitted()
    }

    fun fetchLocationIfPermitted() {
        viewModelScope.launch {
            if (locationProvider.hasLocationPermission()) {
                _uiState.value = _uiState.value.copy(isLoadingLocation = true)
                val success = locationProvider.fetchAndSaveCurrentLocation()
                _uiState.value = _uiState.value.copy(
                    isLoadingLocation = false,
                    locationFetchError = !success
                )
                if (success) {
                    // Trigger alarm recalculation after location update
                    alarmSchedulingService.rescheduleAlarm()
                }
            }
        }
    }

    fun updateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settingsRepository.getSettingsOnce()
            settingsRepository.updateSettings(currentSettings.copy(isEnabled = enabled))
            alarmSchedulingService.rescheduleAlarm()
        }
    }

    fun updateDayEnabled(dayOfWeek: DayOfWeek, enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settingsRepository.getSettingsOnce()
            val newDays = if (enabled) {
                currentSettings.enabledDaysOfWeek + dayOfWeek
            } else {
                currentSettings.enabledDaysOfWeek - dayOfWeek
            }
            settingsRepository.updateSettings(
                currentSettings.copy(enabledDaysOfWeek = newDays)
            )
            alarmSchedulingService.rescheduleAlarm()
        }
    }

    fun updateOffsetMinutes(offset: Int) {
        viewModelScope.launch {
            val currentSettings = settingsRepository.getSettingsOnce()
            settingsRepository.updateSettings(
                currentSettings.copy(offsetMinutesFromCivilDawn = offset)
            )
            alarmSchedulingService.rescheduleAlarm()
        }
    }

    fun skipNextAlarm() {
        viewModelScope.launch {
            val currentSettings = settingsRepository.getSettingsOnce()
            settingsRepository.updateSettings(
                currentSettings.copy(skipNextAlarm = true)
            )
            alarmSchedulingService.rescheduleAlarm()
        }
    }

    fun unskipNextAlarm() {
        viewModelScope.launch {
            val currentSettings = settingsRepository.getSettingsOnce()
            val currentAlarmState = alarmStateRepository.getAlarmStateOnce()
            
            // Clear both the skip flag and the applied date
            settingsRepository.updateSettings(
                currentSettings.copy(skipNextAlarm = false)
            )
            alarmStateRepository.updateAlarmState(
                currentAlarmState.copy(skipNextAlarmAppliedForDate = null)
            )
            alarmSchedulingService.rescheduleAlarm()
        }
    }

    fun requestExactAlarmPermission(): Intent {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            // On older versions, open general app settings
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    fun canUseFullScreenIntent(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            notificationManager?.canUseFullScreenIntent() ?: true
        } else {
            true // Not required on older versions
        }
    }

    fun requestFullScreenIntentPermission(): Intent {
        // Use ACTION_APP_NOTIFICATION_SETTINGS for better Samsung compatibility.
        // Samsung devices have a per-app toggle at:
        // Settings → Apps → Your App → Notifications → Full-screen notifications
        // This intent deep-links directly to the app's notification settings where
        // the user can enable full-screen notifications.
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    }
}

data class SettingsUiState(
    val isEnabled: Boolean = false,
    val enabledDays: Set<DayOfWeek> = emptySet(),
    val offsetMinutes: Int = 0,
    val skipNextAlarm: Boolean = false,
    val locationState: com.democ.civilsunrisealarm.domain.model.LocationState = com.democ.civilsunrisealarm.domain.model.LocationState(),
    val hasLocationPermission: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val locationFetchError: Boolean = false,
    val skipNextAlarmAppliedForDate: java.time.LocalDate? = null,
    val canScheduleExactAlarms: Boolean = true,
    val canUseFullScreenIntent: Boolean = true
)

