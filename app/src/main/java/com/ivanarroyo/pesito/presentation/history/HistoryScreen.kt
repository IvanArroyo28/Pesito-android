package com.ivanarroyo.pesito.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanarroyo.pesito.R
import com.ivanarroyo.pesito.domain.model.HistoryRange
import com.ivanarroyo.pesito.domain.model.RateHistory
import com.ivanarroyo.pesito.domain.model.RatePoint
import com.ivanarroyo.pesito.domain.model.RateSource
import com.ivanarroyo.pesito.presentation.util.MoneyFormat
import com.ivanarroyo.pesito.ui.theme.PesitoTheme
import com.ivanarroyo.pesito.ui.theme.RateDown
import com.ivanarroyo.pesito.ui.theme.RateUp
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onBack = onBack,
        onRangeSelected = viewModel::onRangeSelected,
        onSourceSelected = viewModel::onSourceSelected,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onRangeSelected: (HistoryRange) -> Unit,
    onSourceSelected: (RateSource) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title, state.from, state.to)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back_cd))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // ---- Periodo ----
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryRange.entries.forEach { range ->
                    FilterChip(
                        selected = state.range == range,
                        onClick = { onRangeSelected(range) },
                        label = { Text(stringResource(range.labelRes())) },
                    )
                }
            }

            // ---- Fuente: promedio o TRM oficial ----
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.source == RateSource.AVERAGE,
                    onClick = { onSourceSelected(RateSource.AVERAGE) },
                    label = { Text(stringResource(R.string.source_average)) },
                )
                FilterChip(
                    selected = state.source == RateSource.BANREP,
                    onClick = { onSourceSelected(RateSource.BANREP) },
                    enabled = state.isBanRepAvailable,
                    label = { Text(stringResource(R.string.source_banrep)) },
                )
            }
            Text(
                text = stringResource(
                    when {
                        !state.isBanRepAvailable -> R.string.source_banrep_unavailable
                        state.source == RateSource.BANREP -> R.string.source_banrep_info
                        else -> R.string.source_average_info
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.size(16.dp))

            when (val content = state.content) {
                HistoryContent.Loading -> CenteredBox { CircularProgressIndicator() }
                HistoryContent.Empty -> CenteredBox {
                    Text(stringResource(R.string.history_empty), textAlign = TextAlign.Center)
                }
                is HistoryContent.Error -> CenteredBox {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(
                                if (content.isNetwork) R.string.history_error_network else R.string.error_generic,
                            ),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.size(12.dp))
                        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                    }
                }
                is HistoryContent.Loaded -> LoadedContent(state, content.history)
            }
        }
    }
}

@Composable
private fun LoadedContent(state: HistoryUiState, history: RateHistory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        RateChart(
            points = history.points,
            accessibilityLabel = stringResource(
                R.string.chart_cd,
                state.from,
                state.to,
                MoneyFormat.format(history.min.rate),
                MoneyFormat.format(history.max.rate),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }

    Spacer(Modifier.size(16.dp))

    // ---- Números clave del periodo ----
    val showYear = history.first.date.year != history.last.date.year
    val change = history.changePercent
    val changeColor = when (change.signum()) {
        1 -> RateUp
        -1 -> RateDown
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            label = stringResource(R.string.stat_change),
            value = MoneyFormat.formatPercent(change),
            valueColor = changeColor,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.stat_min),
            value = MoneyFormat.format(history.min.rate),
            detail = MoneyFormat.formatShortDate(history.min.date, showYear),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.stat_max),
            value = MoneyFormat.format(history.max.rate),
            detail = MoneyFormat.formatShortDate(history.max.date, showYear),
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.size(8.dp))
    Text(
        text = stringResource(R.string.chart_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = valueColor)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun HistoryRange.labelRes(): Int = when (this) {
    HistoryRange.MONTH -> R.string.range_month
    HistoryRange.QUARTER -> R.string.range_quarter
    HistoryRange.YEAR -> R.string.range_year
}

// ---------------- Preview ----------------

@Preview(showBackground = true)
@Composable
private fun HistoryPreview() {
    val start = LocalDate.of(2026, 10, 1)
    val rates = listOf("3312.84", "3273.49", "3209.78", "3238.88", "3181.74", "3156.32", "3191.47", "3148.02", "3235.59", "3297.07", "3316.70")
    PesitoTheme {
        HistoryScreen(
            state = HistoryUiState(
                from = "USD",
                to = "COP",
                isBanRepAvailable = true,
                source = RateSource.BANREP,
                content = HistoryContent.Loaded(
                    RateHistory(rates.mapIndexed { i, r -> RatePoint(start.plusDays(i * 4L), BigDecimal(r)) }),
                ),
            ),
            onBack = {},
            onRangeSelected = {},
            onSourceSelected = {},
            onRetry = {},
        )
    }
}
