package com.ivanarroyo.pesito.data.repository

import com.ivanarroyo.pesito.data.local.ExchangeDao
import com.ivanarroyo.pesito.data.mapper.toDomain
import com.ivanarroyo.pesito.data.mapper.toEntities
import com.ivanarroyo.pesito.data.mapper.toEntityOrNull
import com.ivanarroyo.pesito.data.mapper.toSnapshotOrNull
import com.ivanarroyo.pesito.data.remote.FrankfurterApi
import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import com.ivanarroyo.pesito.domain.repository.ExchangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * La pantalla SOLO lee de Room. La red solo sirve para llenar Room.
 * Si no hay internet, refresh() falla pero la app sigue mostrando lo último guardado.
 */
class ExchangeRepositoryImpl @Inject constructor(
    private val api: FrankfurterApi,
    private val dao: ExchangeDao,
    private val clock: Clock,
) : ExchangeRepository {

    override fun observeCurrencies(): Flow<List<Currency>> =
        dao.observeCurrencies().map { rows -> rows.map { it.toDomain() } }

    override fun observeLatestRates(): Flow<RatesSnapshot?> =
        dao.observeRates().map { it.toSnapshotOrNull() }

    override suspend fun refresh(): Result<Unit> =
        try {
            val currencies = api.getCurrencies().mapNotNull { it.toEntityOrNull() }
            val rates = api.getLatestRates().toEntities(fetchedAt = clock.millis())
            // Si la API respondiera vacío, no borramos lo que ya teníamos.
            if (currencies.isNotEmpty() && rates.isNotEmpty()) {
                dao.replaceAll(currencies, rates)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
