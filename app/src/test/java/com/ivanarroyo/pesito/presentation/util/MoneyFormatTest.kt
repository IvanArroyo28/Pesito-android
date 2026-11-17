package com.ivanarroyo.pesito.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyFormatTest {

    @Test
    fun `big numbers use dots for thousands and comma for decimals`() =
        assertEquals("3.293.280,00", MoneyFormat.format(BigDecimal("3293280")))

    @Test
    fun `rounds to two decimals`() =
        assertEquals("3.293,28", MoneyFormat.format(BigDecimal("3293.2849")))

    @Test
    fun `small numbers keep four significant digits`() =
        assertEquals("0,0003037", MoneyFormat.format(BigDecimal("0.000303689")))

    @Test
    fun `zero shows two decimals`() =
        assertEquals("0,00", MoneyFormat.format(BigDecimal.ZERO))
}
