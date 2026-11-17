package com.ivanarroyo.pesito.presentation.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivanarroyo.pesito.domain.model.RatePoint
import com.ivanarroyo.pesito.presentation.util.MoneyFormat
import kotlin.math.roundToInt

/**
 * Gráfica de línea dibujada a mano con Canvas (sin librerías).
 * Arrastra el dedo sobre ella para ver la tasa de cada día.
 */
@Composable
fun RateChart(
    points: List<RatePoint>,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    // Float basta para DIBUJAR (los cálculos de dinero siguen en BigDecimal).
    val values = remember(points) { points.map { it.rate.toFloat() } }
    val minValue = remember(values) { values.min() }
    val maxValue = remember(values) { values.max() }
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    val guideColor = MaterialTheme.colorScheme.outline
    val dotCenterColor = MaterialTheme.colorScheme.surface
    val shown = points[selectedIndex ?: points.lastIndex]

    Column(modifier) {
        // Encabezado: el punto que se está tocando (o el último si no se toca nada).
        Text(
            text = MoneyFormat.format(shown.rate),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = MoneyFormat.formatDate(shown.date),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .semantics { contentDescription = accessibilityLabel }
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        selectedIndex = indexAt(offset.x, size.width.toFloat(), points.size)
                    }
                }
                .pointerInput(points) {
                    // Solo arrastre HORIZONTAL: el vertical sigue sirviendo para hacer scroll.
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            selectedIndex = indexAt(offset.x, size.width.toFloat(), points.size)
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null },
                    ) { change, _ ->
                        selectedIndex = indexAt(change.position.x, size.width.toFloat(), points.size)
                    }
                },
        ) {
            val verticalPadding = 8.dp.toPx()
            val chartHeight = size.height - verticalPadding * 2
            val span = (maxValue - minValue).takeIf { it > 0f } ?: 1f
            val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f

            fun x(index: Int) = index * stepX
            fun y(value: Float) = verticalPadding + chartHeight - (value - minValue) / span * chartHeight

            // Líneas guía de mínimo y máximo.
            val dashed = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            listOf(maxValue, minValue).forEach { value ->
                drawLine(
                    color = guideColor.copy(alpha = 0.5f),
                    start = Offset(0f, y(value)),
                    end = Offset(size.width, y(value)),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashed,
                )
            }

            if (values.size == 1) {
                drawCircle(lineColor, radius = 5.dp.toPx(), center = Offset(size.width / 2, y(values[0])))
                return@Canvas
            }

            val line = Path().apply {
                values.forEachIndexed { i, value ->
                    if (i == 0) moveTo(x(i), y(value)) else lineTo(x(i), y(value))
                }
            }
            // Relleno degradado bajo la línea.
            val fill = Path().apply {
                addPath(line)
                lineTo(x(values.lastIndex), size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = fill,
                brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.30f), Color.Transparent)),
            )
            drawPath(
                path = line,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // Punto seleccionado con su línea vertical.
            selectedIndex?.let { i ->
                drawLine(
                    color = guideColor,
                    start = Offset(x(i), 0f),
                    end = Offset(x(i), size.height),
                    strokeWidth = 1.dp.toPx(),
                )
                val center = Offset(x(i), y(values[i]))
                drawCircle(lineColor, radius = 6.dp.toPx(), center = center)
                drawCircle(dotCenterColor, radius = 3.dp.toPx(), center = center)
            }
        }

        // Fechas de inicio y fin bajo la gráfica.
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = MoneyFormat.formatShortDate(points.first().date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = MoneyFormat.formatShortDate(points.last().date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Convierte la posición X del dedo en el índice del punto más cercano. */
internal fun indexAt(x: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    return ((x / width) * (count - 1)).roundToInt().coerceIn(0, count - 1)
}
