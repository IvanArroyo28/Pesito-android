package com.ivanarroyo.pesito.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** Un punto de la gráfica: la tasa de un día. */
data class RatePoint(
    val date: LocalDate,
    val rate: BigDecimal,
)

enum class HistoryRange(val days: Long) {
    MONTH(30),
    QUARTER(90),
    YEAR(365),
}

enum class RateSource {
    /** Promedio de varios bancos centrales (lo que da Frankfurter por defecto). */
    AVERAGE,

    /** TRM oficial del Banco de la República. Solo existe para dólar ↔ peso. */
    BANREP,
}

/** Historial de un par en un periodo, con sus números clave ya calculados. */
data class RateHistory(val points: List<RatePoint>) {

    init {
        require(points.isNotEmpty()) { "Un historial necesita al menos un punto" }
    }

    val first: RatePoint get() = points.first()
    val last: RatePoint get() = points.last()
    val min: RatePoint get() = points.minBy { it.rate }
    val max: RatePoint get() = points.maxBy { it.rate }

    /** Cuánto subió (+) o bajó (−) la tasa en el periodo, en porcentaje con 2 decimales. */
    val changePercent: BigDecimal
        get() = last.rate.subtract(first.rate)
            .multiply(BigDecimal(100))
            .divide(first.rate, 2, RoundingMode.HALF_EVEN)
}
