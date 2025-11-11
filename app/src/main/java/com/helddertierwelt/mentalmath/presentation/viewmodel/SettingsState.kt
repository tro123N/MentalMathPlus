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

package com.helddertierwelt.mentalmath.presentation.viewmodel

import android.os.Parcelable
import com.helddertierwelt.mentalmath.presentation.theme.ThemeMode
import kotlinx.parcelize.Parcelize

@Parcelize
data class SettingsState(
    val isModeEnabled: Boolean = true,
    val limit: Float = 2f,

    val additionRange: Pair<Float, Float> = 2f to 4f,
    val subtractionRange: Pair<Float, Float> = 2f to 4f,
    val multiplicationRange: Pair<Float, Float> = 2f to 4f,
    val divisionRange: Pair<Float, Float> = 2f to 4f,

    val isAdditionEnabled: Boolean = false,
    val isSubtractionEnabled: Boolean = false,
    val isMultiplicationEnabled: Boolean = false,
    val isDivisionEnabled: Boolean = false,

    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isNumpadDefault: Boolean = false,
    val isActiveScoreDefault: Boolean = false,
) : Parcelable
