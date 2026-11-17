package com.ivanarroyo.pesito.presentation.converter

import com.ivanarroyo.pesito.domain.model.Currency
import java.math.BigDecimal
import java.time.LocalDate

sealed interface ConverterUiState {

    data object Loading : ConverterUiState

    /** [isNetwork] = true si el problema es la conexión (mensaje distinto). */
    data class Error(val isNetwork: Boolean) : ConverterUiState

    data class Ready(
        val currencies: List<Currency>,
        val from: Currency,
        val to: Currency,
        /** Null cuando el campo de monto está vacío. */
        val result: BigDecimal?,
        /** Cuánto vale 1 [from] en [to]. */
        val unitRate: BigDecimal,
        val rateDate: LocalDate,
    ) : ConverterUiState
}
