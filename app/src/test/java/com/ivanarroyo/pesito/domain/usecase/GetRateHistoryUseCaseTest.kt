package com.ivanarroyo.pesito.domain.usecase

import com.ivanarroyo.pesito.domain.model.HistoryRange
import com.ivanarroyo.pesito.domain.model.RatePoint
import com.ivanarroyo.pesito.domain.model.RateSource
import com.ivanarroyo.pesito.domain.repository.HistoryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GetRateHistoryUseCaseTest {

    /** Repositorio falso que recuerda con qué fechas lo llamaron. */
    private class FakeRepository(private val points: List<RatePoint>) : HistoryRepository {
        var lastStart: LocalDate? = null
        var lastEnd: LocalDate? = null

        override suspend fun getHistory(
            from: String,
            to: String,
            start: LocalDate,
            end: LocalDate,
            source: RateSource,
        ): Result<List<RatePoint>> {
            lastStart = start
            lastEnd = end
            return Result.success(points)
        }
    }

    /** "Hoy" fijo: 17 de noviembre de 2026 en Bogotá. */
    private val clock = Clock.fixed(Instant.parse("2026-11-17T17:00:00Z"), ZoneId.of("America/Bogota"))

    private val points = listOf(RatePoint(LocalDate.of(2026, 11, 13), BigDecimal("3301.91")))

    @Test
    fun `one month asks for the last 30 days`() = runBlocking {
        val repository = FakeRepository(points)
        val useCase = GetRateHistoryUseCase(repository, clock)

        useCase("USD", "COP", HistoryRange.MONTH, RateSource.AVERAGE)

        assertEquals(LocalDate.of(2026, 10, 18), repository.lastStart)
        assertEquals(LocalDate.of(2026, 11, 17), repository.lastEnd)
    }

    @Test
    fun `banrep for euro is rejected without calling the api`() = runBlocking {
        val repository = FakeRepository(points)
        val useCase = GetRateHistoryUseCase(repository, clock)

        val result = useCase("EUR", "COP", HistoryRange.MONTH, RateSource.BANREP)

        assertTrue(result.isFailure)
        assertNull(repository.lastStart)
    }

    @Test
    fun `empty answer becomes null`() = runBlocking {
        val useCase = GetRateHistoryUseCase(FakeRepository(emptyList()), clock)

        assertNull(useCase("USD", "COP", HistoryRange.YEAR, RateSource.AVERAGE).getOrThrow())
    }

    @Test
    fun `banrep only exists for dollar and peso`() {
        assertTrue(GetRateHistoryUseCase.isBanRepAvailable("USD", "COP"))
        assertTrue(GetRateHistoryUseCase.isBanRepAvailable("COP", "USD"))
        assertFalse(GetRateHistoryUseCase.isBanRepAvailable("EUR", "COP"))
    }
}
