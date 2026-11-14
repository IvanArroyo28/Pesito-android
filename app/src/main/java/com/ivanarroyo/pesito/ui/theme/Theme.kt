package com.ivanarroyo.pesito.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = Neutral99,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Amber40,
    onSecondary = Neutral99,
    secondaryContainer = Amber90,
    onSecondaryContainer = Amber30,
    error = Red40,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    onSurfaceVariant = NeutralVariant30,
)

private val DarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = Green10,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Amber80,
    onSecondary = Amber30,
    secondaryContainer = Amber30,
    onSecondaryContainer = Amber90,
    error = Red80,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = NeutralVariant80,
)

/**
 * Sin colores dinámicos (Material You) a propósito:
 * así Pesito se ve igual en todos los celulares y conserva su identidad.
 */
@Composable
fun PesitoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
