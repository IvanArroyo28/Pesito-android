package com.ivanarroyo.pesito.domain.usecase

import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import com.ivanarroyo.pesito.domain.repository.ExchangeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

class LoadConverterDataUseCaseTest {

    private class FakeRepository(
        private val currencies: Result<List<Currency>>,
        private val rates: Result<RatesSnapshot>,
    ) : ExchangeRepository {
        override suspend fun getCurrencies() = currencies
        override suspend fun getLatestRates() = rates
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
        val useCase = LoadConverterDataUseCase(
            FakeRepository(Result.success(currencies), Result.success(snapshot)),
        )

        val data = useCase().getOrThrow()

        assertEquals(listOf("COP", "USD", "EUR", "JPY"), data.currencies.map { it.code })
    }

    @Test
    fun `propagates the error`() = runBlocking {
        val useCase = LoadConverterDataUseCase(
            FakeRepository(Result.success(currencies), Result.failure(IOException("sin red"))),
        )

        val result = useCase()

        assertTrue(result.exceptionOrNull() is IOException)
    }
}
