package com.ivanarroyo.pesito.presentation.converter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivanarroyo.pesito.domain.usecase.ConvertCurrencyUseCase
import com.ivanarroyo.pesito.domain.usecase.ConverterData
import com.ivanarroyo.pesito.domain.usecase.LoadConverterDataUseCase
import com.ivanarroyo.pesito.presentation.util.AmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val loadConverterData: LoadConverterDataUseCase,
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
    private val loadState = MutableStateFlow<LoadState>(LoadState.Loading)

    val uiState: StateFlow<ConverterUiState> =
        combine(loadState, fromCode, toCode, amount) { load, from, to, amount ->
            when (load) {
                LoadState.Loading -> ConverterUiState.Loading
                is LoadState.Failed -> ConverterUiState.Error(load.isNetwork)
                is LoadState.Loaded -> buildReady(load.data, from, to, amount)
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
        viewModelScope.launch {
            loadState.value = LoadState.Loading
            loadState.value = loadConverterData().fold(
                onSuccess = { LoadState.Loaded(it) },
                onFailure = { LoadState.Failed(isNetwork = it is IOException) },
            )
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
        )
    }

    private sealed interface LoadState {
        data object Loading : LoadState
        data class Failed(val isNetwork: Boolean) : LoadState
        data class Loaded(val data: ConverterData) : LoadState
    }

    companion object {
        const val DEFAULT_AMOUNT = "1"
        const val DEFAULT_FROM = "USD"
        const val DEFAULT_TO = "COP"
    }
}
