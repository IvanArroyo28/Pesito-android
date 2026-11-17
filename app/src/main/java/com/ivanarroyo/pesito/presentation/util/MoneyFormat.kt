package com.ivanarroyo.pesito.presentation.util

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formato colombiano: punto para miles y coma para decimales (3.293,28). */
object MoneyFormat {

    private val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-CO"))

    /**
     * - 1 o más: dos decimales → 3.293.280,00
     * - Menos de 1: 4 cifras significativas para no mostrar "0,00" → 1 COP = 0,0003037 USD
     */
    fun format(value: BigDecimal): String {
        val abs = value.abs()
        return if (abs >= BigDecimal.ONE || abs.signum() == 0) {
            DecimalFormat("#,##0.00", symbols).apply { roundingMode = RoundingMode.HALF_EVEN }
                .format(value)
        } else {
            val rounded = value.round(MathContext(4, RoundingMode.HALF_EVEN))
            DecimalFormat("0.##########", symbols).format(rounded)
        }
    }

    /** +2,35 % / −1,10 % (siempre con signo, para que se note si subió o bajó). */
    fun formatPercent(value: BigDecimal): String =
        // El % va entre comillas: sin ellas DecimalFormat multiplicaría el número por 100.
        DecimalFormat("+#,##0.00 '%';-#,##0.00 '%'", symbols).apply { roundingMode = RoundingMode.HALF_EVEN }
            .format(value)

    fun formatDate(date: LocalDate): String = dateFormatter.format(date)

    private val shortDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-CO"))

    private val dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.forLanguageTag("es-CO"))

    /** "17 nov, 3:42 p. m." en la hora del celular. */
    fun formatDateTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        dateTimeFormatter.format(instant.atZone(zone))

    /**
     * "13 nov" para los ejes de la gráfica.
     * [withYear] = true cuando el periodo cruza de un año a otro ("17 nov 2025"),
     * si no, "17 nov" al inicio y al final de la gráfica de 1 año sería ambiguo.
     */
    fun formatShortDate(date: LocalDate, withYear: Boolean = false): String =
        if (withYear) dateFormatter.format(date) else shortDateFormatter.format(date)
}
