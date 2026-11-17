package com.ivanarroyo.pesito.data.mapper

import com.ivanarroyo.pesito.data.local.CurrencyEntity
import com.ivanarroyo.pesito.data.local.RateEntity
import com.ivanarroyo.pesito.data.remote.dto.CurrencyDto
import com.ivanarroyo.pesito.data.remote.dto.RateDto
import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

private val SPANISH_CO: Locale = Locale.forLanguageTag("es-CO")
private const val USD = "USD"

// ---------------- API → base de datos ----------------

/**
 * Las criptomonedas se descartan: Pesito es para monedas de verdad.
 * La API da los nombres en inglés ("Colombian Peso"); Java ya trae la traducción
 * ("peso colombiano"), así que la usamos y si no existe dejamos el nombre de la API.
 */
fun CurrencyDto.toEntityOrNull(locale: Locale = SPANISH_CO): CurrencyEntity? {
    if (type != "currency") return null
    val code = isoCode.uppercase()
    return CurrencyEntity(
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
 * Tasas de la API (base USD) → filas para guardar.
 * El dólar no viene en la lista (es la base), así que lo agregamos con tasa 1.
 */
fun List<RateDto>.toEntities(fetchedAt: Long): List<RateEntity> {
    if (isEmpty()) return emptyList()
    val latestDate = maxOf { it.date } // Las fechas ISO se ordenan bien como texto.
    return map { RateEntity(it.quote.uppercase(), it.rate.toPlainString(), it.date, fetchedAt) } +
        RateEntity(USD, BigDecimal.ONE.toPlainString(), latestDate, fetchedAt)
}

// ---------------- base de datos → dominio ----------------

fun CurrencyEntity.toDomain(): Currency = Currency(code = code, name = name, symbol = symbol)

/**
 * Filas guardadas → RatesSnapshot. Null si no hay nada guardado.
 * Cada moneda puede tener una fecha distinta: usamos la más reciente como fecha de la foto.
 */
fun List<RateEntity>.toSnapshotOrNull(): RatesSnapshot? {
    if (isEmpty()) return null
    return RatesSnapshot(
        date = LocalDate.parse(maxOf { it.date }),
        ratesPerUsd = associate { it.code to BigDecimal(it.ratePerUsd) },
        updatedAt = Instant.ofEpochMilli(maxOf { it.fetchedAt }),
    )
}
