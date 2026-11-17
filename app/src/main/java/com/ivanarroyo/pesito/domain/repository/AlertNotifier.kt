package com.ivanarroyo.pesito.domain.repository

import com.ivanarroyo.pesito.domain.model.TriggeredAlert

/**
 * El dominio solo dice "avisa"; CÓMO se avisa (notificación de Android)
 * lo decide la implementación. Así el caso de uso se prueba sin Android.
 */
interface AlertNotifier {
    fun notify(triggered: List<TriggeredAlert>)
}
