package com.ivanarroyo.pesito.domain.repository

import com.ivanarroyo.pesito.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    /** Emite de nuevo cada vez que algo cambia. */
    val preferences: Flow<UserPreferences>

    suspend fun toggleFavorite(code: String)

    suspend fun saveLastPair(from: String, to: String)
}
