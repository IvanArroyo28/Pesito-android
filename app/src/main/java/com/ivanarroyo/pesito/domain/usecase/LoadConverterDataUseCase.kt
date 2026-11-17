package com.ivanarroyo.pesito.domain.usecase

import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import com.ivanarroyo.pesito.domain.repository.ExchangeRepository
import javax.inject.Inject

data class ConverterData(
    /** Solo monedas que tienen tasa hoy: populares primero, luego en orden alfabético. */
    val currencies: List<Currency>,
    val snapshot: RatesSnapshot,
)

class LoadConverterDataUseCase @Inject constructor(
    private val repository: ExchangeRepository,
) {
    suspend operator fun invoke(): Result<ConverterData> {
        val currencies = repository.getCurrencies().getOrElse { return Result.failure(it) }
        val snapshot = repository.getLatestRates().getOrElse { return Result.failure(it) }

        val withRate = currencies.filter { it.code in snapshot.ratesPerUsd }
        val sorted = withRate.sortedWith(
            compareBy<Currency> { POPULAR.indexOf(it.code).let { i -> if (i == -1) Int.MAX_VALUE else i } }
                .thenBy { it.name }
        )
        return Result.success(ConverterData(sorted, snapshot))
    }

    companion object {
        /** Las monedas que más usa alguien en Colombia, en este orden. */
        val POPULAR = listOf("COP", "USD", "EUR", "MXN", "BRL", "ARS", "CLP", "PEN", "GBP", "CAD")
    }
}
