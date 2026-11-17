package com.ivanarroyo.pesito.domain.model

/** Lo que el usuario eligió y queremos recordar aunque cierre la app. */
data class UserPreferences(
    /** En el orden en que el usuario las marcó. */
    val favoriteCodes: List<String> = DEFAULT_FAVORITES,
    val lastFrom: String = DEFAULT_FROM,
    val lastTo: String = DEFAULT_TO,
) {
    companion object {
        /** La primera vez la app ya muestra algo útil; el usuario las cambia cuando quiera. */
        val DEFAULT_FAVORITES = listOf("USD", "EUR", "MXN")
        const val DEFAULT_FROM = "USD"
        const val DEFAULT_TO = "COP"

        /** Agrega la moneda al final si no está; la quita si ya estaba. */
        fun toggle(favorites: List<String>, code: String): List<String> =
            if (code in favorites) favorites - code else favorites + code
    }
}
