package com.democ.civilsunrisealarm.di

import com.democ.civilsunrisealarm.data.repository.AlarmStateRepository
import com.democ.civilsunrisealarm.data.repository.DataStoreAlarmStateRepository
import com.democ.civilsunrisealarm.data.repository.DataStoreLocationRepository
import com.democ.civilsunrisealarm.data.repository.DataStoreSettingsRepository
import com.democ.civilsunrisealarm.data.repository.LocationRepository
import com.democ.civilsunrisealarm.data.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        dataStoreSettingsRepository: DataStoreSettingsRepository
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        dataStoreLocationRepository: DataStoreLocationRepository
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindAlarmStateRepository(
        dataStoreAlarmStateRepository: DataStoreAlarmStateRepository
    ): AlarmStateRepository
}

