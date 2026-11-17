package com.ivanarroyo.pesito.domain.usecase

import com.ivanarroyo.pesito.domain.model.AlertDirection
import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RateAlert
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import com.ivanarroyo.pesito.domain.repository.AlertRepository
import com.ivanarroyo.pesito.domain.repository.ExchangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class AlertUseCasesTest {

    /** Repositorio de alertas en memoria. */
    private class FakeAlertRepository(initial: List<RateAlert> = emptyList()) : AlertRepository {
        val alerts = MutableStateFlow(initial)

        override fun observeAlerts(): Flow<List<RateAlert>> = alerts
        override suspend fun getActiveAlerts() = alerts.value.filter { it.isActive }
        override suspend fun add(alert: RateAlert) {
            alerts.value = alerts.value + alert.copy(id = alerts.value.size + 1L)
        }
        override suspend fun delete(id: Long) {
            alerts.value = alerts.value.filterNot { it.id == id }
        }
        override suspend fun setActive(id: Long, active: Boolean) {
            alerts.value = alerts.value.map { if (it.id == id) it.copy(isActive = active) else it }
        }
    }

    private class FakeExchangeRepository(private val snapshot: RatesSnapshot?) : ExchangeRepository {
        override fun observeCurrencies(): Flow<List<Currency>> = flowOf(emptyList())
        override fun observeLatestRates(): Flow<RatesSnapshot?> = flowOf(snapshot)
        override suspend fun refresh(): Result<Unit> = Result.success(Unit)
    }

    private val snapshot = RatesSnapshot(
        date = LocalDate.of(2026, 11, 13),
        ratesPerUsd = mapOf("USD" to BigDecimal.ONE, "COP" to BigDecimal("3150"), "EUR" to BigDecimal("0.85")),
    )

    @Test
    fun `triggered alerts are returned and switched off`() = runBlocking {
        val alertRepository = FakeAlertRepository(
            listOf(
                // 1 USD = 3150 COP → se cumple "baja de 3200"
                RateAlert(1, "USD", "COP", AlertDirection.BELOW, BigDecimal("3200")),
                // 1 USD = 3150 COP → NO se cumple "sube de 3500"
                RateAlert(2, "USD", "COP", AlertDirection.ABOVE, BigDecimal("3500")),
                // Apagada: se ignora aunque se cumpliría
                RateAlert(3, "USD", "COP", AlertDirection.BELOW, BigDecimal("4000"), isActive = false),
            ),
        )
        val useCase = CheckRateAlertsUseCase(alertRepository, FakeExchangeRepository(snapshot), ConvertCurrencyUseCase())

        val triggered = useCase()

        assertEquals(listOf(1L), triggered.map { it.alert.id })
        assertEquals(0, BigDecimal("3150").compareTo(triggered.single().currentRate))
        // Se apagó para no avisar lo mismo cada 12 horas.
        assertEquals(false, alertRepository.alerts.value.first { it.id == 1L }.isActive)
        assertEquals(true, alertRepository.alerts.value.first { it.id == 2L }.isActive)
    }

    @Test
    fun `no saved rates means nothing to check`() = runBlocking {
        val alertRepository = FakeAlertRepository(listOf(RateAlert(1, "USD", "COP", AlertDirection.BELOW, BigDecimal("9999"))))
        val useCase = CheckRateAlertsUseCase(alertRepository, FakeExchangeRepository(null), ConvertCurrencyUseCase())

        assertTrue(useCase().isEmpty())
    }

    @Test
    fun `create rejects zero and same currency`() = runBlocking {
        val repository = FakeAlertRepository()
        val create = CreateAlertUseCase(repository)

        assertTrue(create("USD", "COP", AlertDirection.BELOW, BigDecimal.ZERO).isFailure)
        assertTrue(create("COP", "COP", AlertDirection.BELOW, BigDecimal("1")).isFailure)
        assertTrue(create("USD", "COP", AlertDirection.BELOW, BigDecimal("3200")).isSuccess)
        assertEquals(1, repository.alerts.value.size)
    }
}
