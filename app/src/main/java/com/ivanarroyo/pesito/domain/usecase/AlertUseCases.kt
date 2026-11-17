package com.ivanarroyo.pesito.domain.usecase

import com.ivanarroyo.pesito.domain.model.AlertDirection
import com.ivanarroyo.pesito.domain.model.RateAlert
import com.ivanarroyo.pesito.domain.model.TriggeredAlert
import com.ivanarroyo.pesito.domain.repository.AlertRepository
import com.ivanarroyo.pesito.domain.repository.ExchangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import javax.inject.Inject

class ObserveAlertsUseCase @Inject constructor(
    private val repository: AlertRepository,
) {
    operator fun invoke(): Flow<List<RateAlert>> = repository.observeAlerts()
}

class CreateAlertUseCase @Inject constructor(
    private val repository: AlertRepository,
) {
    suspend operator fun invoke(
        from: String,
        to: String,
        direction: AlertDirection,
        target: BigDecimal,
    ): Result<Unit> {
        if (from == to) return Result.failure(IllegalArgumentException("Las monedas deben ser distintas"))
        if (target.signum() <= 0) return Result.failure(IllegalArgumentException("El valor debe ser mayor que cero"))
        repository.add(RateAlert(from = from, to = to, direction = direction, target = target))
        return Result.success(Unit)
    }
}

class DeleteAlertUseCase @Inject constructor(
    private val repository: AlertRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}

class SetAlertActiveUseCase @Inject constructor(
    private val repository: AlertRepository,
) {
    suspend operator fun invoke(id: Long, active: Boolean) = repository.setActive(id, active)
}

/**
 * Revisa las alertas activas contra las tasas guardadas.
 * Las que se cumplen se desactivan (avisamos UNA vez) y se devuelven para notificar.
 */
class CheckRateAlertsUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    private val exchangeRepository: ExchangeRepository,
    private val convertCurrency: ConvertCurrencyUseCase,
) {
    suspend operator fun invoke(): List<TriggeredAlert> {
        val snapshot = exchangeRepository.observeLatestRates().first() ?: return emptyList()

        val triggered = alertRepository.getActiveAlerts().mapNotNull { alert ->
            val rate = convertCurrency(snapshot, BigDecimal.ONE, alert.from, alert.to) ?: return@mapNotNull null
            if (alert.isTriggeredBy(rate)) TriggeredAlert(alert, rate) else null
        }
        triggered.forEach { alertRepository.setActive(it.alert.id, active = false) }
        return triggered
    }
}
