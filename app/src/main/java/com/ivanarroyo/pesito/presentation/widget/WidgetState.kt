package com.ivanarroyo.pesito.presentation.widget

import com.ivanarroyo.pesito.domain.model.UserPreferences
import com.ivanarroyo.pesito.domain.usecase.ConvertCurrencyUseCase
import com.ivanarroyo.pesito.domain.usecase.ConverterData
import java.math.BigDecimal
import java.time.LocalDate

/** Lo que muestra el widget. Se calcula aparte para poder probarlo sin Android. */
sealed interface WidgetState {

    /** Todavía no hay tasas guardadas (nunca se abrió la app con internet). */
    data object NoData : WidgetState

    data class Ready(
        /** Par principal: el último que usó la persona en la app. */
        val from: String,
        val to: String,
        val unitRate: BigDecimal,
        /** "1 EUR = 3.829,40 COP", "1 MXN = …" — una fila por favorita. */
        val rows: List<WidgetRow>,
        val rateDate: LocalDate,
    ) : WidgetState
}

data class WidgetRow(
    val code: String,
    /** Cuánto vale 1 unidad de [code] en la moneda destino del par principal. */
    val valueInTarget: BigDecimal,
)

object WidgetStateBuilder {

    fun build(
        data: ConverterData?,
        prefs: UserPreferences,
        convert: ConvertCurrencyUseCase,
    ): WidgetState {
        if (data == null) return WidgetState.NoData
        val snapshot = data.snapshot
        val available = snapshot.ratesPerUsd.keys

        val from = prefs.lastFrom.takeIf { it in available } ?: UserPreferences.DEFAULT_FROM
        val to = prefs.lastTo.takeIf { it in available } ?: UserPreferences.DEFAULT_TO
        val unitRate = convert(snapshot, BigDecimal.ONE, from, to) ?: return WidgetState.NoData

        val rows = prefs.favoriteCodes
            .filter { it != from && it != to && it in available }
            .mapNotNull { code ->
                convert(snapshot, BigDecimal.ONE, code, to)?.let { WidgetRow(code, it) }
            }

        return WidgetState.Ready(from, to, unitRate, rows, snapshot.date)
    }
}
