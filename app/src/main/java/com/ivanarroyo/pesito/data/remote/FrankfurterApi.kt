package com.ivanarroyo.pesito.data.remote

import com.ivanarroyo.pesito.data.remote.dto.CurrencyDto
import com.ivanarroyo.pesito.data.remote.dto.RateDto
import retrofit2.http.GET
import retrofit2.http.Query

/** API gratuita y sin clave: https://frankfurter.dev */
interface FrankfurterApi {

    @GET("v2/currencies")
    suspend fun getCurrencies(): List<CurrencyDto>

    /** Tasas más recientes de todas las monedas contra [base]. */
    @GET("v2/rates")
    suspend fun getLatestRates(@Query("base") base: String = "usd"): List<RateDto>

    /**
     * Una tasa por día hábil entre [from] y [to] (fechas "2026-10-01").
     * [providers] = "BANREP" para la TRM oficial; null = promedio (Retrofit omite los null).
     */
    @GET("v2/rates")
    suspend fun getRateHistory(
        @Query("base") base: String,
        @Query("quotes") quotes: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("providers") providers: String? = null,
    ): List<RateDto>

    companion object {
        const val BASE_URL = "https://api.frankfurter.dev/"
    }
}
