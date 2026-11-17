package com.ivanarroyo.pesito.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ivanarroyo.pesito.domain.repository.AlertNotifier
import com.ivanarroyo.pesito.domain.usecase.CheckRateAlertsUseCase
import com.ivanarroyo.pesito.domain.usecase.RefreshRatesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Trabajo en segundo plano:
 * 1. Descarga tasas nuevas y las guarda en Room (la pantalla y el widget se actualizan solos).
 * 2. Revisa las alertas y notifica las que se cumplieron (Fase 8).
 *
 * @AssistedInject: Context y WorkerParameters los pone WorkManager; el resto lo pone Hilt.
 */
@HiltWorker
class RefreshRatesWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val refreshRates: RefreshRatesUseCase,
    private val checkRateAlerts: CheckRateAlertsUseCase,
    private val alertNotifier: AlertNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val refreshResult = refreshRates()
        if (refreshResult.isSuccess) notifyTriggeredAlerts()

        return when (SyncPolicy.decide(refreshResult, runAttemptCount)) {
            SyncOutcome.SUCCESS -> Result.success()
            SyncOutcome.RETRY -> Result.retry()
            // En un trabajo periódico, failure() NO lo cancela: vuelve a correr en el próximo turno.
            SyncOutcome.FAILURE -> Result.failure()
        }
    }

    private suspend fun notifyTriggeredAlerts() {
        try {
            val triggered = checkRateAlerts()
            if (triggered.isNotEmpty()) alertNotifier.notify(triggered)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Un problema con las alertas no debe marcar como fallida la descarga, que sí salió bien.
        }
    }
}
