package com.ivanarroyo.pesito.domain.usecase

import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ConvertCurrencyUseCaseTest {

    private val convert = ConvertCurrencyUseCase()

    private val snapshot = RatesSnapshot(
        date = LocalDate.of(2026, 11, 13),
        ratesPerUsd = mapOf(
            "USD" to BigDecimal.ONE,
            "COP" to BigDecimal("3293.28"),
            "EUR" to BigDecimal("0.86"),
        ),
    )

    @Test
    fun `usd to cop multiplies by the rate`() {
        val result = convert(snapshot, BigDecimal("100"), "USD", "COP")!!
        assertEquals(0, BigDecimal("329328").compareTo(result))
    }

    @Test
    fun `cop to usd divides by the rate`() {
        val result = convert(snapshot, BigDecimal("3293.28"), "COP", "USD")!!
        assertEquals(0, BigDecimal.ONE.compareTo(result))
    }

    @Test
    fun `cross rate eur to cop goes through the dollar`() {
        // 1 EUR = 3293.28 / 0.86 COP = 3829.395348837209...
        val result = convert(snapshot, BigDecimal.ONE, "EUR", "COP")!!
        assertEquals("3829.40", result.setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString())
    }

    @Test
    fun `same currency returns the same amount`() {
        val amount = BigDecimal("12.34")
        assertEquals(amount, convert(snapshot, amount, "COP", "COP"))
    }

    @Test
    fun `unknown currency returns null`() {
        assertNull(convert(snapshot, BigDecimal.ONE, "USD", "XYZ"))
    }

    @Test
    fun `no floating point errors`() {
        // Con Double, 0.1 + 0.2 da 0.30000000000000004. Con BigDecimal no.
        val result = convert(snapshot, BigDecimal("0.3"), "USD", "USD")
        assertEquals(BigDecimal("0.3"), result)
    }
}
