package com.ivanarroyo.pesito.di

import android.content.Context
import androidx.room.Room
import com.ivanarroyo.pesito.data.local.ExchangeDao
import com.ivanarroyo.pesito.data.local.PesitoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PesitoDatabase =
        Room.databaseBuilder(context, PesitoDatabase::class.java, PesitoDatabase.NAME)
            // La base de datos es solo una copia de la API: si cambia su estructura,
            // la borramos y se vuelve a llenar en la próxima descarga. No se pierde nada del usuario
            // (sus favoritas viven en DataStore).
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideExchangeDao(database: PesitoDatabase): ExchangeDao = database.exchangeDao()
}
