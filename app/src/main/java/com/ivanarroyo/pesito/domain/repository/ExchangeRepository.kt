package com.ivanarroyo.pesito.domain.repository

import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first: la base de datos del celular es la ÚNICA fuente de verdad.
 * - Los observe… leen lo guardado y vuelven a emitir solos cada vez que cambia.
 * - refresh() descarga de internet y guarda. Si falla, lo guardado sigue ahí.
 *
 * Así, cuando en la Fase 6 WorkManager actualice en segundo plano,
 * la pantalla (y luego el widget) se actualizan sin hacer nada extra.
 */
interface ExchangeRepository {
    fun observeCurrencies(): Flow<List<Currency>>

    /** Null si todavía no hay nada guardado (primera vez que se abre la app). */
    fun observeLatestRates(): Flow<RatesSnapshot?>

    suspend fun refresh(): Result<Unit>
}
