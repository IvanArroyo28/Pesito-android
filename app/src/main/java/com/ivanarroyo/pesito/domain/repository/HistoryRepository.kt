package com.ivanarroyo.pesito.domain.repository

import com.ivanarroyo.pesito.domain.model.RatePoint
import com.ivanarroyo.pesito.domain.model.RateSource
import java.time.LocalDate

/** El historial necesita internet: no lo guardamos en Room (serían miles de filas). */
interface HistoryRepository {
    suspend fun getHistory(
        from: String,
        to: String,
        start: LocalDate,
        end: LocalDate,
        source: RateSource,
    ): Result<List<RatePoint>>
}
