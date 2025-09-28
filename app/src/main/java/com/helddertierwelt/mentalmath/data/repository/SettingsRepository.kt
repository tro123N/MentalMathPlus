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

package com.helddertierwelt.mentalmath.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.helddertierwelt.mentalmath.SettingsStateProto
import com.helddertierwelt.mentalmath.data.datastore.SettingsStateSerializer
import com.helddertierwelt.mentalmath.data.datastore.toProto
import com.helddertierwelt.mentalmath.data.datastore.toSettingsState
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<SettingsStateProto> by dataStore(
    fileName = "settings.pb",
    serializer = SettingsStateSerializer
)

class SettingsRepository(
    private val context: Context
) {
    val settingsFlow: Flow<SettingsState> = context.settingsDataStore.data
        .map { proto -> proto.toSettingsState() }

    suspend fun saveSettings(settings: SettingsState) {
        context.settingsDataStore.updateData { currentProto ->
            settings.toProto()
        }
    }
}