package com.ivanarroyo.pesito.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RatesSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Programa la descarga cada 12 horas, solo con internet.
     * KEEP: si ya estaba programada no la reinicia (si no, cada vez que abres la app
     * el contador volvería a cero y nunca llegaría a correr).
     */
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<RefreshRatesWorker>(REPEAT_HOURS, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            // Si falla: reintenta a los 15 min, luego 30, luego 60…
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "refresh_rates"
        private const val REPEAT_HOURS = 12L
        private const val BACKOFF_MINUTES = 15L
    }
}
