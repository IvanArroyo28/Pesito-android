package com.ivanarroyo.pesito.data.repository

import com.ivanarroyo.pesito.data.mapper.inverted
import com.ivanarroyo.pesito.data.mapper.toPoints
import com.ivanarroyo.pesito.data.remote.FrankfurterApi
import com.ivanarroyo.pesito.domain.model.RatePoint
import com.ivanarroyo.pesito.domain.model.RateSource
import com.ivanarroyo.pesito.domain.repository.HistoryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Guarda en memoria lo que ya descargó: si el usuario cambia de "1 mes" a "1 año"
 * y vuelve a "1 mes", no gastamos otra petición.
 */
@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val api: FrankfurterApi,
) : HistoryRepository {

    private data class CacheKey(
        val from: String,
        val to: String,
        val start: LocalDate,
        val end: LocalDate,
        val source: RateSource,
    )

    private val mutex = Mutex()
    private val cache = mutableMapOf<CacheKey, List<RatePoint>>()

    override suspend fun getHistory(
        from: String,
        to: String,
        start: LocalDate,
        end: LocalDate,
        source: RateSource,
    ): Result<List<RatePoint>> {
        val key = CacheKey(from, to, start, end, source)
        mutex.withLock { cache[key] }?.let { return Result.success(it) }

        return try {
            val points = when (source) {
                RateSource.AVERAGE -> api.getRateHistory(
                    base = from.lowercase(),
                    quotes = to.lowercase(),
                    from = start.toString(),
                    to = end.toString(),
                ).toPoints()

                // El BanRep solo publica USD→COP. Para COP→USD pedimos lo mismo y lo invertimos.
                RateSource.BANREP -> {
                    val usdToCop = api.getRateHistory(
                        base = "usd",
                        quotes = "cop",
                        from = start.toString(),
                        to = end.toString(),
                        providers = BANREP,
                    ).toPoints()
                    if (from == "USD") usdToCop else usdToCop.inverted()
                }
            }
            mutex.withLock { cache[key] = points }
            Result.success(points)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        const val BANREP = "BANREP"
    }
}
