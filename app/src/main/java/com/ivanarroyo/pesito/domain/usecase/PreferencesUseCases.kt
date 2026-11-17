package com.ivanarroyo.pesito.domain.usecase

import com.ivanarroyo.pesito.domain.model.UserPreferences
import com.ivanarroyo.pesito.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveUserPreferencesUseCase @Inject constructor(
    private val repository: PreferencesRepository,
) {
    operator fun invoke(): Flow<UserPreferences> = repository.preferences
}

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: PreferencesRepository,
) {
    suspend operator fun invoke(code: String) = repository.toggleFavorite(code)
}

/** Recuerda el último par (USD → COP) para abrir la app justo donde la dejaste. */
class SaveLastPairUseCase @Inject constructor(
    private val repository: PreferencesRepository,
) {
    suspend operator fun invoke(from: String, to: String) = repository.saveLastPair(from, to)
}
