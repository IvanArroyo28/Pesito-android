package com.ivanarroyo.pesito.di

import com.ivanarroyo.pesito.data.preferences.DataStorePreferencesRepository
import com.ivanarroyo.pesito.data.repository.AlertRepositoryImpl
import com.ivanarroyo.pesito.data.repository.ExchangeRepositoryImpl
import com.ivanarroyo.pesito.data.repository.HistoryRepositoryImpl
import com.ivanarroyo.pesito.domain.repository.AlertNotifier
import com.ivanarroyo.pesito.domain.repository.AlertRepository
import com.ivanarroyo.pesito.domain.repository.ExchangeRepository
import com.ivanarroyo.pesito.domain.repository.HistoryRepository
import com.ivanarroyo.pesito.domain.repository.PreferencesRepository
import com.ivanarroyo.pesito.presentation.notifications.AndroidAlertNotifier
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
    abstract fun bindExchangeRepository(impl: ExchangeRepositoryImpl): ExchangeRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: DataStorePreferencesRepository): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    @Binds
    @Singleton
    abstract fun bindAlertNotifier(impl: AndroidAlertNotifier): AlertNotifier
}
