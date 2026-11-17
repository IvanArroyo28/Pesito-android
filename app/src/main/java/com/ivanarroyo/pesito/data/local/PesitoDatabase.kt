package com.ivanarroyo.pesito.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CurrencyEntity::class, RateEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PesitoDatabase : RoomDatabase() {
    abstract fun exchangeDao(): ExchangeDao

    companion object {
        const val NAME = "pesito.db"
    }
}
