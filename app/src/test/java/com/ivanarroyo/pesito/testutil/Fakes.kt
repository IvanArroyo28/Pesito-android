package com.ivanarroyo.pesito.testutil

import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import com.ivanarroyo.pesito.domain.model.UserPreferences
import com.ivanarroyo.pesito.domain.repository.ExchangeRepository
import com.ivanarroyo.pesito.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** "Room" en memoria. [refreshResult] decide si la descarga sale bien o falla. */
class FakeExchangeRepository(
    currencies: List<Currency> = emptyList(),
    snapshot: RatesSnapshot? = null,
    var refreshResult: Result<Unit> = Result.success(Unit),
) : ExchangeRepository {
    val currencies = MutableStateFlow(currencies)
    val snapshot = MutableStateFlow(snapshot)

    override fun observeCurrencies(): Flow<List<Currency>> = currencies
    override fun observeLatestRates(): Flow<RatesSnapshot?> = snapshot
    override suspend fun refresh(): Result<Unit> = refreshResult
}

/** "DataStore" en memoria. */
class FakePreferencesRepository(initial: UserPreferences = UserPreferences()) : PreferencesRepository {
    private val state = MutableStateFlow(initial)
    val current: UserPreferences get() = state.value

    override val preferences: Flow<UserPreferences> = state

    override suspend fun toggleFavorite(code: String) {
        state.update { it.copy(favoriteCodes = UserPreferences.toggle(it.favoriteCodes, code)) }
    }

    override suspend fun saveLastPair(from: String, to: String) {
        state.update { it.copy(lastFrom = from, lastTo = to) }
    }
}
