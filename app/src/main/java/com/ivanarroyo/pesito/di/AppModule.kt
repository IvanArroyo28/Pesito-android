package com.ivanarroyo.pesito.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** El reloj del celular. En los tests usamos uno fijo. */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
