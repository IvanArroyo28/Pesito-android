package com.ivanarroyo.pesito.domain.usecase

import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import com.ivanarroyo.pesito.domain.repository.ExchangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ObserveConverterDataUseCaseTest {

    private class FakeRepository(
        private val currencies: List<Currency>,
        private val snapshot: RatesSnapshot?,
    ) : ExchangeRepository {
        override fun observeCurrencies(): Flow<List<Currency>> = flowOf(currencies)
        override fun observeLatestRates(): Flow<RatesSnapshot?> = flowOf(snapshot)
        override suspend fun refresh(): Result<Unit> = Result.success(Unit)
    }

    private val snapshot = RatesSnapshot(
        date = LocalDate.of(2026, 11, 13),
        ratesPerUsd = mapOf(
            "USD" to BigDecimal.ONE,
            "COP" to BigDecimal("3293.28"),
            "EUR" to BigDecimal("0.86"),
            "JPY" to BigDecimal("150"),
        ),
    )

    private val currencies = listOf(
        Currency("JPY", "Yen", "¥"),
        Currency("EUR", "Euro", "€"),
        Currency("AAA", "Sin tasa", "A"),
        Currency("USD", "Dólar estadounidense", "US$"),
        Currency("COP", "Peso colombiano", "$"),
    )

    @Test
    fun `puts popular currencies first and drops currencies without rate`() = runBlocking {
        val useCase = ObserveConverterDataUseCase(FakeRepository(currencies, snapshot))

        val data = useCase().first()!!

        assertEquals(listOf("COP", "USD", "EUR", "JPY"), data.currencies.map { it.code })
    }

    @Test
    fun `emits null when nothing is saved yet`() = runBlocking {
        val useCase = ObserveConverterDataUseCase(FakeRepository(emptyList(), null))

        assertNull(useCase().first())
    }
}
