package com.ivanarroyo.pesito.di

import com.ivanarroyo.pesito.data.preferences.DataStorePreferencesRepository
import com.ivanarroyo.pesito.data.repository.ExchangeRepositoryImpl
import com.ivanarroyo.pesito.domain.repository.ExchangeRepository
import com.ivanarroyo.pesito.domain.repository.PreferencesRepository
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
}
