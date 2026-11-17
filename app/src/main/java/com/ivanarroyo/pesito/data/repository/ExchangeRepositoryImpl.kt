package com.ivanarroyo.pesito.data.repository

import com.ivanarroyo.pesito.data.mapper.toDomainOrNull
import com.ivanarroyo.pesito.data.mapper.toSnapshot
import com.ivanarroyo.pesito.data.remote.FrankfurterApi
import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import com.ivanarroyo.pesito.domain.repository.ExchangeRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Versión "solo red" con caché en memoria.
 * En la Fase 3 la reemplazamos por una que guarda en Room y funciona sin internet.
 */
class ExchangeRepositoryImpl @Inject constructor(
    private val api: FrankfurterApi,
) : ExchangeRepository {

    private val mutex = Mutex()
    private var currencies: List<Currency>? = null
    private var snapshot: RatesSnapshot? = null

    override suspend fun getCurrencies(): Result<List<Currency>> = safeCall {
        mutex.withLock {
            currencies ?: api.getCurrencies().mapNotNull { it.toDomainOrNull() }.also { currencies = it }
        }
    }

    override suspend fun getLatestRates(): Result<RatesSnapshot> = safeCall {
        mutex.withLock {
            snapshot ?: api.getLatestRates().toSnapshot().also { snapshot = it }
        }
    }

    private inline fun <T> safeCall(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
