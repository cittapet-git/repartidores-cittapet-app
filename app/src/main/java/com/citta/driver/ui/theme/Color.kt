package com.citta.driver.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Città driver palette, aligned with the Cittapet Home reference. Light only for this pass — there is no
 * dark colour set yet.
 */
val CittaBackground = Color(0xFFFFFFFF)
val CittaSurface = Color(0xFFFFFFFF)
val CittaPanelSurface = Color(0xFFF3F3F3)

val CittaRed = Color(0xFFD64545)
val CittaRedPressed = Color(0xFFBC3636)
val CittaRedContainer = Color(0xFFF9E1E1)

val CittaPrimary = CittaRed
val CittaPrimaryPressed = CittaRedPressed
val CittaOnPrimary = Color(0xFFFFFFFF)

/** Secondary red for soft fills / graphic details: citta-red at 10% opacity. */
val CittaPrimarySoft = CittaRed.copy(alpha = 0.1f)

/** Delivered / completed status. */
val CittaGreen = Color(0xFF3FA34D)

/** The floating bottom navigation pill. */
val CittaNavBar = Color(0xFF1B1B1B)
val CittaOnNavBar = Color(0xFFFFFFFF)

val CittaTextPrimary = Color(0xFF1D1D1F)
val CittaTextSecondary = Color(0xFF8A8A8E)

val CittaError = CittaRed
