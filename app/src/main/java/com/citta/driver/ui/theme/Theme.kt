package com.citta.driver.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Città driver theme. Light only for this pass — the palette is lifted straight from the Home
 * reference. `MainActivity` wraps the whole app in [CittaDriverTheme]; there is no dark scheme
 * yet, so a device in dark mode still gets these light tokens.
 */
private val CittaLightColorScheme = lightColorScheme(
    primary = CittaPrimary,
    onPrimary = CittaOnPrimary,
    primaryContainer = CittaRedContainer,
    onPrimaryContainer = CittaRedPressed,
    secondary = CittaGreen,
    onSecondary = CittaOnPrimary,
    background = CittaBackground,
    onBackground = CittaTextPrimary,
    surface = CittaSurface,
    onSurface = CittaTextPrimary,
    surfaceVariant = CittaSurface,
    onSurfaceVariant = CittaTextSecondary,
    outline = CittaTextSecondary,
    error = CittaError,
    onError = CittaOnPrimary,
)

@Composable
fun CittaDriverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CittaLightColorScheme,
        typography = CittaTypography,
        content = content,
    )
}
