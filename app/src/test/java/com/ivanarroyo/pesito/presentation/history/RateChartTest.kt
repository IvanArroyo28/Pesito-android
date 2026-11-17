package com.ivanarroyo.pesito.presentation.history

import org.junit.Assert.assertEquals
import org.junit.Test

class RateChartTest {

    @Test
    fun `finger at the start, middle and end`() {
        assertEquals(0, indexAt(x = 0f, width = 1000f, count = 11))
        assertEquals(5, indexAt(x = 500f, width = 1000f, count = 11))
        assertEquals(10, indexAt(x = 1000f, width = 1000f, count = 11))
    }

    @Test
    fun `finger outside the chart stays inside the range`() {
        assertEquals(0, indexAt(x = -50f, width = 1000f, count = 11))
        assertEquals(10, indexAt(x = 1200f, width = 1000f, count = 11))
    }
}
