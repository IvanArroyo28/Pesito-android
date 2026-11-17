package com.ivanarroyo.pesito.data.mapper

import com.ivanarroyo.pesito.data.remote.dto.CurrencyDto
import com.ivanarroyo.pesito.data.remote.dto.RateDto
import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

private val SPANISH_CO: Locale = Locale.forLanguageTag("es-CO")
private const val USD = "USD"

/**
 * Las criptomonedas se descartan: Pesito es para monedas de verdad.
 * La API da los nombres en inglés ("Colombian Peso"); Java ya trae la traducción
 * ("peso colombiano"), así que la usamos y si no existe dejamos el nombre de la API.
 */
fun CurrencyDto.toDomainOrNull(locale: Locale = SPANISH_CO): Currency? {
    if (type != "currency") return null
    val code = isoCode.uppercase()
    return Currency(
        code = code,
        name = localizedName(code, locale) ?: name,
        symbol = symbol?.takeIf { it.isNotBlank() } ?: code,
    )
}

private fun localizedName(code: String, locale: Locale): String? =
    runCatching { java.util.Currency.getInstance(code).getDisplayName(locale) }
        .getOrNull()
        // Si Java no conoce la moneda, devuelve el mismo código: eso no nos sirve como nombre.
        ?.takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) }
        ?.replaceFirstChar { it.titlecase(locale) }

/**
 * Convierte la lista de tasas (base USD) en un RatesSnapshot.
 * Cada moneda puede venir con una fecha distinta: tomamos la más reciente como fecha de la foto.
 * El dólar no viene en la lista (es la base), así que lo agregamos con tasa 1.
 */
fun List<RateDto>.toSnapshot(): RatesSnapshot {
    val rates = associate { it.quote.uppercase() to it.rate } + (USD to BigDecimal.ONE)
    val date = maxOfOrNull { LocalDate.parse(it.date) } ?: LocalDate.now()
    return RatesSnapshot(date = date, ratesPerUsd = rates)
}
