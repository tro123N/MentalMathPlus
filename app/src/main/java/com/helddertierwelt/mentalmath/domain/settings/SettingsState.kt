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

package com.helddertierwelt.mentalmath.domain.settings

import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.domain.settings.enums.FormMode
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import com.helddertierwelt.mentalmath.domain.settings.enums.InputDirectionMode
import com.helddertierwelt.mentalmath.domain.settings.enums.InputMode
import com.helddertierwelt.mentalmath.domain.settings.enums.NotificationMode
import com.helddertierwelt.mentalmath.domain.settings.enums.SoundMode
import com.helddertierwelt.mentalmath.domain.settings.enums.ThemeMode
import com.helddertierwelt.mentalmath.domain.settings.enums.VibrationMode

data class SettingsState(
    val gameMode: GameMode = GameMode.PROBLEMS,
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
    val notificationMode: NotificationMode = NotificationMode.OFF,
    val soundMode: SoundMode = SoundMode.SOUND_OFF,
    val vibrationMode: VibrationMode = VibrationMode.VIBRATION_OFF,
    val inputMode: InputMode = InputMode.PHONE_KEYPAD,
    val inputDirectionMode: InputDirectionMode = InputDirectionMode.LEFT_TO_RIGHT,
    val formMode: FormMode = FormMode.HORIZONTAL
) {
    fun toNewGameRecord(seed: Long) = GameRecord(
        gameMode = gameMode,
        modeLimit = limit.toInt(),
        additionRangeStart = if (isAdditionEnabled) additionRange.first.toInt() else 0,
        additionRangeEnd = if (isAdditionEnabled) additionRange.second.toInt() else 0,
        subtractionRangeStart = if (isSubtractionEnabled) subtractionRange.first.toInt() else 0,
        subtractionRangeEnd = if (isSubtractionEnabled) subtractionRange.second.toInt() else 0,
        multiplicationRangeStart = if (isMultiplicationEnabled) multiplicationRange.first.toInt() else 0,
        multiplicationRangeEnd = if (isMultiplicationEnabled) multiplicationRange.second.toInt() else 0,
        divisionRangeStart = if (isDivisionEnabled) divisionRange.first.toInt() else 0,
        divisionRangeEnd = if (isDivisionEnabled) divisionRange.second.toInt() else 0,
        activeScore = SettingsLinkUtil.NO_SCORE,
        seed = seed
    )
}