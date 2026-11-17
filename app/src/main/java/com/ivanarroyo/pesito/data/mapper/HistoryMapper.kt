package com.ivanarroyo.pesito.data.mapper

import com.ivanarroyo.pesito.data.remote.dto.RateDto
import com.ivanarroyo.pesito.domain.model.RatePoint
import com.ivanarroyo.pesito.domain.model.RatesSnapshot
import java.math.BigDecimal
import java.time.LocalDate

fun List<RateDto>.toPoints(): List<RatePoint> =
    map { RatePoint(LocalDate.parse(it.date), it.rate) }.sortedBy { it.date }

/** USD→COP a COP→USD: 1 / tasa. */
fun List<RatePoint>.inverted(): List<RatePoint> =
    map { it.copy(rate = BigDecimal.ONE.divide(it.rate, RatesSnapshot.MATH_CONTEXT)) }
