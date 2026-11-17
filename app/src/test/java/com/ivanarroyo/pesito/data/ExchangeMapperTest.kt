package com.ivanarroyo.pesito.data

import com.ivanarroyo.pesito.data.local.RateEntity
import com.ivanarroyo.pesito.data.mapper.toEntities
import com.ivanarroyo.pesito.data.mapper.toEntityOrNull
import com.ivanarroyo.pesito.data.mapper.toSnapshotOrNull
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
        assertNull(dto.toEntityOrNull())
    }

    @Test
    fun `unknown code keeps the api name and missing symbol falls back to the code`() {
        val dto = CurrencyDto(isoCode = "zzz", name = "Test Money", symbol = null, type = "currency")

        val entity = dto.toEntityOrNull()!!

        assertEquals("ZZZ", entity.code)
        assertEquals("Test Money", entity.name)
        assertEquals("ZZZ", entity.symbol)
    }

    @Test
    fun `rates become entities with usd added and exact text`() {
        val rates = listOf(
            RateDto("2026-11-13", "USD", "COP", BigDecimal("3293.28")),
            RateDto("2026-11-12", "USD", "BTC", BigDecimal("0.0000102345678912345")),
        )

        val entities = rates.toEntities()

        assertEquals(3, entities.size)
        assertEquals(RateEntity("USD", "1", "2026-11-13"), entities.last())
        assertEquals("0.0000102345678912345", entities.first { it.code == "BTC" }.ratePerUsd)
    }

    @Test
    fun `entities become a snapshot with the latest date`() {
        val entities = listOf(
            RateEntity("USD", "1", "2026-11-13"),
            RateEntity("COP", "3293.28", "2026-11-13"),
            RateEntity("ARS", "1450.5", "2026-11-12"),
        )

        val snapshot = entities.toSnapshotOrNull()!!

        assertEquals(LocalDate.of(2026, 11, 13), snapshot.date)
        assertEquals(BigDecimal("3293.28"), snapshot.ratesPerUsd["COP"])
    }

    @Test
    fun `no saved rates means no snapshot`() {
        assertNull(emptyList<RateEntity>().toSnapshotOrNull())
    }
}
