package com.ivanarroyo.pesito.presentation.alerts

import com.ivanarroyo.pesito.domain.model.AlertDirection
import com.ivanarroyo.pesito.domain.model.RateAlert
import com.ivanarroyo.pesito.presentation.util.AmountInput
import java.math.BigDecimal

data class AlertsUiState(
    /** Par con el que se crean las alertas nuevas (el que tenías en el conversor). */
    val from: String,
    val to: String,
    /** 1 [from] en [to] hoy. Null si aún no hay tasas guardadas. */
    val currentRate: BigDecimal? = null,
    val alerts: List<AlertItem> = emptyList(),
    val isLoading: Boolean = true,
)

data class AlertItem(
    val alert: RateAlert,
    /** Tasa de hoy para el par de ESTA alerta (puede ser distinto al de arriba). */
    val currentRate: BigDecimal?,
)

/** Lo que se está escribiendo en el diálogo de "Nueva alerta". */
data class AlertDraft(
    val direction: AlertDirection = AlertDirection.BELOW,
    val targetText: String = "",
) {
    val target: BigDecimal? get() = AmountInput.parse(targetText)
    val isValid: Boolean get() = (target?.signum() ?: 0) > 0
}
