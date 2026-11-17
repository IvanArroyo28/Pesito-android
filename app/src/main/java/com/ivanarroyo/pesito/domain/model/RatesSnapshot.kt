package com.ivanarroyo.pesito.domain.model

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

/**
 * Foto de las tasas de un día, todas expresadas contra el dólar:
 * ratesPerUsd["COP"] = 3293.28 significa 1 USD = 3.293,28 COP.
 *
 * Con una sola descarga (base USD) podemos convertir entre CUALQUIER par
 * usando el dólar como puente: COP → EUR = tasa(EUR) / tasa(COP).
 */
data class RatesSnapshot(
    val date: LocalDate,
    val ratesPerUsd: Map<String, BigDecimal>,
) {
    /** Cuántas unidades de [to] vale 1 unidad de [from]. Null si no tenemos alguna de las dos. */
    fun rate(from: String, to: String): BigDecimal? {
        val fromPerUsd = ratesPerUsd[from] ?: return null
        val toPerUsd = ratesPerUsd[to] ?: return null
        if (fromPerUsd.signum() == 0) return null
        return toPerUsd.divide(fromPerUsd, MATH_CONTEXT)
    }

    companion object {
        /** 16 dígitos significativos: precisión de sobra para dinero y sin errores de Double. */
        val MATH_CONTEXT: MathContext = MathContext.DECIMAL64
    }
}
