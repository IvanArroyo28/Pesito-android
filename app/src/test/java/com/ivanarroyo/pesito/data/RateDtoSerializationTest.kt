package com.ivanarroyo.pesito.data

import com.ivanarroyo.pesito.data.remote.dto.CurrencyDto
import com.ivanarroyo.pesito.data.remote.dto.RateDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class RateDtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `rate keeps every decimal exactly`() {
        val body = """[{"date":"2026-11-13","base":"USD","quote":"BTC","rate":0.0000102345678912345}]"""

        val rates = json.decodeFromString<List<RateDto>>(body)

        assertEquals(BigDecimal("0.0000102345678912345"), rates.single().rate)
    }

    @Test
    fun `currency json with extra fields is parsed`() {
        val body = """[{"iso_code":"COP","iso_numeric":"170","name":"Colombian Peso","symbol":"$",
            "type":"currency","start_date":"1999-01-04","end_date":null}]"""

        val currency = json.decodeFromString<List<CurrencyDto>>(body).single()

        assertEquals("COP", currency.isoCode)
        assertEquals("currency", currency.type)
    }
}
