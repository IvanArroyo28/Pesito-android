package com.ivanarroyo.pesito.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CurrencyEntity::class, RateEntity::class],
    // v2 (Fase 6): se agregó rates.fetchedAt.
    version = 2,
    exportSchema = false,
)
abstract class PesitoDatabase : RoomDatabase() {
    abstract fun exchangeDao(): ExchangeDao

    companion object {
        const val NAME = "pesito.db"
    }
}
