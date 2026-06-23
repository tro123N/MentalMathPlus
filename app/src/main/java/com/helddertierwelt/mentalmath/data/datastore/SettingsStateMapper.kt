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
import com.helddertierwelt.mentalmath.SettingsStateProto.NotificationMode.DAILY
import com.helddertierwelt.mentalmath.SettingsStateProto.NotificationMode.OFF
import com.helddertierwelt.mentalmath.SettingsStateProto.NotificationMode.THREEDAILY
import com.helddertierwelt.mentalmath.SettingsStateProto.NotificationMode.WEEKLY
import com.helddertierwelt.mentalmath.SettingsStateProto.ThemeMode.DARK
import com.helddertierwelt.mentalmath.SettingsStateProto.ThemeMode.LIGHT
import com.helddertierwelt.mentalmath.SettingsStateProto.ThemeMode.SYSTEM
import com.helddertierwelt.mentalmath.domain.settings.NotificationMode
import com.helddertierwelt.mentalmath.domain.settings.SettingsState
import com.helddertierwelt.mentalmath.domain.settings.ThemeMode

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
        .setIsNumpadDefault(isNumpadDefault)
        .setIsRightToLeftDefault(isRightToLeftDefault)
        .setIsSoundEnabled(isSoundEnabled)
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
        isNumpadDefault = isNumpadDefault,
        isRightToLeftDefault = isRightToLeftDefault,
        isSoundEnabled = isSoundEnabled,
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