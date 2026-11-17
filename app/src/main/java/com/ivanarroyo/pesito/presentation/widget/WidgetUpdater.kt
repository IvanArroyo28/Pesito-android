package com.ivanarroyo.pesito.presentation.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.ivanarroyo.pesito.domain.usecase.ObserveConverterDataUseCase
import com.ivanarroyo.pesito.domain.usecase.ObserveUserPreferencesUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Mantiene el widget al día SIN que nadie tenga que acordarse de avisarle:
 * observa Room y DataStore, y cada vez que algo cambia redibuja el widget.
 *
 * Funciona aunque la app esté cerrada: cuando WorkManager corre, Android abre
 * el proceso, PesitoApp arranca este observador y el widget se entera.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val observeConverterData: ObserveConverterDataUseCase,
    private val observeUserPreferences: ObserveUserPreferencesUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            combine(observeConverterData(), observeUserPreferences()) { data, prefs -> data to prefs }
                .distinctUntilChanged()
                .collect {
                    try {
                        PesitoWidget().updateAll(context) // Si no hay widgets puestos, no hace nada.
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Un error dibujando el widget nunca debe tumbar la app.
                    }
                }
        }
    }
}
