package com.democ.civilsunrisealarm.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.democ.civilsunrisealarm.domain.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val dataStore = context.settingsDataStore

    companion object {
        private val KEY_IS_ENABLED = booleanPreferencesKey("is_enabled")
        private val KEY_ENABLED_DAYS = stringSetPreferencesKey("enabled_days")
        private val KEY_OFFSET_MINUTES = intPreferencesKey("offset_minutes")
        private val KEY_SKIP_NEXT_ALARM = booleanPreferencesKey("skip_next_alarm")
    }

    override fun getSettings(): Flow<UserSettings> {
        return dataStore.data.map { preferences ->
            UserSettings(
                isEnabled = preferences[KEY_IS_ENABLED] ?: false,
                enabledDaysOfWeek = (preferences[KEY_ENABLED_DAYS] ?: emptySet())
                    .mapNotNull { DayOfWeek.valueOf(it) }
                    .toSet(),
                offsetMinutesFromCivilDawn = preferences[KEY_OFFSET_MINUTES] ?: 0,
                skipNextAlarm = preferences[KEY_SKIP_NEXT_ALARM] ?: false
            )
        }
    }

    override suspend fun updateSettings(settings: UserSettings) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_ENABLED] = settings.isEnabled
            preferences[KEY_ENABLED_DAYS] = settings.enabledDaysOfWeek.map { it.name }.toSet()
            preferences[KEY_OFFSET_MINUTES] = settings.offsetMinutesFromCivilDawn
            preferences[KEY_SKIP_NEXT_ALARM] = settings.skipNextAlarm
        }
    }

    override suspend fun getSettingsOnce(): UserSettings {
        return getSettings().first()
    }
}

