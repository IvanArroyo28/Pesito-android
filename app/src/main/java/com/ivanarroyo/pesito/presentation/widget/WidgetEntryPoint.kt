package com.ivanarroyo.pesito.presentation.widget

import com.ivanarroyo.pesito.data.sync.RatesSyncScheduler
import com.ivanarroyo.pesito.domain.usecase.ConvertCurrencyUseCase
import com.ivanarroyo.pesito.domain.usecase.ObserveConverterDataUseCase
import com.ivanarroyo.pesito.domain.usecase.ObserveUserPreferencesUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * El widget no es una Activity ni un ViewModel, así que Hilt no le puede poner @Inject.
 * Un EntryPoint es la "puerta" para pedirle a Hilt las dependencias a mano.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun observeConverterData(): ObserveConverterDataUseCase
    fun observeUserPreferences(): ObserveUserPreferencesUseCase
    fun convertCurrency(): ConvertCurrencyUseCase
    fun ratesSyncScheduler(): RatesSyncScheduler
}
