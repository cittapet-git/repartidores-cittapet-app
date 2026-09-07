package com.citta.driver.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.citta.driver.R

/**
 * Driver type scale aligned to the web app, which uses Outfit. The four weights are bundled
 * as `res/font/outfit_*.ttf` (SIL OFL) so the app renders identically offline and on first
 * launch — no Google Fonts downloadable-fonts round trip.
 */
val CittaSans = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

val CittaMono = FontFamily.Monospace

val CittaTypography = Typography(
    displayLarge = TextStyle(fontFamily = CittaSans),
    displayMedium = TextStyle(fontFamily = CittaSans),
    displaySmall = TextStyle(fontFamily = CittaSans),
    headlineLarge = TextStyle(fontFamily = CittaSans),
    headlineMedium = TextStyle(fontFamily = CittaSans, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = CittaSans),
    titleLarge = TextStyle(fontFamily = CittaSans, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = CittaSans, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = CittaSans),
    bodyLarge = TextStyle(fontFamily = CittaSans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = CittaSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = CittaSans),
    labelLarge = TextStyle(fontFamily = CittaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = CittaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp),
    labelMedium = TextStyle(fontFamily = CittaSans),
)
