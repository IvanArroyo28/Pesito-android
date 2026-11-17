package com.ivanarroyo.pesito.data.remote.dto

import com.ivanarroyo.pesito.data.remote.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/** Un elemento de GET /v2/currencies. */
@Serializable
data class CurrencyDto(
    @SerialName("iso_code") val isoCode: String,
    val name: String,
    val symbol: String? = null,
    /** "currency" o "crypto". */
    val type: String,
)

/** Un elemento de GET /v2/rates: {"date":"2026-11-13","base":"USD","quote":"COP","rate":3293.28} */
@Serializable
data class RateDto(
    val date: String,
    val base: String,
    val quote: String,
    @Serializable(with = BigDecimalSerializer::class)
    val rate: BigDecimal,
)
