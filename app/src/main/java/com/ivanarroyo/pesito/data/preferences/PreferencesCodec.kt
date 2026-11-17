package com.ivanarroyo.pesito.data.preferences

/**
 * DataStore no guarda listas, pero sí texto. Guardamos "USD,EUR,MXN".
 * (Un Set<String> perdería el orden en que el usuario marcó sus favoritas.)
 */
object PreferencesCodec {

    private const val SEPARATOR = ","

    fun encodeCodes(codes: List<String>): String = codes.joinToString(SEPARATOR)

    /** Texto vacío = el usuario quitó todas sus favoritas (lista vacía, NO las de fábrica). */
    fun decodeCodes(text: String): List<String> =
        text.split(SEPARATOR)
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .distinct()
}
