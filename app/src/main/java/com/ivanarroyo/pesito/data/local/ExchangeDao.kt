package com.ivanarroyo.pesito.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ExchangeDao {

    @Query("SELECT * FROM currencies")
    abstract fun observeCurrencies(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM rates")
    abstract fun observeRates(): Flow<List<RateEntity>>

    @Upsert
    abstract suspend fun upsertCurrencies(currencies: List<CurrencyEntity>)

    @Upsert
    abstract suspend fun upsertRates(rates: List<RateEntity>)

    @Query("DELETE FROM currencies")
    abstract suspend fun clearCurrencies()

    @Query("DELETE FROM rates")
    abstract suspend fun clearRates()

    /**
     * Reemplaza todo en una sola transacción: o se guarda todo o nada.
     * Nunca quedan monedas nuevas con tasas viejas a medias.
     */
    @Transaction
    open suspend fun replaceAll(currencies: List<CurrencyEntity>, rates: List<RateEntity>) {
        clearCurrencies()
        upsertCurrencies(currencies)
        clearRates()
        upsertRates(rates)
    }
}
