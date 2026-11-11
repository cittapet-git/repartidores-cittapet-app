package com.citta.driver.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Città driver theme. `MainActivity` wraps the whole app in [CittaDriverTheme] and passes the
 * driver's choice from Ajustes. Light is the default so previews and the status screen look as
 * before.
 */
private fun CittaColors.toColorScheme(dark: Boolean) =
    if (dark) {
        darkColorScheme(
            primary = red,
            onPrimary = onPrimary,
            primaryContainer = redContainer,
            onPrimaryContainer = primaryText,
            secondary = green,
            onSecondary = onPrimary,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = panelSurface,
            onSurfaceVariant = textSecondary,
            outline = textSecondary,
            error = error,
            onError = onPrimary,
        )
    } else {
        lightColorScheme(
            primary = red,
            onPrimary = onPrimary,
            primaryContainer = redContainer,
            onPrimaryContainer = redPressed,
            secondary = green,
            onSecondary = onPrimary,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surface,
            onSurfaceVariant = textSecondary,
            outline = textSecondary,
            error = error,
            onError = onPrimary,
        )
    }

@Composable
fun CittaDriverTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkCittaColors else LightCittaColors
    CompositionLocalProvider(LocalCittaColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(dark = darkTheme),
            typography = CittaTypography,
            content = content,
        )
    }
}
