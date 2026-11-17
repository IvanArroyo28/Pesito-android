package com.ivanarroyo.pesito.domain.usecase

import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import com.ivanarroyo.pesito.domain.repository.ExchangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class ConverterData(
    /** Solo monedas que tienen tasa: populares primero, luego en orden alfabético. */
    val currencies: List<Currency>,
    val snapshot: RatesSnapshot,
)

/** Une monedas + tasas guardadas. Emite null mientras no haya datos en el celular. */
class ObserveConverterDataUseCase @Inject constructor(
    private val repository: ExchangeRepository,
) {
    operator fun invoke(): Flow<ConverterData?> =
        combine(repository.observeCurrencies(), repository.observeLatestRates()) { currencies, snapshot ->
            if (snapshot == null) return@combine null
            val withRate = currencies.filter { it.code in snapshot.ratesPerUsd }
            if (withRate.isEmpty()) return@combine null
            ConverterData(withRate.sortedWith(popularFirst), snapshot)
        }

    companion object {
        /** Las monedas que más usa alguien en Colombia, en este orden. */
        val POPULAR = listOf("COP", "USD", "EUR", "MXN", "BRL", "ARS", "CLP", "PEN", "GBP", "CAD")

        private val popularFirst = compareBy<Currency> {
            POPULAR.indexOf(it.code).let { i -> if (i == -1) Int.MAX_VALUE else i }
        }.thenBy { it.name }
    }
}
