package com.ivanarroyo.pesito

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ivanarroyo.pesito.data.sync.RatesSyncScheduler
import com.ivanarroyo.pesito.presentation.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PesitoApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var ratesSyncScheduler: RatesSyncScheduler
    @Inject lateinit var widgetUpdater: WidgetUpdater

    /** WorkManager crea los Workers con la fábrica de Hilt. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate() // Aquí Hilt llena los @Inject de arriba.
        ratesSyncScheduler.schedule()
        widgetUpdater.start()
    }
}
