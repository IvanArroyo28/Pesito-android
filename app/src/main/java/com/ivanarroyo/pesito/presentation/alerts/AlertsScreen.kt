package com.ivanarroyo.pesito.presentation.alerts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanarroyo.pesito.R
import com.ivanarroyo.pesito.domain.model.AlertDirection
import com.ivanarroyo.pesito.domain.model.RateAlert
import com.ivanarroyo.pesito.presentation.util.MoneyFormat
import com.ivanarroyo.pesito.ui.theme.PesitoTheme
import java.math.BigDecimal

@Composable
fun AlertsRoute(
    onBack: () -> Unit,
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ---- Permiso de notificaciones (Android 13+) ----
    var hasPermission by remember { mutableStateOf(context.hasNotificationPermission()) }
    var wasDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        wasDenied = !granted
    }
    // Si la persona lo activa desde Ajustes y vuelve, lo detectamos al regresar a la pantalla.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = context.hasNotificationPermission()
    }
    val askPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    AlertsScreen(
        state = state,
        draft = viewModel.draft,
        hasNotificationPermission = hasPermission,
        permissionWasDenied = wasDenied,
        onBack = onBack,
        onRequestPermission = askPermission,
        onOpenSettings = { context.openNotificationSettings() },
        onNewAlert = {
            // Pedimos el permiso justo cuando se entiende para qué es: al crear una alerta.
            if (!hasPermission && !wasDenied) askPermission()
            viewModel.onNewAlert()
        },
        onDraftDirectionChange = viewModel::onDraftDirectionChange,
        onDraftTargetChange = viewModel::onDraftTargetChange,
        onDismissDraft = viewModel::onDismissDraft,
        onSaveDraft = viewModel::onSaveDraft,
        onToggleAlert = viewModel::onToggleAlert,
        onDeleteAlert = viewModel::onDeleteAlert,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    state: AlertsUiState,
    draft: AlertDraft?,
    hasNotificationPermission: Boolean,
    permissionWasDenied: Boolean,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onNewAlert: () -> Unit,
    onDraftDirectionChange: (AlertDirection) -> Unit,
    onDraftTargetChange: (String) -> Unit,
    onDismissDraft: () -> Unit,
    onSaveDraft: () -> Unit,
    onToggleAlert: (id: Long, active: Boolean) -> Unit,
    onDeleteAlert: (id: Long) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.alerts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back_cd))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewAlert,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.alert_new)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!hasNotificationPermission) {
                item(key = "permission") {
                    PermissionBanner(
                        wasDenied = permissionWasDenied,
                        onRequestPermission = onRequestPermission,
                        onOpenSettings = onOpenSettings,
                    )
                }
            }

            state.currentRate?.let { rate ->
                item(key = "today") {
                    Text(
                        text = stringResource(R.string.alerts_today, state.from, MoneyFormat.format(rate), state.to),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            when {
                state.isLoading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.alerts.isEmpty() -> item(key = "empty") {
                    Text(
                        text = stringResource(R.string.alerts_empty, state.from),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    )
                }

                else -> items(state.alerts, key = { it.alert.id }) { item ->
                    AlertCard(
                        item = item,
                        onToggle = { active -> onToggleAlert(item.alert.id, active) },
                        onDelete = { onDeleteAlert(item.alert.id) },
                    )
                }
            }
        }
    }

    draft?.let {
        NewAlertDialog(
            from = state.from,
            to = state.to,
            currentRate = state.currentRate,
            draft = it,
            onDirectionChange = onDraftDirectionChange,
            onTargetChange = onDraftTargetChange,
            onDismiss = onDismissDraft,
            onSave = onSaveDraft,
        )
    }
}

@Composable
private fun PermissionBanner(
    wasDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.alerts_permission_text), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.size(8.dp))
            // Si ya dijo que no, Android no vuelve a mostrar el diálogo: toca ir a Ajustes.
            if (wasDenied) {
                Button(onClick = onOpenSettings) { Text(stringResource(R.string.alerts_permission_settings)) }
            } else {
                Button(onClick = onRequestPermission) { Text(stringResource(R.string.alerts_permission_allow)) }
            }
        }
    }
}

@Composable
private fun AlertCard(
    item: AlertItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val alert = item.alert
    Card(shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${alert.from} → ${alert.to}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        if (alert.direction == AlertDirection.BELOW) R.string.alert_rule_below else R.string.alert_rule_above,
                        MoneyFormat.format(alert.target),
                        alert.to,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = when {
                        !alert.isActive -> stringResource(R.string.alert_inactive)
                        item.currentRate != null -> stringResource(R.string.alert_today_short, MoneyFormat.format(item.currentRate))
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = alert.isActive, onCheckedChange = onToggle)
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.alert_delete_cd))
            }
        }
    }
}

@Composable
private fun NewAlertDialog(
    from: String,
    to: String,
    currentRate: BigDecimal?,
    draft: AlertDraft,
    onDirectionChange: (AlertDirection) -> Unit,
    onTargetChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.alert_dialog_title, from, to)) },
        text = {
            Column {
                Text(stringResource(R.string.alert_dialog_question, from))
                Spacer(Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = draft.direction == AlertDirection.BELOW,
                        onClick = { onDirectionChange(AlertDirection.BELOW) },
                        label = { Text(stringResource(R.string.alert_direction_below)) },
                    )
                    FilterChip(
                        selected = draft.direction == AlertDirection.ABOVE,
                        onClick = { onDirectionChange(AlertDirection.ABOVE) },
                        label = { Text(stringResource(R.string.alert_direction_above)) },
                    )
                }
                OutlinedTextField(
                    value = draft.targetText,
                    onValueChange = onTargetChange,
                    label = { Text(stringResource(R.string.alert_target_label)) },
                    placeholder = { currentRate?.let { Text(MoneyFormat.format(it)) } },
                    suffix = { Text(to) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                currentRate?.let {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = stringResource(R.string.alert_today_short, MoneyFormat.format(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = draft.isValid) { Text(stringResource(R.string.alert_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.alert_cancel)) }
        },
    )
}

private fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.openNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

// ---------------- Preview ----------------

@Preview(showBackground = true)
@Composable
private fun AlertsPreview() {
    PesitoTheme {
        AlertsScreen(
            state = AlertsUiState(
                from = "USD",
                to = "COP",
                currentRate = BigDecimal("3301.91"),
                isLoading = false,
                alerts = listOf(
                    AlertItem(RateAlert(1, "USD", "COP", AlertDirection.BELOW, BigDecimal("3200")), BigDecimal("3301.91")),
                    AlertItem(RateAlert(2, "EUR", "COP", AlertDirection.ABOVE, BigDecimal("4000"), isActive = false), BigDecimal("3829.40")),
                ),
            ),
            draft = null,
            hasNotificationPermission = false,
            permissionWasDenied = false,
            onBack = {},
            onRequestPermission = {},
            onOpenSettings = {},
            onNewAlert = {},
            onDraftDirectionChange = {},
            onDraftTargetChange = {},
            onDismissDraft = {},
            onSaveDraft = {},
            onToggleAlert = { _, _ -> },
            onDeleteAlert = {},
        )
    }
}
