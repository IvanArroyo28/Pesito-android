package com.ivanarroyo.pesito.data

import com.ivanarroyo.pesito.data.remote.FrankfurterApi
import com.ivanarroyo.pesito.data.remote.dto.CurrencyDto
import com.ivanarroyo.pesito.data.remote.dto.RateDto
import com.ivanarroyo.pesito.data.repository.HistoryRepositoryImpl
import com.ivanarroyo.pesito.domain.model.RateSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class HistoryRepositoryImplTest {

    /** API falsa que cuenta cuántas veces la llamaron y con qué parámetros. */
    private class FakeApi : FrankfurterApi {
        var historyCalls = 0
        var lastBase: String? = null
        var lastProviders: String? = null

        override suspend fun getCurrencies(): List<CurrencyDto> = emptyList()
        override suspend fun getLatestRates(base: String): List<RateDto> = emptyList()

        override suspend fun getRateHistory(
            base: String,
            quotes: String,
            from: String,
            to: String,
            providers: String?,
        ): List<RateDto> {
            historyCalls++
            lastBase = base
            lastProviders = providers
            // Llegan desordenadas a propósito: el repositorio debe ordenarlas.
            return listOf(
                RateDto("2026-11-14", "USD", "COP", BigDecimal("4000")),
                RateDto("2026-11-13", "USD", "COP", BigDecimal("3200")),
            )
        }
    }

    private val start = LocalDate.of(2026, 10, 18)
    private val end = LocalDate.of(2026, 11, 17)

    @Test
    fun `points come sorted by date`() = runBlocking {
        val repository = HistoryRepositoryImpl(FakeApi())

        val points = repository.getHistory("USD", "COP", start, end, RateSource.AVERAGE).getOrThrow()

        assertEquals(listOf(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 14)), points.map { it.date })
    }

    @Test
    fun `banrep peso to dollar asks usd-cop and inverts it`() = runBlocking {
        val api = FakeApi()
        val repository = HistoryRepositoryImpl(api)

        val points = repository.getHistory("COP", "USD", start, end, RateSource.BANREP).getOrThrow()

        assertEquals("usd", api.lastBase)
        assertEquals("BANREP", api.lastProviders)
        assertEquals(0, BigDecimal("0.00025").compareTo(points.last().rate)) // 1 / 4000
    }

    @Test
    fun `second identical request uses the cache`() = runBlocking {
        val api = FakeApi()
        val repository = HistoryRepositoryImpl(api)

        repository.getHistory("USD", "COP", start, end, RateSource.AVERAGE)
        repository.getHistory("USD", "COP", start, end, RateSource.AVERAGE)

        assertEquals(1, api.historyCalls)
    }
}
