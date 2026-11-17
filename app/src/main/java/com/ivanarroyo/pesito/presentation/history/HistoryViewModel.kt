package com.ivanarroyo.pesito.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ivanarroyo.pesito.domain.model.HistoryRange
import com.ivanarroyo.pesito.domain.model.RateSource
import com.ivanarroyo.pesito.domain.usecase.GetRateHistoryUseCase
import com.ivanarroyo.pesito.presentation.navigation.HistoryDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRateHistory: GetRateHistoryUseCase,
) : ViewModel() {

    /** Los argumentos de la ruta (from y to) llegan aquí gracias a Navigation + Hilt. */
    private val route = savedStateHandle.toRoute<HistoryDestination>()
    private val isBanRepAvailable = GetRateHistoryUseCase.isBanRepAvailable(route.from, route.to)

    private val range = MutableStateFlow(HistoryRange.MONTH)
    private val source = MutableStateFlow(RateSource.AVERAGE)
    private val retryCount = MutableStateFlow(0)

    private val baseState = HistoryUiState(
        from = route.from,
        to = route.to,
        isBanRepAvailable = isBanRepAvailable,
    )

    /**
     * transformLatest: si el usuario toca "1 año" mientras aún carga "1 mes",
     * se cancela la petición vieja y solo cuenta la última.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> =
        combine(range, source, retryCount) { range, source, _ -> range to source }
            .transformLatest { (range, source) ->
                val state = baseState.copy(range = range, source = source)
                emit(state.copy(content = HistoryContent.Loading))

                val content: HistoryContent = getRateHistory(route.from, route.to, range, source).fold(
                    onSuccess = { history -> history?.let { HistoryContent.Loaded(it) } ?: HistoryContent.Empty },
                    onFailure = { HistoryContent.Error(isNetwork = it is IOException) },
                )
                emit(state.copy(content = content))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = baseState,
            )

    fun onRangeSelected(newRange: HistoryRange) {
        range.value = newRange
    }

    fun onSourceSelected(newSource: RateSource) {
        if (newSource == RateSource.BANREP && !isBanRepAvailable) return
        source.value = newSource
    }

    fun retry() {
        retryCount.update { it + 1 }
    }
}
