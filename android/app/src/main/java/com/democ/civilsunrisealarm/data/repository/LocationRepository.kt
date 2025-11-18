package com.democ.civilsunrisealarm.data.repository

import com.democ.civilsunrisealarm.domain.model.LocationState
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getLocationState(): Flow<LocationState>
    suspend fun updateLocation(latitude: Double, longitude: Double)
    suspend fun getLocationStateOnce(): LocationState
}

