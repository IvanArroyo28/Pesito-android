package com.ivanarroyo.pesito.presentation.converter

import com.ivanarroyo.pesito.domain.model.Currency
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

sealed interface ConverterUiState {

    /** Primera vez: no hay nada guardado y estamos descargando. */
    data object Loading : ConverterUiState

    /** No hay nada guardado Y falló la descarga. [isNetwork] = problema de conexión. */
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
        /** Cuándo se descargaron por última vez (por la app o por WorkManager). */
        val updatedAt: Instant? = null,
        /** Se está descargando en segundo plano (muestra el indicador de "jalar para actualizar"). */
        val isRefreshing: Boolean = false,
        /** La última descarga falló: mostramos las tasas guardadas con un aviso. */
        val isShowingSavedRates: Boolean = false,
        /** Para pintar las estrellas en el selector. */
        val favoriteCodes: Set<String> = emptySet(),
        /** El monto convertido a cada favorita (sin incluir la moneda de origen). */
        val favorites: List<FavoriteConversion> = emptyList(),
    ) : ConverterUiState
}

data class FavoriteConversion(
    val currency: Currency,
    /** Null cuando el campo de monto está vacío. */
    val amount: BigDecimal?,
)
