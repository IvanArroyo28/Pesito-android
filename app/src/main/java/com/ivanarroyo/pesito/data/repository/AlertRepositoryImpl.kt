package com.ivanarroyo.pesito.data.repository

import com.ivanarroyo.pesito.data.local.AlertDao
import com.ivanarroyo.pesito.data.mapper.toDomain
import com.ivanarroyo.pesito.data.mapper.toEntity
import com.ivanarroyo.pesito.domain.model.RateAlert
import com.ivanarroyo.pesito.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

class AlertRepositoryImpl @Inject constructor(
    private val dao: AlertDao,
    private val clock: Clock,
) : AlertRepository {

    override fun observeAlerts(): Flow<List<RateAlert>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getActiveAlerts(): List<RateAlert> = dao.getActive().map { it.toDomain() }

    override suspend fun add(alert: RateAlert) {
        dao.insert(alert.copy(id = 0).toEntity(createdAt = clock.millis()))
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active)
}
