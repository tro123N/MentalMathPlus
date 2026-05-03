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

import androidx.datastore.core.Serializer
import com.helddertierwelt.mentalmath.SettingsStateProto
import com.helddertierwelt.mentalmath.domain.settings.SettingsState
import java.io.InputStream
import java.io.OutputStream

object SettingsStateSerializer : Serializer<SettingsStateProto> {
    override val defaultValue: SettingsStateProto = SettingsState().toProto()

    override suspend fun readFrom(input: InputStream): SettingsStateProto {
        return SettingsStateProto.parseFrom(input)
    }

    override suspend fun writeTo(t: SettingsStateProto, output: OutputStream) {
        t.writeTo(output)
    }
}