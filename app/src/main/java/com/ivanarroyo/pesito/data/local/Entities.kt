package com.ivanarroyo.pesito.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey val code: String,
    val name: String,
    val symbol: String,
)

/**
 * La tasa se guarda como TEXTO ("3293.28") y no como número decimal:
 * SQLite guardaría un REAL (Double) y perderíamos la precisión del BigDecimal.
 */
@Entity(tableName = "rates")
data class RateEntity(
    @PrimaryKey val code: String,
    val ratePerUsd: String,
    /** Fecha de la tasa en formato ISO: "2026-11-13". */
    val date: String,
)
