package com.ivanarroyo.pesito.data

import com.ivanarroyo.pesito.data.local.CurrencyEntity
import com.ivanarroyo.pesito.data.local.ExchangeDao
import com.ivanarroyo.pesito.data.local.RateEntity
import com.ivanarroyo.pesito.data.remote.FrankfurterApi
import com.ivanarroyo.pesito.data.remote.dto.CurrencyDto
import com.ivanarroyo.pesito.data.remote.dto.RateDto
import com.ivanarroyo.pesito.data.repository.ExchangeRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

/** Prueba lo más importante de la Fase 3: sin internet, lo guardado NO se pierde. */
class ExchangeRepositoryImplTest {

    /** Base de datos falsa en memoria. */
    private class FakeDao : ExchangeDao() {
        val currencies = MutableStateFlow<List<CurrencyEntity>>(emptyList())
        val rates = MutableStateFlow<List<RateEntity>>(emptyList())

        override fun observeCurrencies(): Flow<List<CurrencyEntity>> = currencies
        override fun observeRates(): Flow<List<RateEntity>> = rates
        override suspend fun upsertCurrencies(currencies: List<CurrencyEntity>) {
            this.currencies.value = this.currencies.value + currencies
        }
        override suspend fun upsertRates(rates: List<RateEntity>) {
            this.rates.value = this.rates.value + rates
        }
        override suspend fun clearCurrencies() { currencies.value = emptyList() }
        override suspend fun clearRates() { rates.value = emptyList() }
    }

    /** API falsa: podemos "apagar el internet" con [offline]. */
    private class FakeApi(var copRate: String = "3293.28") : FrankfurterApi {
        var offline = false

        override suspend fun getCurrencies(): List<CurrencyDto> {
            if (offline) throw IOException("sin internet")
            return listOf(
                CurrencyDto("COP", "Colombian Peso", "$", "currency"),
                CurrencyDto("USD", "US Dollar", "$", "currency"),
                CurrencyDto("BTC", "Bitcoin", "₿", "crypto"),
            )
        }

        override suspend fun getLatestRates(base: String): List<RateDto> {
            if (offline) throw IOException("sin internet")
            return listOf(RateDto("2026-11-13", "USD", "COP", BigDecimal(copRate)))
        }

        override suspend fun getRateHistory(
            base: String,
            quotes: String,
            from: String,
            to: String,
            providers: String?,
        ): List<RateDto> = emptyList()
    }

    @Test
    fun `refresh saves currencies and rates`() = runBlocking {
        val dao = FakeDao()
        val repository = ExchangeRepositoryImpl(FakeApi(), dao)

        val result = repository.refresh()

        assertTrue(result.isSuccess)
        assertEquals(listOf("COP", "USD"), repository.observeCurrencies().first().map { it.code })
        assertEquals(BigDecimal("3293.28"), repository.observeLatestRates().first()!!.ratesPerUsd["COP"])
    }

    @Test
    fun `without internet the saved rates are kept`() = runBlocking {
        val dao = FakeDao()
        val api = FakeApi()
        val repository = ExchangeRepositoryImpl(api, dao)
        repository.refresh()

        api.offline = true
        val result = repository.refresh()

        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(BigDecimal("3293.28"), repository.observeLatestRates().first()!!.ratesPerUsd["COP"])
    }

    @Test
    fun `a new refresh replaces the old rates`() = runBlocking {
        val dao = FakeDao()
        val api = FakeApi()
        val repository = ExchangeRepositoryImpl(api, dao)
        repository.refresh()

        api.copRate = "3300.00"
        repository.refresh()

        assertEquals(BigDecimal("3300.00"), repository.observeLatestRates().first()!!.ratesPerUsd["COP"])
        assertEquals(2, dao.rates.value.size) // COP + USD, sin duplicados
    }
}
