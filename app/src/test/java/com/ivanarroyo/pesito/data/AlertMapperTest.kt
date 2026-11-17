package com.ivanarroyo.pesito.data

import com.ivanarroyo.pesito.data.mapper.toDomain
import com.ivanarroyo.pesito.data.mapper.toEntity
import com.ivanarroyo.pesito.domain.model.AlertDirection
import com.ivanarroyo.pesito.domain.model.RateAlert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class AlertMapperTest {

    @Test
    fun `alert survives the trip to the database and back`() {
        val alert = RateAlert(7, "COP", "USD", AlertDirection.ABOVE, BigDecimal("0.000312"), isActive = false)

        val entity = alert.toEntity(createdAt = 123L)

        assertEquals("ABOVE", entity.direction)
        assertEquals("0.000312", entity.target)
        assertEquals(alert, entity.toDomain())
    }
}
