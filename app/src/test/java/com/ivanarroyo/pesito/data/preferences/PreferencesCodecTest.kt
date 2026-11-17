package com.ivanarroyo.pesito.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesCodecTest {

    @Test
    fun `encode and decode keep the order`() {
        val codes = listOf("MXN", "USD", "EUR")
        assertEquals(codes, PreferencesCodec.decodeCodes(PreferencesCodec.encodeCodes(codes)))
    }

    @Test
    fun `empty text means no favorites`() =
        assertEquals(emptyList<String>(), PreferencesCodec.decodeCodes(""))

    @Test
    fun `decode cleans spaces, lowercase and duplicates`() =
        assertEquals(listOf("USD", "EUR"), PreferencesCodec.decodeCodes(" usd,EUR,,usd "))
}
