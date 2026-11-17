package com.ivanarroyo.pesito.presentation.converter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivanarroyo.pesito.domain.usecase.ConvertCurrencyUseCase
import com.ivanarroyo.pesito.domain.usecase.ConverterData
import com.ivanarroyo.pesito.domain.usecase.ObserveConverterDataUseCase
import com.ivanarroyo.pesito.domain.usecase.RefreshRatesUseCase
import com.ivanarroyo.pesito.presentation.util.AmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ConverterViewModel @Inject constructor(
    observeConverterData: ObserveConverterDataUseCase,
    private val refreshRates: RefreshRatesUseCase,
    private val convertCurrency: ConvertCurrencyUseCase,
) : ViewModel() {

    /**
     * El texto del campo vive en estado de Compose (no en un Flow) para que el cursor
     * no salte mientras el usuario escribe. El número ya parseado sí va al Flow.
     */
    var amountText by mutableStateOf(DEFAULT_AMOUNT)
        private set

    private val amount = MutableStateFlow(AmountInput.parse(DEFAULT_AMOUNT))
    private val fromCode = MutableStateFlow(DEFAULT_FROM)
    private val toCode = MutableStateFlow(DEFAULT_TO)
    private val refreshState = MutableStateFlow(RefreshState(isRefreshing = true))
    private var refreshJob: Job? = null

    val uiState: StateFlow<ConverterUiState> =
        combine(
            observeConverterData(), // lo guardado en Room
            refreshState,           // cómo va la descarga
            fromCode,
            toCode,
            amount,
        ) { data, refresh, from, to, amount ->
            when {
                // Hay datos guardados: SIEMPRE los mostramos, haya internet o no.
                data != null -> buildReady(data, refresh, from, to, amount)
                refresh.failure != null -> ConverterUiState.Error(refresh.failure == Failure.NETWORK)
                else -> ConverterUiState.Loading
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConverterUiState.Loading,
        )

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return // ya hay una descarga en curso
        refreshJob = viewModelScope.launch {
            refreshState.update { it.copy(isRefreshing = true) }
            val failure = refreshRates().exceptionOrNull()?.let {
                if (it is IOException) Failure.NETWORK else Failure.OTHER
            }
            refreshState.value = RefreshState(isRefreshing = false, failure = failure)
        }
    }

    fun onAmountChange(input: String) {
        val clean = AmountInput.sanitize(input) ?: return
        amountText = clean
        amount.value = AmountInput.parse(clean)
    }

    fun onFromSelected(code: String) {
        if (code == toCode.value) onSwap() else fromCode.value = code
    }

    fun onToSelected(code: String) {
        if (code == fromCode.value) onSwap() else toCode.value = code
    }

    fun onSwap() {
        val oldFrom = fromCode.value
        fromCode.value = toCode.value
        toCode.value = oldFrom
    }

    private fun buildReady(
        data: ConverterData,
        refresh: RefreshState,
        fromCode: String,
        toCode: String,
        amount: BigDecimal?,
    ): ConverterUiState {
        val currencies = data.currencies
        val from = currencies.firstOrNull { it.code == fromCode } ?: currencies.first()
        val to = currencies.firstOrNull { it.code == toCode } ?: currencies.first()
        val unitRate = convertCurrency(data.snapshot, BigDecimal.ONE, from.code, to.code)
            ?: return ConverterUiState.Error(isNetwork = false)

        return ConverterUiState.Ready(
            currencies = currencies,
            from = from,
            to = to,
            result = amount?.let { convertCurrency(data.snapshot, it, from.code, to.code) },
            unitRate = unitRate,
            rateDate = data.snapshot.date,
            isRefreshing = refresh.isRefreshing,
            isShowingSavedRates = refresh.failure != null,
        )
    }

    private enum class Failure { NETWORK, OTHER }

    private data class RefreshState(
        val isRefreshing: Boolean = false,
        val failure: Failure? = null,
    )

    companion object {
        const val DEFAULT_AMOUNT = "1"
        const val DEFAULT_FROM = "USD"
        const val DEFAULT_TO = "COP"
    }
}
