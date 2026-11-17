package com.ivanarroyo.pesito.presentation.util

import java.math.BigDecimal

/**
 * Reglas del campo de monto. En Colombia el decimal es la coma, pero muchos teclados
 * solo muestran el punto, así que aceptamos los dos y siempre mostramos coma.
 */
object AmountInput {

    private const val MAX_INTEGER_DIGITS = 12
    private const val MAX_DECIMALS = 2

    /** Devuelve el texto limpio, o null si lo escrito no es válido (y se ignora). */
    fun sanitize(raw: String): String? {
        val text = raw.trim().replace('.', ',')
        if (text.isEmpty()) return ""
        if (text.any { !it.isDigit() && it != ',' }) return null
        if (text.count { it == ',' } > 1) return null

        val integerPart = text.substringBefore(',')
        val decimalPart = text.substringAfter(',', missingDelimiterValue = "")
        if (integerPart.length > MAX_INTEGER_DIGITS) return null
        if (decimalPart.length > MAX_DECIMALS) return null
        return text
    }

    /** "1234,5" → 1234.5. Vacío o solo "," → null (no hay monto todavía). */
    fun parse(text: String): BigDecimal? {
        val normalized = text.replace(',', '.')
        if (normalized.isEmpty() || normalized == ".") return null
        return normalized.toBigDecimalOrNull()
    }
}
