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
import android.util.Log
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import java.math.BigInteger

object SettingsLinkUtil {
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
            "%019d".format(record.seed),
            "%06d".format((record.totalScore * 100).toInt())
        ).joinToString("").toBigInteger().toByteArray()

        val encoded = Base64.encodeToString(settingsStringByteArray, Base64.URL_SAFE or Base64.NO_WRAP)
        return encoded
    }

    fun deserialize(encoded: String): GameRecord {
        val byteArray = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP)
        val rawString = BigInteger(byteArray).toString()
        val bigIntString = when (rawString.length) {
            in 28..29 -> rawString.padStart(29, '0') // without totalScore
            in 34..35 -> rawString.padStart(35, '0') // with totalScore
            else -> rawString
        }

        val hasTotalScore = bigIntString.length == 35

        return GameRecord(
            id = 0,
            isModeEnabled = bigIntString[0] == '1',
            modeLimit = bigIntString[1].digitToInt(),
            additionRangeStart = bigIntString[2].digitToInt(),
            additionRangeEnd = bigIntString[3].digitToInt(),
            subtractionRangeStart = bigIntString[4].digitToInt(),
            subtractionRangeEnd = bigIntString[5].digitToInt(),
            multiplicationRangeStart = bigIntString[6].digitToInt(),
            multiplicationRangeEnd = bigIntString[7].digitToInt(),
            divisionRangeStart = bigIntString[8].digitToInt(),
            divisionRangeEnd = bigIntString[9].digitToInt(),
            totalAnswers = 0,
            correctAnswers = 0,
            activeTime = 0L,
            totalTime = 0L,
            activeScore = 0f,
            seed = bigIntString.substring(10, 29).toLong(),
            totalScore = if (hasTotalScore) bigIntString.substring(29, 35).toInt() / 100f else -1f,
            taskResults = emptyList()
        )
    }

    fun isValidQueryParameter(settingsString: String): Boolean {
        try {
            Log.d("SettingsLinkUtil", "Input: $settingsString")
            val byteArray = Base64.decode(settingsString, Base64.URL_SAFE or Base64.NO_WRAP)
            val rawString = BigInteger(byteArray).toString()
            val bigIntString = when (rawString.length) {
                in 28..29 -> rawString.padStart(29, '0')
                in 34..35 -> rawString.padStart(35, '0')
                else -> return false
            }
            Log.d("SettingsLinkUtil", "bigIntString: $bigIntString")

            val isModeEnabled = bigIntString.substring(0, 1)
            Log.d("SettingsLinkUtil", "isModeEnabled: $isModeEnabled")
            if (isModeEnabled != "0" && isModeEnabled != "1") return false

            val modeLimit = bigIntString.substring(1, 2).toInt()
            Log.d("SettingsLinkUtil", "modeLimit: $modeLimit")
            if (modeLimit !in 1..5) return false

            val addStart = bigIntString.substring(2, 3).toInt()
            val addEnd = bigIntString.substring(3, 4).toInt()
            Log.d("SettingsLinkUtil", "addStart: $addStart, addEnd: $addEnd")
            if (addStart !in 0..9 || addEnd !in 0..9 || addStart > addEnd) return false

            val subStart = bigIntString.substring(4, 5).toInt()
            val subEnd = bigIntString.substring(5, 6).toInt()
            Log.d("SettingsLinkUtil", "subStart: $subStart, subEnd: $subEnd")
            if (subStart !in 0..9 || subEnd !in 0..9 || subStart > subEnd) return false

            val mulStart = bigIntString.substring(6, 7).toInt()
            val mulEnd = bigIntString.substring(7, 8).toInt()
            Log.d("SettingsLinkUtil", "mulStart: $mulStart, mulEnd: $mulEnd")
            if (mulStart !in 0..9 || mulEnd !in 0..9 || mulStart > mulEnd) return false

            val divStart = bigIntString.substring(8, 9).toInt()
            val divEnd = bigIntString.substring(9, 10).toInt()
            Log.d("SettingsLinkUtil", "divStart: $divStart, divEnd: $divEnd")
            if (divStart !in 0..9 || divEnd !in 0..9 || divStart > divEnd) return false

            val seed = bigIntString.substring(10, 29)
            Log.d("SettingsLinkUtil", "seed: $seed")
            if (seed.toLongOrNull() == null) return false

            if (bigIntString.length == 35) {
                val totalScoreRaw = bigIntString.substring(29, 35).toInt()
                val totalScore = totalScoreRaw / 100f
                Log.d("SettingsLinkUtil", "totalScoreRaw: $totalScoreRaw, totalScore: $totalScore")
                if (totalScore < 0f || totalScore > 9999.99f) return false
            }

            return true
        } catch (e: Exception) {
            Log.e("SettingsLinkUtil", "Exception: ${e.message}", e)
            return false
        }
    }
}
