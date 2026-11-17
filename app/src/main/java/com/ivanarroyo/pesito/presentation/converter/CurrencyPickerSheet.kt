package com.ivanarroyo.pesito.presentation.converter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivanarroyo.pesito.R
import com.ivanarroyo.pesito.domain.model.Currency
import com.ivanarroyo.pesito.domain.usecase.ObserveConverterDataUseCase
import kotlinx.coroutines.launch
import java.text.Normalizer

/** Hoja inferior con buscador para elegir una moneda. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerSheet(
    currencies: List<Currency>,
    selectedCode: String,
    onSelect: (String) -> Unit,
    favoriteCodes: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }

    // Las favoritas van arriba del todo para encontrarlas rápido.
    val favorites = remember(currencies, favoriteCodes) {
        currencies.filter { it.code in favoriteCodes }
    }
    val popular = remember(currencies) {
        currencies.filter { it.code in ObserveConverterDataUseCase.POPULAR }
    }
    val others = remember(currencies) {
        currencies.filterNot { it.code in ObserveConverterDataUseCase.POPULAR }
    }
    val results = remember(currencies, query) {
        val q = query.normalizeForSearch()
        currencies.filter { it.code.normalizeForSearch().contains(q) || it.name.normalizeForSearch().contains(q) }
    }

    // Cierra la hoja con animación y luego avisa al padre.
    val pick: (String) -> Unit = { code ->
        onSelect(code)
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.picker_title),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.picker_search)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.clear_cd))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        }

        LazyColumn(Modifier.fillMaxWidth()) {
            // Una moneda puede salir en dos secciones (favorita y popular),
            // así que cada key lleva el nombre de la sección para no repetirse.
            val row: @Composable (Currency) -> Unit = {
                CurrencyRow(
                    currency = it,
                    selected = it.code == selectedCode,
                    isFavorite = it.code in favoriteCodes,
                    onClick = pick,
                    onToggleFavorite = onToggleFavorite,
                )
            }
            if (query.isBlank()) {
                if (favorites.isNotEmpty()) {
                    item(key = "header_favorites") { SectionHeader(stringResource(R.string.picker_favorites)) }
                    items(favorites, key = { "fav_${it.code}" }) { row(it) }
                }
                item(key = "header_popular") { SectionHeader(stringResource(R.string.picker_popular)) }
                items(popular, key = { "pop_${it.code}" }) { row(it) }
                item(key = "header_all") { SectionHeader(stringResource(R.string.picker_all)) }
                items(others, key = { "all_${it.code}" }) { row(it) }
            } else if (results.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.picker_empty, query),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(results, key = { "res_${it.code}" }) { row(it) }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun CurrencyRow(
    currency: Currency,
    selected: Boolean,
    isFavorite: Boolean,
    onClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(currency.code) }
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = currency.code,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(56.dp),
        )
        Text(
            text = currency.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = currency.symbol,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = { onToggleFavorite(currency.code) }) {
            Icon(
                painter = painterResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border),
                contentDescription = stringResource(
                    if (isFavorite) R.string.favorite_remove_cd else R.string.favorite_add_cd,
                ),
                tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Dólar" y "dolar" deben coincidir: minúsculas y sin tildes. */
private fun String.normalizeForSearch(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
