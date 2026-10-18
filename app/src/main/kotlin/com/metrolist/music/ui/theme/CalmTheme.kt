/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.metrolist.music.R

// Night-sky navy with soft periwinkle and sea-glass accents. Containers stay close to the
// background so bars, sheets and cards read as one quiet surface.
val CalmDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFF9DB8FF),
        onPrimary = Color(0xFF0A1430),
        primaryContainer = Color(0xFF263F78),
        onPrimaryContainer = Color(0xFFDCE5FF),
        inversePrimary = Color(0xFF3A5AA8),
        secondary = Color(0xFFB2C6E6),
        onSecondary = Color(0xFF13203A),
        secondaryContainer = Color(0xFF223556),
        onSecondaryContainer = Color(0xFFD7E3F8),
        tertiary = Color(0xFF9FD9C9),
        onTertiary = Color(0xFF0D2B26),
        tertiaryContainer = Color(0xFF1F4A44),
        onTertiaryContainer = Color(0xFFC6F2E5),
        background = Color(0xFF0B1530),
        onBackground = Color(0xFFEDF1FC),
        surface = Color(0xFF0B1530),
        onSurface = Color(0xFFEDF1FC),
        surfaceVariant = Color(0xFF1B2850),
        onSurfaceVariant = Color(0xFFA9B4D0),
        surfaceTint = Color(0xFF9DB8FF),
        inverseSurface = Color(0xFFE3E8F6),
        inverseOnSurface = Color(0xFF16203A),
        outline = Color(0xFF55628A),
        outlineVariant = Color(0xFF2A375E),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF26345E),
        surfaceDim = Color(0xFF0B1530),
        surfaceContainerLowest = Color(0xFF070F25),
        surfaceContainerLow = Color(0xFF0E1936),
        surfaceContainer = Color(0xFF0B1530),
        surfaceContainerHigh = Color(0xFF16234A),
        surfaceContainerHighest = Color(0xFF1E2C56),
    )

// Daylight counterpart: pale sky with ink-blue text.
val CalmLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF3354A3),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDAE3FF),
        onPrimaryContainer = Color(0xFF0E1E4A),
        inversePrimary = Color(0xFF9DB8FF),
        secondary = Color(0xFF55627E),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDCE4F7),
        onSecondaryContainer = Color(0xFF132036),
        tertiary = Color(0xFF2F6B60),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFC6F0E4),
        onTertiaryContainer = Color(0xFF07201B),
        background = Color(0xFFF2F5FC),
        onBackground = Color(0xFF111A33),
        surface = Color(0xFFF2F5FC),
        onSurface = Color(0xFF111A33),
        surfaceVariant = Color(0xFFDFE5F3),
        onSurfaceVariant = Color(0xFF4A5675),
        surfaceTint = Color(0xFF3354A3),
        inverseSurface = Color(0xFF242E48),
        inverseOnSurface = Color(0xFFEEF1FA),
        outline = Color(0xFF7A86A6),
        outlineVariant = Color(0xFFC6CEE2),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFF8FAFF),
        surfaceDim = Color(0xFFD8DEEC),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF6F8FD),
        surfaceContainer = Color(0xFFF2F5FC),
        surfaceContainerHigh = Color(0xFFE6EBF7),
        surfaceContainerHighest = Color(0xFFDEE4F3),
    )

@OptIn(ExperimentalTextApi::class)
private fun figtree(weight: FontWeight) =
    Font(
        resId = R.font.figtree,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

val Figtree =
    FontFamily(
        figtree(FontWeight.Light),
        figtree(FontWeight.Normal),
        figtree(FontWeight.Medium),
        figtree(FontWeight.SemiBold),
        figtree(FontWeight.Bold),
    )

private fun TextStyle.calm(weight: FontWeight? = null) = copy(fontFamily = Figtree, fontWeight = weight ?: fontWeight)

// Light display weights and friendly medium titles, in the spirit of Calm.
fun calmTypography(base: Typography = Typography()): Typography =
    Typography(
        displayLarge = base.displayLarge.calm(FontWeight.Light),
        displayMedium = base.displayMedium.calm(FontWeight.Light),
        displaySmall = base.displaySmall.calm(FontWeight.Light),
        headlineLarge = base.headlineLarge.calm(FontWeight.Light),
        headlineMedium = base.headlineMedium.calm(FontWeight.Light),
        headlineSmall = base.headlineSmall.calm(FontWeight.Normal),
        titleLarge = base.titleLarge.calm(FontWeight.Medium),
        titleMedium = base.titleMedium.calm(FontWeight.Medium),
        titleSmall = base.titleSmall.calm(FontWeight.Medium),
        bodyLarge = base.bodyLarge.calm(),
        bodyMedium = base.bodyMedium.calm(),
        bodySmall = base.bodySmall.calm(),
        labelLarge = base.labelLarge.calm(FontWeight.Medium),
        labelMedium = base.labelMedium.calm(FontWeight.Medium),
        labelSmall = base.labelSmall.calm(FontWeight.Medium),
    )
