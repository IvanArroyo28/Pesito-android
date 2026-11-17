package com.ivanarroyo.pesito.domain.model

import java.math.BigDecimal

enum class AlertDirection {
    /** Avísame cuando la tasa SUBA hasta [RateAlert.target] o más. */
    ABOVE,

    /** Avísame cuando la tasa BAJE hasta [RateAlert.target] o menos. */
    BELOW,
}

/** "Avísame cuando 1 USD baje de 3.200 COP". */
data class RateAlert(
    val id: Long = 0,
    val from: String,
    val to: String,
    val direction: AlertDirection,
    val target: BigDecimal,
    /** Al cumplirse se desactiva sola, para no avisar lo mismo cada 12 horas. */
    val isActive: Boolean = true,
) {
    fun isTriggeredBy(currentRate: BigDecimal): Boolean = when (direction) {
        AlertDirection.ABOVE -> currentRate >= target
        AlertDirection.BELOW -> currentRate <= target
    }
}

/** Una alerta que se acaba de cumplir, con la tasa que la hizo saltar. */
data class TriggeredAlert(
    val alert: RateAlert,
    val currentRate: BigDecimal,
)
