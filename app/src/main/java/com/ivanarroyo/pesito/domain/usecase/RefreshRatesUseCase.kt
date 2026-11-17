package com.ivanarroyo.pesito.domain.usecase

import com.ivanarroyo.pesito.domain.repository.ExchangeRepository
import javax.inject.Inject

/** Pide tasas nuevas a internet. Lo usarán la pantalla y, en la Fase 6, WorkManager. */
class RefreshRatesUseCase @Inject constructor(
    private val repository: ExchangeRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.refresh()
}
