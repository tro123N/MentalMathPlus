/*
Mental Math - Android app for practicing mental arithmetic
Copyright (C) 2025-2026 HeldDerTierwelt

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see https://www.gnu.org/licenses/gpl-3.0.md.
*/

package com.helddertierwelt.mentalmath.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.graphics.Color
import com.helddertierwelt.mentalmath.domain.climb.model.Palette
import com.helddertierwelt.mentalmath.domain.settings.enums.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3C3F41),
    secondary = Color(0xFF9B9B9B),
    tertiary = Color(0xFF5A5A5A),

    background = Color(0xFF1E1E1E),
    surface = Color(0xFF2A2A2A),

    onPrimary = Color(0xFFEDEDED),
    onSecondary = Color(0xFF1E1E1E),
    onTertiary = Color(0xFFEDEDED),

    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFFFFFF),
    secondary = Color(0xFF9B9B9B),
    tertiary = Color(0xFF5A5A5A),

    background = Color(0xFFEEEEEE),
    surface = Color(0xFFFFFFFF),

    onPrimary = Color(0xFF3C3F41),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFF3C3F41),

    onBackground = Color(0xFF3C3F41),
    onSurface = Color(0xFF3C3F41),
)

val green = Color(0xFF39A241)
val red = Color(0xFFD34747)
val yellow = Color(0xFFFBC02D)
val blue = Color(0xFF1976D2)

// Mental Math Plus: Farbthemen. Karten (primary), Hintergrund, Schrift und eine Akzentfarbe je Thema.
private data class PaletteColors(
    val card: Color,
    val background: Color,
    val surface: Color,
    val text: Color,
    val accent: Color,
)

private fun paletteColors(palette: Palette, dark: Boolean): PaletteColors? = when (palette) {
    Palette.STANDARD -> null
    Palette.OZEAN -> if (dark) {
        PaletteColors(Color(0xFF1B2A38), Color(0xFF0F1923), Color(0xFF223344), Color(0xFFE3EEF7), Color(0xFF4FA3F0))
    } else {
        PaletteColors(Color(0xFFFFFFFF), Color(0xFFE2EBF3), Color(0xFFFFFFFF), Color(0xFF22344A), Color(0xFF1565C0))
    }

    Palette.WALD -> if (dark) {
        PaletteColors(Color(0xFF1D2A21), Color(0xFF111914), Color(0xFF243328), Color(0xFFE3EFE6), Color(0xFF3DB8A8))
    } else {
        PaletteColors(Color(0xFFFFFFFF), Color(0xFFE3EDE5), Color(0xFFFFFFFF), Color(0xFF243A2B), Color(0xFF00796B))
    }

    Palette.ABENDROT -> if (dark) {
        PaletteColors(Color(0xFF32241D), Color(0xFF1B1310), Color(0xFF3A2A22), Color(0xFFF4E8E1), Color(0xFFFF8A50))
    } else {
        PaletteColors(Color(0xFFFFFFFF), Color(0xFFF3E8E0), Color(0xFFFFFFFF), Color(0xFF45312A), Color(0xFFD84315))
    }

    Palette.LAVENDEL -> if (dark) {
        PaletteColors(Color(0xFF29233A), Color(0xFF16121F), Color(0xFF302A42), Color(0xFFEDE7F6), Color(0xFFB39DDB))
    } else {
        PaletteColors(Color(0xFFFFFFFF), Color(0xFFECE6F3), Color(0xFFFFFFFF), Color(0xFF392E4C), Color(0xFF7E57C2))
    }

    Palette.SCHWARZ -> if (dark) {
        PaletteColors(Color(0xFF161616), Color(0xFF000000), Color(0xFF1C1C1C), Color(0xFFEDEDED), Color(0xFF2E8CE6))
    } else {
        null
    }
}

private fun colorSchemeFor(palette: Palette, dark: Boolean) = paletteColors(palette, dark)?.let { c ->
    val base = if (dark) DarkColorScheme else LightColorScheme
    base.copy(
        primary = c.card,
        background = c.background,
        surface = c.surface,
        onPrimary = c.text,
        onTertiary = c.text,
        onBackground = c.text,
        onSurface = c.text,
    )
} ?: if (dark) DarkColorScheme else LightColorScheme

/** Vorschaufarben eines Farbthemas: Hintergrund, Karte und Akzent. */
fun palettePreview(palette: Palette, dark: Boolean): Triple<Color, Color, Color> {
    val scheme = colorSchemeFor(palette, dark)
    return Triple(scheme.background, scheme.primary, paletteColors(palette, dark)?.accent ?: blue)
}

/** Akzentfarbe des Farbthemas (Knöpfe, Markierungen im Kletterweg). */
val LocalAccent = staticCompositionLocalOf { blue }

/** Akzentfarbe des aktuellen Farbthemas. */
val accent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAccent.current

/** Ist gerade der dunkle Modus aktiv? */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun MentalMathTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: Palette = Palette.STANDARD,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = colorSchemeFor(palette, darkTheme)
    val accentColor = paletteColors(palette, darkTheme)?.accent ?: blue
    val screenHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val dimens = remember(screenHeight) { Dimens.from(screenHeight) }

    CompositionLocalProvider(
        LocalDimens provides dimens,
        LocalAccent provides accentColor,
        LocalDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object MentalMathTheme {
    val dimens: Dimens
        @Composable
        @ReadOnlyComposable
        get() = LocalDimens.current

    /** Akzentfarbe des Farbthemas. */
    val accent: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAccent.current
}