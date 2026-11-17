package com.ivanarroyo.pesito.domain.repository

import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RatesSnapshot

/**
 * Contrato del dominio. En la Fase 3 cambiaremos la implementación
 * por una que guarde todo en el celular (offline-first) sin tocar esta interfaz.
 */
interface ExchangeRepository {
    suspend fun getCurrencies(): Result<List<Currency>>
    suspend fun getLatestRates(): Result<RatesSnapshot>
}
