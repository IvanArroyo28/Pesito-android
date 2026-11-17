package com.ivanarroyo.pesito.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RatesSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val needsInternet = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Programa la descarga cada 12 horas, solo con internet.
     * KEEP: si ya estaba programada no la reinicia (si no, cada vez que abres la app
     * el contador volvería a cero y nunca llegaría a correr).
     */
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<RefreshRatesWorker>(REPEAT_HOURS, TimeUnit.HOURS)
            .setConstraints(needsInternet)
            // Si falla: reintenta a los 15 min, luego 30, luego 60…
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Descarga de una vez (botón del widget). Si tocan varias veces, solo cuenta la última. */
    fun refreshNow() {
        val request = OneTimeWorkRequestBuilder<RefreshRatesWorker>()
            .setConstraints(needsInternet)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            REFRESH_NOW_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "refresh_rates"
        const val REFRESH_NOW_WORK_NAME = "refresh_rates_now"
        private const val REPEAT_HOURS = 12L
        private const val BACKOFF_MINUTES = 15L
    }
}
