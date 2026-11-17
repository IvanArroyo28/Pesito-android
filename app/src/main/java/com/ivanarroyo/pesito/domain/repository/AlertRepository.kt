package com.ivanarroyo.pesito.domain.repository

import com.ivanarroyo.pesito.domain.model.RateAlert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    /** Todas las alertas, las más nuevas primero. */
    fun observeAlerts(): Flow<List<RateAlert>>
    suspend fun getActiveAlerts(): List<RateAlert>
    suspend fun add(alert: RateAlert)
    suspend fun delete(id: Long)
    suspend fun setActive(id: Long, active: Boolean)
}
