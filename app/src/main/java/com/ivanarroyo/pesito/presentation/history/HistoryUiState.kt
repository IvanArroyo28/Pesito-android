package com.ivanarroyo.pesito.presentation.history

import com.ivanarroyo.pesito.domain.model.HistoryRange
import com.ivanarroyo.pesito.domain.model.RateHistory
import com.ivanarroyo.pesito.domain.model.RateSource

data class HistoryUiState(
    val from: String,
    val to: String,
    val range: HistoryRange = HistoryRange.MONTH,
    val source: RateSource = RateSource.AVERAGE,
    /** El botón "TRM BanRep" solo se habilita para USD ↔ COP. */
    val isBanRepAvailable: Boolean = false,
    val content: HistoryContent = HistoryContent.Loading,
)

sealed interface HistoryContent {
    data object Loading : HistoryContent
    data class Error(val isNetwork: Boolean) : HistoryContent
    data object Empty : HistoryContent
    data class Loaded(val history: RateHistory) : HistoryContent
}
