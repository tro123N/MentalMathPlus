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

package com.helddertierwelt.mentalmath.util

import android.util.Base64
import com.helddertierwelt.mentalmath.data.entity.GameRecord

object GameStringSerializer {
    fun serialize(record: GameRecord): String {
        val settingsStringByteArray = listOf(
            if (record.isModeEnabled) "1" else "0",
            record.modeLimit.toString(),
            record.additionRangeStart.toString(),
            record.additionRangeEnd.toString(),
            record.subtractionRangeStart.toString(),
            record.subtractionRangeEnd.toString(),
            record.multiplicationRangeStart.toString(),
            record.multiplicationRangeEnd.toString(),
            record.divisionRangeStart.toString(),
            record.divisionRangeEnd.toString(),
            "%06d".format((record.totalScore * 100).toInt()),
            "%019d".format(record.seed)
        ).joinToString("").toBigInteger().toByteArray()

        val encoded = Base64.encodeToString(settingsStringByteArray, Base64.URL_SAFE or Base64.NO_WRAP)
        return encoded
    }
}
