package com.citta.driver.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Città driver palette, aligned with the Cittapet Home reference. There is a light and a dark set;
 * [CittaDriverTheme] provides the active one through [LocalCittaColors]. The dark values mirror the
 * dashboard's `dark-mode.css`: dark grey surfaces (never pure black), off-white text, and the brand
 * red/green lightened so they stay readable on dark. Solid red fills that carry white text use a
 * deeper red, as the dashboard does.
 *
 * Screens keep reading the `Citta*` names below; each one resolves against the active palette.
 */
@Immutable
class CittaColors(
    val background: Color,
    val surface: Color,
    val panelSurface: Color,
    val red: Color,
    val redPressed: Color,
    val redContainer: Color,
    /** Red for text and thin accents; the solid [red] fill can be too dark to read as small text. */
    val primaryText: Color,
    val primarySoft: Color,
    val onPrimary: Color,
    val green: Color,
    val navBar: Color,
    val onNavBar: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val error: Color,
)

val LightCittaColors = CittaColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    panelSurface = Color(0xFFF3F3F3),
    red = Color(0xFFD64545),
    redPressed = Color(0xFFBC3636),
    redContainer = Color(0xFFF9E1E1),
    primaryText = Color(0xFFD64545),
    primarySoft = Color(0xFFD64545).copy(alpha = 0.1f),
    onPrimary = Color(0xFFFFFFFF),
    green = Color(0xFF3FA34D),
    navBar = Color(0xFF1B1B1B),
    onNavBar = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1D1D1F),
    textSecondary = Color(0xFF8A8A8E),
    error = Color(0xFFD64545),
)

val DarkCittaColors = CittaColors(
    background = Color(0xFF121214),
    surface = Color(0xFF1C1C1F),
    panelSurface = Color(0xFF232327),
    red = Color(0xFFD33A40),
    redPressed = Color(0xFFB9333A),
    redContainer = Color(0xFF3A2224),
    primaryText = Color(0xFFF0565C),
    primarySoft = Color(0xFFF0565C).copy(alpha = 0.16f),
    onPrimary = Color(0xFFFFFFFF),
    green = Color(0xFF2FBF71),
    navBar = Color(0xFF2A2A2F),
    onNavBar = Color(0xFFECECEE),
    textPrimary = Color(0xFFECECEE),
    textSecondary = Color(0xFF9898A0),
    error = Color(0xFFF0565C),
)

val LocalCittaColors = staticCompositionLocalOf { LightCittaColors }

val CittaBackground: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.background
val CittaSurface: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.surface
val CittaPanelSurface: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.panelSurface

val CittaRed: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.red
val CittaRedPressed: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.redPressed
val CittaRedContainer: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.redContainer

val CittaPrimary: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.red
val CittaPrimaryPressed: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.redPressed
val CittaPrimaryText: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.primaryText
val CittaOnPrimary: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.onPrimary

/** Secondary red for soft fills / graphic details. */
val CittaPrimarySoft: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.primarySoft

/** Delivered / completed status. */
val CittaGreen: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.green

/** The floating bottom navigation pill. */
val CittaNavBar: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.navBar
val CittaOnNavBar: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.onNavBar

val CittaTextPrimary: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.textPrimary
val CittaTextSecondary: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.textSecondary

val CittaError: Color @Composable @ReadOnlyComposable get() = LocalCittaColors.current.error
