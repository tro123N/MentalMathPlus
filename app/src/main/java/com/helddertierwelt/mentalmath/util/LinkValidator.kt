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
import java.math.BigInteger

object LinkValidator {
    fun isValidSettingsString(settingsString: String): Boolean {
        return try {
            val byteArray = Base64.decode(settingsString, Base64.URL_SAFE or Base64.NO_WRAP)
            val bigIntString = BigInteger(byteArray).toString().padStart(35, '0')
            val isModeEnabled = bigIntString.substring(0, 1)
            if (isModeEnabled != "0" && isModeEnabled != "1") return false
            val modeLimit = bigIntString.substring(1, 2).toInt()
            if (modeLimit !in 1..5) return false
            val addStart = bigIntString.substring(2, 3).toInt()
            val addEnd = bigIntString.substring(3, 4).toInt()
            if (addStart !in 0..9 || addEnd !in 0..9 || addStart > addEnd) return false
            val subStart = bigIntString.substring(4, 5).toInt()
            val subEnd = bigIntString.substring(5, 6).toInt()
            if (subStart !in 0..9 || subEnd !in 0..9 || subStart > subEnd) return false
            val mulStart = bigIntString.substring(6, 7).toInt()
            val mulEnd = bigIntString.substring(7, 8).toInt()
            if (mulStart !in 0..9 || mulEnd !in 0..9 || mulStart > mulEnd) return false
            val divStart = bigIntString.substring(8, 9).toInt()
            val divEnd = bigIntString.substring(9, 10).toInt()
            if (divStart !in 0..9 || divEnd !in 0..9 || divStart > divEnd) return false
            val totalScoreRaw = bigIntString.substring(10, 16).toInt()
            val totalScore = totalScoreRaw / 100f
            if (totalScore < 0f || totalScore > 9999.99f) return false
            val seed = bigIntString.substring(17, 35)
            seed.toLongOrNull() != null
        } catch (_: Exception) {
            false
        }
    }
}
