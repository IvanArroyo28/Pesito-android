package com.ivanarroyo.pesito.data

import com.ivanarroyo.pesito.data.mapper.toDomainOrNull
import com.ivanarroyo.pesito.data.mapper.toSnapshot
import com.ivanarroyo.pesito.data.remote.dto.CurrencyDto
import com.ivanarroyo.pesito.data.remote.dto.RateDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ExchangeMapperTest {

    @Test
    fun `crypto is filtered out`() {
        val dto = CurrencyDto(isoCode = "BTC", name = "Bitcoin", symbol = "₿", type = "crypto")
        assertNull(dto.toDomainOrNull())
    }

    @Test
    fun `unknown code keeps the api name and missing symbol falls back to the code`() {
        val dto = CurrencyDto(isoCode = "zzz", name = "Test Money", symbol = null, type = "currency")

        val currency = dto.toDomainOrNull()!!

        assertEquals("ZZZ", currency.code)
        assertEquals("Test Money", currency.name)
        assertEquals("ZZZ", currency.symbol)
    }

    @Test
    fun `snapshot adds usd and keeps the latest date`() {
        val rates = listOf(
            RateDto("2026-11-13", "USD", "COP", BigDecimal("3293.28")),
            RateDto("2026-11-12", "USD", "ARS", BigDecimal("1450.5")),
        )

        val snapshot = rates.toSnapshot()

        assertEquals(LocalDate.of(2026, 11, 13), snapshot.date)
        assertEquals(BigDecimal.ONE, snapshot.ratesPerUsd["USD"])
        assertEquals(BigDecimal("3293.28"), snapshot.ratesPerUsd["COP"])
        assertEquals(3, snapshot.ratesPerUsd.size)
    }
}
