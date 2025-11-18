package com.democ.civilsunrisealarm.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.democ.civilsunrisealarm.domain.model.AlarmState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val Context.alarmStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "alarm_state")

@Singleton
class DataStoreAlarmStateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmStateRepository {

    private val dataStore = context.alarmStateDataStore
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    companion object {
        private val KEY_NEXT_ALARM_TIME = longPreferencesKey("next_alarm_time_millis")
        private val KEY_LAST_ALARM_TRIGGERED = longPreferencesKey("last_alarm_triggered_at_millis")
        private val KEY_LAST_COMPUTATION_DATE = stringPreferencesKey("last_computation_date")
        private val KEY_SKIP_APPLIED_FOR_DATE = stringPreferencesKey("skip_next_alarm_applied_for_date")
        private val KEY_DEBUG_SOLAR_NOON = longPreferencesKey("debug_solar_noon_millis")
        private val KEY_DEBUG_CIVIL_DAWN = longPreferencesKey("debug_civil_dawn_millis")
        private val KEY_DEBUG_HOUR_ANGLE = doublePreferencesKey("debug_hour_angle")
        private val KEY_DEBUG_HOURS_FROM_NOON = doublePreferencesKey("debug_hours_from_noon")
    }

    override fun getAlarmState(): Flow<AlarmState> {
        return dataStore.data.map { preferences ->
            AlarmState(
                nextAlarmTimeMillis = preferences[KEY_NEXT_ALARM_TIME],
                lastAlarmTriggeredAtMillis = preferences[KEY_LAST_ALARM_TRIGGERED],
                lastComputationDate = preferences[KEY_LAST_COMPUTATION_DATE]?.let {
                    try {
                        LocalDate.parse(it, dateFormatter)
                    } catch (e: Exception) {
                        null
                    }
                },
                skipNextAlarmAppliedForDate = preferences[KEY_SKIP_APPLIED_FOR_DATE]?.let {
                    try {
                        LocalDate.parse(it, dateFormatter)
                    } catch (e: Exception) {
                        null
                    }
                },
                debugSolarNoonMillis = preferences[KEY_DEBUG_SOLAR_NOON],
                debugCivilDawnMillis = preferences[KEY_DEBUG_CIVIL_DAWN],
                debugHourAngle = preferences[KEY_DEBUG_HOUR_ANGLE],
                debugHoursFromNoon = preferences[KEY_DEBUG_HOURS_FROM_NOON]
            )
        }
    }

    override suspend fun updateAlarmState(state: AlarmState) {
        dataStore.edit { preferences ->
            if (state.nextAlarmTimeMillis != null) {
                preferences[KEY_NEXT_ALARM_TIME] = state.nextAlarmTimeMillis
            } else {
                preferences.remove(KEY_NEXT_ALARM_TIME)
            }
            if (state.lastAlarmTriggeredAtMillis != null) {
                preferences[KEY_LAST_ALARM_TRIGGERED] = state.lastAlarmTriggeredAtMillis
            } else {
                preferences.remove(KEY_LAST_ALARM_TRIGGERED)
            }
            if (state.lastComputationDate != null) {
                preferences[KEY_LAST_COMPUTATION_DATE] = state.lastComputationDate.format(dateFormatter)
            } else {
                preferences.remove(KEY_LAST_COMPUTATION_DATE)
            }
            if (state.skipNextAlarmAppliedForDate != null) {
                preferences[KEY_SKIP_APPLIED_FOR_DATE] = state.skipNextAlarmAppliedForDate.format(dateFormatter)
            } else {
                preferences.remove(KEY_SKIP_APPLIED_FOR_DATE)
            }
            if (state.debugSolarNoonMillis != null) {
                preferences[KEY_DEBUG_SOLAR_NOON] = state.debugSolarNoonMillis
            } else {
                preferences.remove(KEY_DEBUG_SOLAR_NOON)
            }
            if (state.debugCivilDawnMillis != null) {
                preferences[KEY_DEBUG_CIVIL_DAWN] = state.debugCivilDawnMillis
            } else {
                preferences.remove(KEY_DEBUG_CIVIL_DAWN)
            }
            if (state.debugHourAngle != null) {
                preferences[KEY_DEBUG_HOUR_ANGLE] = state.debugHourAngle
            } else {
                preferences.remove(KEY_DEBUG_HOUR_ANGLE)
            }
            if (state.debugHoursFromNoon != null) {
                preferences[KEY_DEBUG_HOURS_FROM_NOON] = state.debugHoursFromNoon
            } else {
                preferences.remove(KEY_DEBUG_HOURS_FROM_NOON)
            }
        }
    }

    override suspend fun getAlarmStateOnce(): AlarmState {
        return getAlarmState().first()
    }
}

