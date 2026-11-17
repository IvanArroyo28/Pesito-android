package com.ivanarroyo.pesito.presentation.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ivanarroyo.pesito.MainActivity
import com.ivanarroyo.pesito.R
import com.ivanarroyo.pesito.presentation.util.MoneyFormat
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/**
 * Widget de pantalla de inicio hecho con Glance (Compose para widgets).
 * Glance NO usa los composables normales: Column, Text, etc. vienen de androidx.glance.
 */
class PesitoWidget : GlanceAppWidget() {

    /** Exact: recibimos el tamaño real para decidir cuántas filas caben. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)

        // Leemos lo guardado en Room y DataStore. El widget nunca llama a internet por su cuenta.
        val state = WidgetStateBuilder.build(
            data = entryPoint.observeConverterData()().first(),
            prefs = entryPoint.observeUserPreferences()().first(),
            convert = entryPoint.convertCurrency(),
        )

        provideContent {
            GlanceTheme {
                WidgetContent(state)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        // ---- Encabezado: nombre + botón de actualizar ----
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.app_name),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.defaultWeight())
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = context.getString(R.string.widget_refresh_cd),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier
                    .size(24.dp)
                    .clickable(actionRunCallback<RefreshRatesAction>()),
            )
        }

        when (state) {
            WidgetState.NoData -> {
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = context.getString(R.string.widget_no_data),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                )
            }

            is WidgetState.Ready -> {
                // ---- Par principal en grande ----
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "1 ${state.from} =",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                )
                Text(
                    text = "${MoneyFormat.format(state.unitRate)} ${state.to}",
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )

                // ---- Favoritas: solo las que caben según el alto del widget ----
                val freeHeight = size.height - HEADER_AND_MAIN_HEIGHT
                val rowsThatFit = (freeHeight / ROW_HEIGHT).toInt().coerceIn(0, MAX_ROWS)
                state.rows.take(rowsThatFit).forEach { row ->
                    WidgetRowView(row, state.to, GlanceTheme.colors.onSurface)
                }

                if (size.height >= SHOW_DATE_MIN_HEIGHT) {
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = context.getString(R.string.widget_rate_date, MoneyFormat.formatShortDate(state.rateDate)),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetRowView(row: WidgetRow, target: String, color: ColorProvider) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.code,
            style = TextStyle(color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = "${MoneyFormat.format(row.valueInTarget)} $target",
            style = TextStyle(color = color, fontSize = 13.sp),
            maxLines = 1,
        )
    }
}

private val HEADER_AND_MAIN_HEIGHT = 96.dp
private val ROW_HEIGHT = 22.dp
private val SHOW_DATE_MIN_HEIGHT = 150.dp
private const val MAX_ROWS = 5

/** Botón de actualizar: pide a WorkManager una descarga YA (si hay internet). */
class RefreshRatesAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .ratesSyncScheduler()
            .refreshNow()
    }
}

/** Android habla con el widget a través de este receptor (va registrado en el Manifest). */
class PesitoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PesitoWidget()
}
