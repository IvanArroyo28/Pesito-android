package com.ivanarroyo.pesito.domain.model

/** Una moneda real (no cripto), por ejemplo COP · Peso colombiano · $. */
data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
)
