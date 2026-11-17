package com.ivanarroyo.pesito.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CurrencyEntity::class, RateEntity::class, AlertEntity::class],
    // v2 (Fase 6): se agregó rates.fetchedAt. v3 (Fase 8): tabla alerts.
    version = 3,
    exportSchema = false,
)
abstract class PesitoDatabase : RoomDatabase() {
    abstract fun exchangeDao(): ExchangeDao
    abstract fun alertDao(): AlertDao

    companion object {
        const val NAME = "pesito.db"
    }
}
