package com.ivanarroyo.pesito.presentation.alerts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ivanarroyo.pesito.domain.model.AlertDirection
import com.ivanarroyo.pesito.domain.usecase.ConvertCurrencyUseCase
import com.ivanarroyo.pesito.domain.usecase.CreateAlertUseCase
import com.ivanarroyo.pesito.domain.usecase.DeleteAlertUseCase
import com.ivanarroyo.pesito.domain.usecase.ObserveAlertsUseCase
import com.ivanarroyo.pesito.domain.usecase.ObserveConverterDataUseCase
import com.ivanarroyo.pesito.domain.usecase.SetAlertActiveUseCase
import com.ivanarroyo.pesito.presentation.navigation.AlertsDestination
import com.ivanarroyo.pesito.presentation.util.AmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeAlerts: ObserveAlertsUseCase,
    observeConverterData: ObserveConverterDataUseCase,
    private val convertCurrency: ConvertCurrencyUseCase,
    private val createAlert: CreateAlertUseCase,
    private val deleteAlert: DeleteAlertUseCase,
    private val setAlertActive: SetAlertActiveUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AlertsDestination>()

    val uiState: StateFlow<AlertsUiState> =
        combine(observeAlerts(), observeConverterData()) { alerts, data ->
            fun rateOf(from: String, to: String) =
                data?.let { convertCurrency(it.snapshot, BigDecimal.ONE, from, to) }

            AlertsUiState(
                from = route.from,
                to = route.to,
                currentRate = rateOf(route.from, route.to),
                alerts = alerts.map { AlertItem(it, rateOf(it.from, it.to)) },
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AlertsUiState(from = route.from, to = route.to),
        )

    /** Diálogo de nueva alerta. Null = cerrado. Estado de Compose, igual que el monto del conversor. */
    var draft by mutableStateOf<AlertDraft?>(null)
        private set

    fun onNewAlert() {
        draft = AlertDraft()
    }

    fun onDraftDirectionChange(direction: AlertDirection) {
        draft = draft?.copy(direction = direction)
    }

    fun onDraftTargetChange(input: String) {
        val clean = AmountInput.sanitize(input, maxDecimals = TARGET_MAX_DECIMALS) ?: return
        draft = draft?.copy(targetText = clean)
    }

    fun onDismissDraft() {
        draft = null
    }

    fun onSaveDraft() {
        val current = draft ?: return
        val target = current.target ?: return
        viewModelScope.launch {
            createAlert(route.from, route.to, current.direction, target)
                .onSuccess { draft = null }
        }
    }

    fun onToggleAlert(id: Long, active: Boolean) {
        viewModelScope.launch { setAlertActive(id, active) }
    }

    fun onDeleteAlert(id: Long) {
        viewModelScope.launch { deleteAlert(id) }
    }

    private companion object {
        const val TARGET_MAX_DECIMALS = 6
    }
}
