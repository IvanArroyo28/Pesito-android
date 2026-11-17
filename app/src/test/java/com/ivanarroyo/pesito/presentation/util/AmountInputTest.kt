package com.ivanarroyo.pesito.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class AmountInputTest {

    @Test
    fun `dot becomes comma`() = assertEquals("12,5", AmountInput.sanitize("12.5"))

    @Test
    fun `letters are rejected`() = assertNull(AmountInput.sanitize("12a"))

    @Test
    fun `two separators are rejected`() = assertNull(AmountInput.sanitize("1,2,3"))

    @Test
    fun `more than two decimals are rejected`() = assertNull(AmountInput.sanitize("1,234"))

    @Test
    fun `empty is allowed`() = assertEquals("", AmountInput.sanitize(""))

    @Test
    fun `parse uses comma as decimal`() = assertEquals(BigDecimal("1234.5"), AmountInput.parse("1234,5"))

    @Test
    fun `parse of empty or lone comma is null`() {
        assertNull(AmountInput.parse(""))
        assertNull(AmountInput.parse(","))
    }
}
