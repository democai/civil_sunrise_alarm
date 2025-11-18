package com.democ.civilsunrisealarm.data.repository

import com.democ.civilsunrisealarm.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<UserSettings>
    suspend fun updateSettings(settings: UserSettings)
    suspend fun getSettingsOnce(): UserSettings
}

