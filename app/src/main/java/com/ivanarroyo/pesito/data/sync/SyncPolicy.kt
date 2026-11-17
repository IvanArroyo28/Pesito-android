package com.ivanarroyo.pesito.data.sync

import java.io.IOException

enum class SyncOutcome { SUCCESS, RETRY, FAILURE }

/**
 * Qué hacer según cómo salió la descarga. Está separado del Worker
 * para poder probarlo con un test normal, sin Android.
 */
object SyncPolicy {

    /** Tras 3 intentos fallidos nos rendimos hasta el próximo turno (12 h después). */
    const val MAX_ATTEMPTS = 3

    fun decide(result: Result<Unit>, runAttemptCount: Int): SyncOutcome = when {
        result.isSuccess -> SyncOutcome.SUCCESS
        // Sin internet o servidor caído: vale la pena reintentar en un rato.
        result.exceptionOrNull() is IOException && runAttemptCount < MAX_ATTEMPTS -> SyncOutcome.RETRY
        else -> SyncOutcome.FAILURE
    }
}
