package com.ivanarroyo.pesito.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ivanarroyo.pesito.domain.model.UserPreferences
import com.ivanarroyo.pesito.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * DataStore: el reemplazo moderno de SharedPreferences.
 * Lee y escribe en segundo plano (nunca bloquea la pantalla) y expone un Flow.
 */
class DataStorePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data
        // Si el archivo se daña, arrancamos con los valores por defecto en vez de cerrar la app.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            UserPreferences(
                // Si la clave no existe todavía (primera vez) usamos las favoritas de fábrica.
                favoriteCodes = prefs[Keys.FAVORITES]?.let(PreferencesCodec::decodeCodes)
                    ?: UserPreferences.DEFAULT_FAVORITES,
                lastFrom = prefs[Keys.LAST_FROM] ?: UserPreferences.DEFAULT_FROM,
                lastTo = prefs[Keys.LAST_TO] ?: UserPreferences.DEFAULT_TO,
            )
        }

    override suspend fun toggleFavorite(code: String) {
        // edit {} es atómico: dos toques rápidos no se pisan entre sí.
        dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES]?.let(PreferencesCodec::decodeCodes)
                ?: UserPreferences.DEFAULT_FAVORITES
            prefs[Keys.FAVORITES] = PreferencesCodec.encodeCodes(UserPreferences.toggle(current, code))
        }
    }

    override suspend fun saveLastPair(from: String, to: String) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_FROM] = from
            prefs[Keys.LAST_TO] = to
        }
    }

    private object Keys {
        val FAVORITES = stringPreferencesKey("favorite_codes")
        val LAST_FROM = stringPreferencesKey("last_from")
        val LAST_TO = stringPreferencesKey("last_to")
    }
}
