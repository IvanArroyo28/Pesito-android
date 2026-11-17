package com.ivanarroyo.pesito.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserPreferencesTest {

    @Test
    fun `toggle adds a new favorite at the end`() =
        assertEquals(listOf("USD", "EUR"), UserPreferences.toggle(listOf("USD"), "EUR"))

    @Test
    fun `toggle removes an existing favorite`() =
        assertEquals(listOf("EUR"), UserPreferences.toggle(listOf("USD", "EUR"), "USD"))

    @Test
    fun `toggle twice leaves the list as it was`() {
        val start = listOf("USD", "EUR")
        assertEquals(start, UserPreferences.toggle(UserPreferences.toggle(start, "MXN"), "MXN"))
    }
}
