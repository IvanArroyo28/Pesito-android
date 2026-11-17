package com.ivanarroyo.pesito.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ivanarroyo.pesito.domain.usecase.RefreshRatesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Trabajo en segundo plano: descarga tasas nuevas y las guarda en Room.
 * Como la pantalla observa Room (Fase 3), si la app está abierta se actualiza sola.
 *
 * @AssistedInject: Context y WorkerParameters los pone WorkManager;
 * el caso de uso lo pone Hilt.
 */
@HiltWorker
class RefreshRatesWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val refreshRates: RefreshRatesUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        when (SyncPolicy.decide(refreshRates(), runAttemptCount)) {
            SyncOutcome.SUCCESS -> Result.success()
            SyncOutcome.RETRY -> Result.retry()
            // En un trabajo periódico, failure() NO lo cancela: vuelve a correr en el próximo turno.
            SyncOutcome.FAILURE -> Result.failure()
        }
}
