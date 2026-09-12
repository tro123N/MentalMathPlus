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

package com.helddertierwelt.mentalmath.data.datastore

import com.helddertierwelt.mentalmath.SettingsStateProto
import com.helddertierwelt.mentalmath.SettingsStateProto.InputMode.NUMBER_PAD
import com.helddertierwelt.mentalmath.SettingsStateProto.InputMode.PHONE_KEYPAD
import com.helddertierwelt.mentalmath.SettingsStateProto.NotificationMode.DAILY
import com.helddertierwelt.mentalmath.SettingsStateProto.NotificationMode.OFF
import com.helddertierwelt.mentalmath.SettingsStateProto.NotificationMode.THREEDAILY
import com.helddertierwelt.mentalmath.SettingsStateProto.NotificationMode.WEEKLY
import com.helddertierwelt.mentalmath.SettingsStateProto.SoundMode.SOUND_OFF
import com.helddertierwelt.mentalmath.SettingsStateProto.SoundMode.SOUND_ON
import com.helddertierwelt.mentalmath.SettingsStateProto.ThemeMode.DARK
import com.helddertierwelt.mentalmath.SettingsStateProto.ThemeMode.LIGHT
import com.helddertierwelt.mentalmath.SettingsStateProto.ThemeMode.SYSTEM
import com.helddertierwelt.mentalmath.SettingsStateProto.VibrationMode.VIBRATION_OFF
import com.helddertierwelt.mentalmath.SettingsStateProto.VibrationMode.VIBRATION_ON
import com.helddertierwelt.mentalmath.domain.settings.SettingsState
import com.helddertierwelt.mentalmath.domain.settings.enums.InputMode
import com.helddertierwelt.mentalmath.domain.settings.enums.NotificationMode
import com.helddertierwelt.mentalmath.domain.settings.enums.SoundMode
import com.helddertierwelt.mentalmath.domain.settings.enums.ThemeMode
import com.helddertierwelt.mentalmath.domain.settings.enums.VibrationMode

fun SettingsState.toProto(): SettingsStateProto {
    return SettingsStateProto.newBuilder()
        .setIsModeEnabled(isModeEnabled)
        .setLimit(limit)
        .setPlusMin(additionRange.first)
        .setPlusMax(additionRange.second)
        .setMinusMin(subtractionRange.first)
        .setMinusMax(subtractionRange.second)
        .setMultiplyMin(multiplicationRange.first)
        .setMultiplyMax(multiplicationRange.second)
        .setDivideMin(divisionRange.first)
        .setDivideMax(divisionRange.second)
        .setIsPlusEnabled(isAdditionEnabled)
        .setIsMinusEnabled(isSubtractionEnabled)
        .setIsMultiplyEnabled(isMultiplicationEnabled)
        .setIsDivideEnabled(isDivisionEnabled)
        .setInputMode(
            when (inputMode) {
                InputMode.PHONE_KEYPAD -> PHONE_KEYPAD
                InputMode.NUMBER_PAD -> NUMBER_PAD
            }
        )
        .setIsRightToLeftDefault(isRightToLeftDefault)
        .setIsVerticalFormDefault(isVerticalFormDefault)
        .setThemeMode(
            when (themeMode) {
                ThemeMode.SYSTEM -> SYSTEM
                ThemeMode.LIGHT -> LIGHT
                ThemeMode.DARK -> DARK
            }
        )
        .setNotificationMode(
            when (notificationMode) {
                NotificationMode.OFF -> OFF
                NotificationMode.DAILY -> DAILY
                NotificationMode.THREEDAILY -> THREEDAILY
                NotificationMode.WEEKLY -> WEEKLY
            }
        )
        .setSoundMode(
            when (soundMode) {
                SoundMode.SOUND_OFF -> SOUND_OFF
                SoundMode.SOUND_ON -> SOUND_ON
            }
        )
        .setVibrationMode(
            when (vibrationMode) {
                VibrationMode.VIBRATION_OFF -> VIBRATION_OFF
                VibrationMode.VIBRATION_ON -> VIBRATION_ON
            }
        )
        .build()
}

fun SettingsStateProto.toSettingsState(): SettingsState {
    return SettingsState(
        isModeEnabled = isModeEnabled,
        limit = limit,
        additionRange = plusMin to plusMax,
        subtractionRange = minusMin to minusMax,
        multiplicationRange = multiplyMin to multiplyMax,
        divisionRange = divideMin to divideMax,
        isAdditionEnabled = isPlusEnabled,
        isSubtractionEnabled = isMinusEnabled,
        isMultiplicationEnabled = isMultiplyEnabled,
        isDivisionEnabled = isDivideEnabled,
        inputMode = when (inputMode) {
            PHONE_KEYPAD -> InputMode.PHONE_KEYPAD
            NUMBER_PAD -> InputMode.NUMBER_PAD
            SettingsStateProto.InputMode.UNRECOGNIZED -> InputMode.PHONE_KEYPAD
        },
        isRightToLeftDefault = isRightToLeftDefault,
        soundMode = when (soundMode) {
            SOUND_OFF -> SoundMode.SOUND_OFF
            SOUND_ON -> SoundMode.SOUND_ON
            SettingsStateProto.SoundMode.UNRECOGNIZED -> SoundMode.SOUND_OFF
        },
        vibrationMode = when (vibrationMode) {
            VIBRATION_OFF -> VibrationMode.VIBRATION_OFF
            VIBRATION_ON -> VibrationMode.VIBRATION_ON
            SettingsStateProto.VibrationMode.UNRECOGNIZED -> VibrationMode.VIBRATION_OFF
        },
        isVerticalFormDefault = isVerticalFormDefault,
        themeMode = when (themeMode) {
            LIGHT -> ThemeMode.LIGHT
            DARK -> ThemeMode.DARK
            SYSTEM -> ThemeMode.SYSTEM
            SettingsStateProto.ThemeMode.UNRECOGNIZED -> ThemeMode.SYSTEM
        },
        notificationMode = when (notificationMode) {
            OFF -> NotificationMode.OFF
            DAILY -> NotificationMode.DAILY
            THREEDAILY -> NotificationMode.THREEDAILY
            WEEKLY -> NotificationMode.WEEKLY
            SettingsStateProto.NotificationMode.UNRECOGNIZED -> NotificationMode.OFF
        }
    )
}