package com.democ.civilsunrisealarm.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.democ.civilsunrisealarm.domain.model.LocationState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.locationDataStore: DataStore<Preferences> by preferencesDataStore(name = "location")

@Singleton
class DataStoreLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationRepository {

    private val dataStore = context.locationDataStore

    companion object {
        private val KEY_LATITUDE = doublePreferencesKey("latitude")
        private val KEY_LONGITUDE = doublePreferencesKey("longitude")
        private val KEY_LAST_UPDATED = longPreferencesKey("last_updated_at_millis")
    }

    override fun getLocationState(): Flow<LocationState> {
        return dataStore.data.map { preferences ->
            LocationState(
                latitude = preferences[KEY_LATITUDE],
                longitude = preferences[KEY_LONGITUDE],
                lastUpdatedAtMillis = preferences[KEY_LAST_UPDATED]
            )
        }
    }

    override suspend fun updateLocation(latitude: Double, longitude: Double) {
        dataStore.edit { preferences ->
            preferences[KEY_LATITUDE] = latitude
            preferences[KEY_LONGITUDE] = longitude
            preferences[KEY_LAST_UPDATED] = System.currentTimeMillis()
        }
    }

    override suspend fun getLocationStateOnce(): LocationState {
        return getLocationState().first()
    }
}

