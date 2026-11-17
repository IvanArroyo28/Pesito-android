package com.ivanarroyo.pesito.presentation.converter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivanarroyo.pesito.domain.model.UserPreferences
import com.ivanarroyo.pesito.domain.usecase.ConvertCurrencyUseCase
import com.ivanarroyo.pesito.domain.usecase.ConverterData
import com.ivanarroyo.pesito.domain.usecase.ObserveConverterDataUseCase
import com.ivanarroyo.pesito.domain.usecase.ObserveUserPreferencesUseCase
import com.ivanarroyo.pesito.domain.usecase.RefreshRatesUseCase
import com.ivanarroyo.pesito.domain.usecase.SaveLastPairUseCase
import com.ivanarroyo.pesito.domain.usecase.ToggleFavoriteUseCase
import com.ivanarroyo.pesito.presentation.util.AmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ConverterViewModel @Inject constructor(
    observeConverterData: ObserveConverterDataUseCase,
    private val observeUserPreferences: ObserveUserPreferencesUseCase,
    private val refreshRates: RefreshRatesUseCase,
    private val convertCurrency: ConvertCurrencyUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val saveLastPair: SaveLastPairUseCase,
) : ViewModel() {

    /**
     * El texto del campo vive en estado de Compose (no en un Flow) para que el cursor
     * no salte mientras el usuario escribe. El número ya parseado sí va al Flow.
     */
    var amountText by mutableStateOf(DEFAULT_AMOUNT)
        private set

    private val amount = MutableStateFlow(AmountInput.parse(DEFAULT_AMOUNT))

    /** El par elegido. Null hasta leer de DataStore el último que usó la persona. */
    private val selection = MutableStateFlow<Selection?>(null)

    private val refreshState = MutableStateFlow(RefreshState(isRefreshing = true))
    private var refreshJob: Job? = null

    val uiState: StateFlow<ConverterUiState> =
        combine(
            observeConverterData(),   // tasas guardadas en Room
            observeUserPreferences(), // favoritas guardadas en DataStore
            refreshState,             // cómo va la descarga
            selection,
            amount,
        ) { data, prefs, refresh, selection, amount ->
            when {
                selection == null -> ConverterUiState.Loading
                // Hay datos guardados: SIEMPRE los mostramos, haya internet o no.
                data != null -> buildReady(data, prefs, refresh, selection, amount)
                refresh.failure != null -> ConverterUiState.Error(refresh.failure == Failure.NETWORK)
                else -> ConverterUiState.Loading
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConverterUiState.Loading,
        )

    init {
        viewModelScope.launch {
            // Abrimos la app en el último par que usó la persona.
            val prefs = observeUserPreferences().first()
            selection.value = Selection(prefs.lastFrom, prefs.lastTo)
        }
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
        val current = selection.value ?: return
        if (code == current.to) onSwap() else updateSelection(current.copy(from = code))
    }

    fun onToSelected(code: String) {
        val current = selection.value ?: return
        if (code == current.from) onSwap() else updateSelection(current.copy(to = code))
    }

    fun onSwap() {
        val current = selection.value ?: return
        updateSelection(Selection(from = current.to, to = current.from))
    }

    fun onToggleFavorite(code: String) {
        viewModelScope.launch { toggleFavorite(code) }
    }

    private fun updateSelection(new: Selection) {
        selection.value = new
        viewModelScope.launch { saveLastPair(new.from, new.to) }
    }

    private fun buildReady(
        data: ConverterData,
        prefs: UserPreferences,
        refresh: RefreshState,
        selection: Selection,
        amount: BigDecimal?,
    ): ConverterUiState {
        val currencies = data.currencies
        val byCode = currencies.associateBy { it.code }
        val from = byCode[selection.from] ?: currencies.first()
        val to = byCode[selection.to] ?: currencies.first()
        val unitRate = convertCurrency(data.snapshot, BigDecimal.ONE, from.code, to.code)
            ?: return ConverterUiState.Error(isNetwork = false)

        val favorites = prefs.favoriteCodes
            .filter { it != from.code }      // no tiene sentido convertir USD → USD
            .mapNotNull { byCode[it] }       // ignora favoritas sin tasa hoy
            .map { currency ->
                FavoriteConversion(
                    currency = currency,
                    amount = amount?.let { convertCurrency(data.snapshot, it, from.code, currency.code) },
                )
            }

        return ConverterUiState.Ready(
            currencies = currencies,
            from = from,
            to = to,
            result = amount?.let { convertCurrency(data.snapshot, it, from.code, to.code) },
            unitRate = unitRate,
            rateDate = data.snapshot.date,
            updatedAt = data.snapshot.updatedAt,
            isRefreshing = refresh.isRefreshing,
            isShowingSavedRates = refresh.failure != null,
            favoriteCodes = prefs.favoriteCodes.toSet(),
            favorites = favorites,
        )
    }

    private data class Selection(val from: String, val to: String)

    private enum class Failure { NETWORK, OTHER }

    private data class RefreshState(
        val isRefreshing: Boolean = false,
        val failure: Failure? = null,
    )

    companion object {
        const val DEFAULT_AMOUNT = "1"
    }
}
