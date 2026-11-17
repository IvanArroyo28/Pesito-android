package com.ivanarroyo.pesito.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class RateHistoryTest {

    private val history = RateHistory(
        listOf(
            RatePoint(LocalDate.of(2026, 10, 1), BigDecimal("3200")),
            RatePoint(LocalDate.of(2026, 10, 2), BigDecimal("3100")),
            RatePoint(LocalDate.of(2026, 10, 3), BigDecimal("3400")),
            RatePoint(LocalDate.of(2026, 10, 4), BigDecimal("3280")),
        ),
    )

    @Test
    fun `min and max find the right days`() {
        assertEquals(LocalDate.of(2026, 10, 2), history.min.date)
        assertEquals(LocalDate.of(2026, 10, 3), history.max.date)
    }

    @Test
    fun `change percent compares first and last day`() {
        // (3280 - 3200) / 3200 * 100 = 2.5 %
        assertEquals(BigDecimal("2.50"), history.changePercent)
    }

    @Test
    fun `negative change`() {
        val down = RateHistory(
            listOf(
                RatePoint(LocalDate.of(2026, 10, 1), BigDecimal("4000")),
                RatePoint(LocalDate.of(2026, 10, 2), BigDecimal("3900")),
            ),
        )
        assertEquals(BigDecimal("-2.50"), down.changePercent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty history is not allowed`() {
        RateHistory(emptyList())
    }
}
