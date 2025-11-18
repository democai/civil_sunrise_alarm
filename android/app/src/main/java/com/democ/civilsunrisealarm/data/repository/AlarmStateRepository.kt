package com.democ.civilsunrisealarm.data.repository

import com.democ.civilsunrisealarm.domain.model.AlarmState
import kotlinx.coroutines.flow.Flow

interface AlarmStateRepository {
    fun getAlarmState(): Flow<AlarmState>
    suspend fun updateAlarmState(state: AlarmState)
    suspend fun getAlarmStateOnce(): AlarmState
}

