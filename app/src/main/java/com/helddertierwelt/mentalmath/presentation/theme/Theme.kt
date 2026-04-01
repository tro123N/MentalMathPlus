/*
Mental Math - Android app for practicing mental arithmetic
Copyright (C) 2025 HeldDerTierwelt

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
import androidx.compose.ui.graphics.Color

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

@Composable
fun MentalMathTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}