package com.ivanarroyo.pesito.presentation.converter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanarroyo.pesito.R
import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.presentation.util.MoneyFormat
import com.ivanarroyo.pesito.ui.theme.PesitoTheme
import java.math.BigDecimal
import java.time.LocalDate

/** Conecta el ViewModel con la pantalla. La pantalla en sí no sabe que existe Hilt. */
@Composable
fun ConverterRoute(
    onOpenHistory: (from: String, to: String) -> Unit,
    viewModel: ConverterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConverterScreen(
        state = state,
        amountText = viewModel.amountText,
        onAmountChange = viewModel::onAmountChange,
        onFromSelected = viewModel::onFromSelected,
        onToSelected = viewModel::onToSelected,
        onSwap = viewModel::onSwap,
        onRefresh = viewModel::refresh,
        onToggleFavorite = viewModel::onToggleFavorite,
        onOpenHistory = onOpenHistory,
    )
}

private enum class PickerTarget { FROM, TO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    state: ConverterUiState,
    amountText: String,
    onAmountChange: (String) -> Unit,
    onFromSelected: (String) -> Unit,
    onToSelected: (String) -> Unit,
    onSwap: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenHistory: (from: String, to: String) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state) {
                ConverterUiState.Loading -> LoadingContent()
                is ConverterUiState.Error -> ErrorContent(state.isNetwork, onRefresh)
                is ConverterUiState.Ready -> ReadyContent(
                    state = state,
                    amountText = amountText,
                    onAmountChange = onAmountChange,
                    onFromSelected = onFromSelected,
                    onToSelected = onToSelected,
                    onSwap = onSwap,
                    onRefresh = onRefresh,
                    onToggleFavorite = onToggleFavorite,
                    onOpenHistory = onOpenHistory,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyContent(
    state: ConverterUiState.Ready,
    amountText: String,
    onAmountChange: (String) -> Unit,
    onFromSelected: (String) -> Unit,
    onToSelected: (String) -> Unit,
    onSwap: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenHistory: (from: String, to: String) -> Unit,
) {
    var pickerTarget by rememberSaveable { mutableStateOf<PickerTarget?>(null) }
    var swapTurns by remember { mutableFloatStateOf(0f) }
    val swapRotation by animateFloatAsState(targetValue = swapTurns * 180f, label = "swap")

    // Jalar hacia abajo para actualizar las tasas.
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isShowingSavedRates) {
                SavedRatesBanner(MoneyFormat.formatDate(state.rateDate))
                Spacer(Modifier.size(12.dp))
            }

            // ---- Moneda de origen + monto ----
            CurrencyCard(label = stringResource(R.string.from_label)) {
                CurrencyChip(currency = state.from, onClick = { pickerTarget = PickerTarget.FROM })
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.amount_label)) },
                    prefix = { Text(state.from.symbol + " ") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ---- Botón para intercambiar ----
            FilledTonalIconButton(
                onClick = {
                    swapTurns += 1f
                    onSwap()
                },
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(52.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_swap_vert),
                    contentDescription = stringResource(R.string.swap_cd),
                    modifier = Modifier.rotate(swapRotation),
                )
            }

            // ---- Moneda destino + resultado ----
            CurrencyCard(
                label = stringResource(R.string.to_label),
                highlighted = true,
            ) {
                CurrencyChip(currency = state.to, onClick = { pickerTarget = PickerTarget.TO })
                Spacer(Modifier.size(12.dp))
                Text(
                    text = state.result?.let { "${state.to.symbol} ${MoneyFormat.format(it)}" } ?: "—",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                )
            }

            Spacer(Modifier.size(20.dp))
            Text(
                text = stringResource(
                    R.string.unit_rate,
                    state.from.code,
                    MoneyFormat.format(state.unitRate),
                    state.to.code,
                ),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.rate_date, MoneyFormat.formatDate(state.rateDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = { onOpenHistory(state.from.code, state.to.code) }) {
                Icon(painterResource(R.drawable.ic_show_chart), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.open_history))
            }

            // ---- Favoritas: el mismo monto en varias monedas de un vistazo ----
            Spacer(Modifier.size(24.dp))
            FavoritesSection(
                favorites = state.favorites,
                onFavoriteClick = onToSelected,
            )
            Spacer(Modifier.size(16.dp))
        }
    }

    pickerTarget?.let { target ->
        CurrencyPickerSheet(
            currencies = state.currencies,
            selectedCode = if (target == PickerTarget.FROM) state.from.code else state.to.code,
            onSelect = { code ->
                if (target == PickerTarget.FROM) onFromSelected(code) else onToSelected(code)
            },
            favoriteCodes = state.favoriteCodes,
            onToggleFavorite = onToggleFavorite,
            onDismiss = { pickerTarget = null },
        )
    }
}

@Composable
private fun FavoritesSection(
    favorites: List<FavoriteConversion>,
    onFavoriteClick: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.favorites_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(8.dp))
        if (favorites.isEmpty()) {
            Text(
                text = stringResource(R.string.favorites_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
        ) {
            favorites.forEachIndexed { index, favorite ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                FavoriteRow(favorite, onClick = { onFavoriteClick(favorite.currency.code) })
            }
        }
    }
}

/** Una fila: código y nombre a la izquierda, el monto convertido a la derecha. Tocarla la pone como destino. */
@Composable
private fun FavoriteRow(favorite: FavoriteConversion, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = favorite.currency.code,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = favorite.currency.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = favorite.amount?.let { "${favorite.currency.symbol} ${MoneyFormat.format(it)}" } ?: "—",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
        )
    }
}

/** Aviso cuando no hay internet: la app sigue funcionando con lo guardado. */
@Composable
private fun SavedRatesBanner(date: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            text = stringResource(R.string.saved_rates_banner, date),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun CurrencyCard(
    label: String,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = if (highlighted) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(8.dp))
            content()
        }
    }
}

/** Botón tipo "píldora" que muestra la moneda elegida y abre el selector. */
@Composable
private fun CurrencyChip(currency: Currency, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(currency.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                text = currency.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(painterResource(R.drawable.ic_arrow_drop_down), contentDescription = null)
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.size(16.dp))
        Text(stringResource(R.string.loading_rates))
    }
}

@Composable
private fun ErrorContent(isNetwork: Boolean, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(if (isNetwork) R.string.error_network else R.string.error_generic),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = onRetry) {
            Icon(painterResource(R.drawable.ic_refresh), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.retry))
        }
    }
}

// ---------------- Previews ----------------

private val previewCurrencies = listOf(
    Currency("COP", "Peso colombiano", "$"),
    Currency("USD", "Dólar estadounidense", "US$"),
    Currency("EUR", "Euro", "€"),
)

@Preview(showBackground = true)
@Composable
private fun ConverterReadyPreview() {
    PesitoTheme {
        ConverterScreen(
            state = ConverterUiState.Ready(
                currencies = previewCurrencies,
                from = previewCurrencies[1],
                to = previewCurrencies[0],
                result = BigDecimal("329328.00"),
                unitRate = BigDecimal("3293.28"),
                rateDate = LocalDate.of(2026, 11, 13),
                isShowingSavedRates = true,
                favoriteCodes = setOf("EUR"),
                favorites = listOf(FavoriteConversion(previewCurrencies[2], BigDecimal("86.00"))),
            ),
            amountText = "100",
            onAmountChange = {},
            onFromSelected = {},
            onToSelected = {},
            onSwap = {},
            onRefresh = {},
            onToggleFavorite = {},
            onOpenHistory = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConverterErrorPreview() {
    PesitoTheme {
        ConverterScreen(
            state = ConverterUiState.Error(isNetwork = true),
            amountText = "1",
            onAmountChange = {},
            onFromSelected = {},
            onToSelected = {},
            onSwap = {},
            onRefresh = {},
            onToggleFavorite = {},
            onOpenHistory = { _, _ -> },
        )
    }
}
