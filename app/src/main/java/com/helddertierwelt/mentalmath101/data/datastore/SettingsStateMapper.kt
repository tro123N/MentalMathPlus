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

package com.helddertierwelt.mentalmath101.data.datastore

import com.helddertierwelt.mentalmath101.SettingsStateProto
import com.helddertierwelt.mentalmath101.SettingsStateProto.NotificationMode.DAILY
import com.helddertierwelt.mentalmath101.SettingsStateProto.NotificationMode.OFF
import com.helddertierwelt.mentalmath101.SettingsStateProto.NotificationMode.THREEDAILY
import com.helddertierwelt.mentalmath101.SettingsStateProto.NotificationMode.WEEKLY
import com.helddertierwelt.mentalmath101.SettingsStateProto.ThemeMode.DARK
import com.helddertierwelt.mentalmath101.SettingsStateProto.ThemeMode.LIGHT
import com.helddertierwelt.mentalmath101.SettingsStateProto.ThemeMode.SYSTEM
import com.helddertierwelt.mentalmath101.presentation.component.settings.menu.NotificationMode
import com.helddertierwelt.mentalmath101.presentation.theme.ThemeMode
import com.helddertierwelt.mentalmath101.presentation.viewmodel.settings.SettingsState

fun SettingsState.toProto(): SettingsStateProto {
    return SettingsStateProto.newBuilder()
        .setIsModeEnabled(isModeEnabled)
        .setLimit(limit)
        .setIsPlusEnabled(isAdditionEnabled)
        .setIsMinusEnabled(isSubtractionEnabled)
        .setIsMultiplyEnabled(isMultiplicationEnabled)
        .setIsDivideEnabled(isDivisionEnabled)
        .setIsActiveScoreDefault(isActiveScoreDefault)
        .setIsNumpadDefault(isNumpadDefault)
        .setIsRightToLeft(isRightToLeft)
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
        isAdditionEnabled = isPlusEnabled,
        isSubtractionEnabled = isMinusEnabled,
        isMultiplicationEnabled = isMultiplyEnabled,
        isDivisionEnabled = isDivideEnabled,
        isActiveScoreDefault = isActiveScoreDefault,
        isNumpadDefault = isNumpadDefault,
        isRightToLeft = isRightToLeft,
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