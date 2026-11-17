package com.ivanarroyo.pesito.presentation.converter

import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import com.ivanarroyo.pesito.domain.model.UserPreferences
import com.ivanarroyo.pesito.domain.usecase.ConvertCurrencyUseCase
import com.ivanarroyo.pesito.domain.usecase.ObserveConverterDataUseCase
import com.ivanarroyo.pesito.domain.usecase.ObserveUserPreferencesUseCase
import com.ivanarroyo.pesito.domain.usecase.RefreshRatesUseCase
import com.ivanarroyo.pesito.domain.usecase.SaveLastPairUseCase
import com.ivanarroyo.pesito.domain.usecase.ToggleFavoriteUseCase
import com.ivanarroyo.pesito.testutil.FakeExchangeRepository
import com.ivanarroyo.pesito.testutil.FakePreferencesRepository
import com.ivanarroyo.pesito.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ConverterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val currencies = listOf(
        Currency("COP", "Peso colombiano", "$"),
        Currency("USD", "Dólar estadounidense", "US$"),
        Currency("EUR", "Euro", "€"),
        Currency("MXN", "Peso mexicano", "MX$"),
    )

    private val snapshot = RatesSnapshot(
        date = LocalDate.of(2026, 11, 13),
        ratesPerUsd = mapOf(
            "USD" to BigDecimal.ONE,
            "COP" to BigDecimal("3300"),
            "EUR" to BigDecimal("0.825"), // 1 EUR = 3300 / 0.825 = 4000 COP
            "MXN" to BigDecimal("18"),
        ),
    )

    private fun createViewModel(
        exchange: FakeExchangeRepository = FakeExchangeRepository(currencies, snapshot),
        prefs: FakePreferencesRepository = FakePreferencesRepository(),
    ) = ConverterViewModel(
        observeConverterData = ObserveConverterDataUseCase(exchange),
        observeUserPreferences = ObserveUserPreferencesUseCase(prefs),
        refreshRates = RefreshRatesUseCase(exchange),
        convertCurrency = ConvertCurrencyUseCase(),
        toggleFavorite = ToggleFavoriteUseCase(prefs),
        saveLastPair = SaveLastPairUseCase(prefs),
    )

    /** uiState solo calcula mientras alguien lo observa (WhileSubscribed): simulamos la pantalla. */
    private fun TestScope.observe(viewModel: ConverterViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun ConverterViewModel.ready() = uiState.value as ConverterUiState.Ready

    @Test
    fun `opens on the last pair used and converts`() = runTest {
        val prefs = FakePreferencesRepository(UserPreferences(lastFrom = "EUR", lastTo = "COP"))
        val viewModel = createViewModel(prefs = prefs)
        observe(viewModel)
        advanceUntilIdle()

        val state = viewModel.ready()
        assertEquals("EUR", state.from.code)
        assertEquals("COP", state.to.code)
        assertEquals(0, BigDecimal("4000").compareTo(state.result))
    }

    @Test
    fun `typing an amount updates the result and invalid input is ignored`() = runTest {
        val viewModel = createViewModel(prefs = FakePreferencesRepository(UserPreferences(lastFrom = "EUR", lastTo = "COP")))
        observe(viewModel)

        viewModel.onAmountChange("2.5")
        viewModel.onAmountChange("2,5x") // letra: se ignora
        advanceUntilIdle()

        assertEquals("2,5", viewModel.amountText)
        assertEquals(0, BigDecimal("10000").compareTo(viewModel.ready().result))
    }

    @Test
    fun `swap exchanges the currencies and remembers the pair`() = runTest {
        val prefs = FakePreferencesRepository(UserPreferences(lastFrom = "USD", lastTo = "COP"))
        val viewModel = createViewModel(prefs = prefs)
        observe(viewModel)

        viewModel.onSwap()
        advanceUntilIdle()

        assertEquals("COP", viewModel.ready().from.code)
        assertEquals("USD", viewModel.ready().to.code)
        assertEquals("COP", prefs.current.lastFrom)
        assertEquals("USD", prefs.current.lastTo)
    }

    @Test
    fun `choosing the same currency on both sides swaps them`() = runTest {
        val viewModel = createViewModel(prefs = FakePreferencesRepository(UserPreferences(lastFrom = "USD", lastTo = "COP")))
        observe(viewModel)

        viewModel.onToSelected("USD")
        advanceUntilIdle()

        assertEquals("COP", viewModel.ready().from.code)
        assertEquals("USD", viewModel.ready().to.code)
    }

    @Test
    fun `offline with saved rates still converts and shows the banner`() = runTest {
        val exchange = FakeExchangeRepository(currencies, snapshot, refreshResult = Result.failure(IOException()))
        val viewModel = createViewModel(exchange = exchange)
        observe(viewModel)
        advanceUntilIdle()

        val state = viewModel.ready()
        assertTrue(state.isShowingSavedRates)
        assertEquals(0, BigDecimal("3300").compareTo(state.result))
    }

    @Test
    fun `offline with nothing saved shows the network error`() = runTest {
        val exchange = FakeExchangeRepository(refreshResult = Result.failure(IOException()))
        val viewModel = createViewModel(exchange = exchange)
        observe(viewModel)
        advanceUntilIdle()

        assertEquals(ConverterUiState.Error(isNetwork = true), viewModel.uiState.value)
    }

    @Test
    fun `favorites skip the source currency and show the converted amount`() = runTest {
        val prefs = FakePreferencesRepository(
            UserPreferences(favoriteCodes = listOf("USD", "EUR", "MXN"), lastFrom = "USD", lastTo = "COP"),
        )
        val viewModel = createViewModel(prefs = prefs)
        observe(viewModel)
        advanceUntilIdle()

        val favorites = viewModel.ready().favorites
        assertEquals(listOf("EUR", "MXN"), favorites.map { it.currency.code })
        assertEquals(0, BigDecimal("0.825").compareTo(favorites.first().amount))
    }

    @Test
    fun `toggling a favorite saves it`() = runTest {
        val prefs = FakePreferencesRepository(UserPreferences(favoriteCodes = listOf("USD")))
        val viewModel = createViewModel(prefs = prefs)
        observe(viewModel)

        viewModel.onToggleFavorite("EUR")
        advanceUntilIdle()

        assertEquals(listOf("USD", "EUR"), prefs.current.favoriteCodes)
        assertEquals(setOf("USD", "EUR"), viewModel.ready().favoriteCodes)
    }
}
