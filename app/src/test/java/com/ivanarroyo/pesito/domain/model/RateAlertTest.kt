package com.ivanarroyo.pesito.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class RateAlertTest {

    private val below = RateAlert(from = "USD", to = "COP", direction = AlertDirection.BELOW, target = BigDecimal("3200"))
    private val above = below.copy(direction = AlertDirection.ABOVE)

    @Test
    fun `below triggers when the rate drops to the target or less`() {
        assertTrue(below.isTriggeredBy(BigDecimal("3199.99")))
        assertTrue(below.isTriggeredBy(BigDecimal("3200.00"))) // justo en el valor también cuenta
        assertFalse(below.isTriggeredBy(BigDecimal("3200.01")))
    }

    @Test
    fun `above triggers when the rate rises to the target or more`() {
        assertTrue(above.isTriggeredBy(BigDecimal("3300")))
        assertTrue(above.isTriggeredBy(BigDecimal("3200")))
        assertFalse(above.isTriggeredBy(BigDecimal("3100")))
    }
}
