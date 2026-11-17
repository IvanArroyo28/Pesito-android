package com.ivanarroyo.pesito.presentation.widget

import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import com.ivanarroyo.pesito.domain.model.UserPreferences
import com.ivanarroyo.pesito.domain.usecase.ConvertCurrencyUseCase
import com.ivanarroyo.pesito.domain.usecase.ConverterData
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class WidgetStateBuilderTest {

    private val convert = ConvertCurrencyUseCase()

    private val data = ConverterData(
        currencies = listOf(
            Currency("COP", "Peso colombiano", "$"),
            Currency("USD", "Dólar estadounidense", "US$"),
            Currency("EUR", "Euro", "€"),
        ),
        snapshot = RatesSnapshot(
            date = LocalDate.of(2026, 11, 13),
            ratesPerUsd = mapOf(
                "USD" to BigDecimal.ONE,
                "COP" to BigDecimal("3300"),
                "EUR" to BigDecimal("0.825"),
            ),
        ),
    )

    @Test
    fun `no saved rates shows the empty state`() =
        assertEquals(WidgetState.NoData, WidgetStateBuilder.build(null, UserPreferences(), convert))

    @Test
    fun `main pair is the last pair used in the app`() {
        val state = WidgetStateBuilder.build(data, UserPreferences(lastFrom = "USD", lastTo = "COP"), convert)
            as WidgetState.Ready

        assertEquals("USD", state.from)
        assertEquals("COP", state.to)
        assertEquals(0, BigDecimal("3300").compareTo(state.unitRate))
    }

    @Test
    fun `rows skip the main pair and favorites without rate`() {
        val prefs = UserPreferences(favoriteCodes = listOf("USD", "EUR", "MXN"), lastFrom = "USD", lastTo = "COP")

        val state = WidgetStateBuilder.build(data, prefs, convert) as WidgetState.Ready

        // USD ya es el par principal y MXN no tiene tasa: solo queda EUR.
        assertEquals(listOf("EUR"), state.rows.map { it.code })
        // 1 EUR = 3300 / 0.825 = 4000 COP
        assertEquals("4000.00", state.rows.single().valueInTarget.setScale(2, RoundingMode.HALF_EVEN).toPlainString())
    }
}
