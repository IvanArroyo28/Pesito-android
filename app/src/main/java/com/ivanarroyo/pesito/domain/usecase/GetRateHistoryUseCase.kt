package com.ivanarroyo.pesito.domain.usecase

import com.ivanarroyo.pesito.domain.model.HistoryRange
import com.ivanarroyo.pesito.domain.model.RateHistory
import com.ivanarroyo.pesito.domain.model.RateSource
import com.ivanarroyo.pesito.domain.repository.HistoryRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Recibe un Clock en vez de llamar LocalDate.now() directamente:
 * así en los tests podemos fijar "hoy" y comprobar las fechas exactas.
 */
class GetRateHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository,
    private val clock: Clock,
) {
    /** Devuelve null si la API no tiene datos para ese periodo. */
    suspend operator fun invoke(
        from: String,
        to: String,
        range: HistoryRange,
        source: RateSource,
    ): Result<RateHistory?> {
        if (source == RateSource.BANREP && !isBanRepAvailable(from, to)) {
            return Result.failure(IllegalArgumentException("El BanRep solo publica la TRM dólar ↔ peso"))
        }
        val end = LocalDate.now(clock)
        val start = end.minusDays(range.days)
        return repository.getHistory(from, to, start, end, source).map { points ->
            points.takeIf { it.isNotEmpty() }?.let(::RateHistory)
        }
    }

    companion object {
        fun isBanRepAvailable(from: String, to: String): Boolean =
            setOf(from, to) == setOf("USD", "COP")
    }
}
