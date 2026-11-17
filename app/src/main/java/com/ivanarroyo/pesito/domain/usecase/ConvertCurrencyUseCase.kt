package com.ivanarroyo.pesito.domain.usecase

import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import java.math.BigDecimal
import javax.inject.Inject

/** Función pura: convierte un monto entre dos monedas. Sin red ni estado, fácil de testear. */
class ConvertCurrencyUseCase @Inject constructor() {

    operator fun invoke(
        snapshot: RatesSnapshot,
        amount: BigDecimal,
        from: String,
        to: String,
    ): BigDecimal? {
        if (from == to) return amount
        val rate = snapshot.rate(from, to) ?: return null
        return amount.multiply(rate, RatesSnapshot.MATH_CONTEXT)
    }
}
