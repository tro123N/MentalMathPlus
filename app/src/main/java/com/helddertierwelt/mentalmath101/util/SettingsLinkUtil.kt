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

package com.helddertierwelt.mentalmath101.util

import android.util.Base64
import android.util.Log
import com.helddertierwelt.mentalmath101.data.entity.GameRecord
import java.math.BigInteger
import java.util.Locale

object SettingsLinkUtil {
    fun serialize(record: GameRecord): String {
        val settingsStringByteArray = listOf(
            if (record.isModeEnabled) "1" else "0",
            record.modeLimit.toString(),
            if (record.isAdditionEnabled) "1" else "0",
            if (record.isSubtractionEnabled) "1" else "0",
            if (record.isMultiplicationEnabled) "1" else "0",
            if (record.isDivisionEnabled) "1" else "0",
            "%019d".format(Locale.ENGLISH,record.seed),
            "%06d".format(Locale.ENGLISH,(record.totalScore * 100).toInt())
        ).joinToString("").toBigInteger().toByteArray()

        val encoded = Base64.encodeToString(settingsStringByteArray, Base64.URL_SAFE or Base64.NO_WRAP)
        return encoded
    }

    fun deserialize(encoded: String): GameRecord {
        val byteArray = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP)
        val rawString = BigInteger(byteArray).toString()
        val bigIntString = when (rawString.length) {
            in 24..25 -> rawString.padStart(25, '0') // without totalScore
            in 30..31 -> rawString.padStart(31, '0') // with totalScore
            else -> rawString
        }

        val hasTotalScore = bigIntString.length == 31

        return GameRecord(
            id = 0,
            isModeEnabled = bigIntString[0] == '1',
            modeLimit = bigIntString[1].digitToInt(),
            isAdditionEnabled = bigIntString[2] == '1',
            isSubtractionEnabled = bigIntString[3] == '1',
            isMultiplicationEnabled = bigIntString[4] == '1',
            isDivisionEnabled = bigIntString[5] == '1',
            totalAnswers = 0,
            correctAnswers = 0,
            activeTime = 0L,
            totalTime = 0L,
            activeScore = 0f,
            seed = bigIntString.substring(6, 25).toLong(),
            totalScore = if (hasTotalScore) bigIntString.substring(25, 31).toInt() / 100f else -1f,
            taskResults = emptyList()
        )
    }

    fun isValidQueryParameter(settingsString: String): Boolean {
        try {
            Log.d("SettingsLinkUtil", "Input: $settingsString")
            val byteArray = Base64.decode(settingsString, Base64.URL_SAFE or Base64.NO_WRAP)
            val rawString = BigInteger(byteArray).toString()
            val bigIntString = when (rawString.length) {
                in 24..25 -> rawString.padStart(25, '0')
                in 30..31 -> rawString.padStart(31, '0')
                else -> return false
            }
            Log.d("SettingsLinkUtil", "bigIntString: $bigIntString")

            val isModeEnabled = bigIntString.take(1)
            Log.d("SettingsLinkUtil", "isModeEnabled: $isModeEnabled")
            if (isModeEnabled != "0" && isModeEnabled != "1") return false

            val modeLimit = bigIntString[1].digitToInt()
            Log.d("SettingsLinkUtil", "modeLimit: $modeLimit")
            if (modeLimit !in 1..5) return false

            val isAdditionEnabled = bigIntString[2]
            Log.d("SettingsLinkUtil", "isAdditionEnabled: $isAdditionEnabled")
            if (isAdditionEnabled != '0' && isAdditionEnabled != '1') return false

            val isSubtractionEnabled = bigIntString[3]
            Log.d("SettingsLinkUtil", "isSubtractionEnabled: $isSubtractionEnabled")
            if (isSubtractionEnabled != '0' && isSubtractionEnabled != '1') return false

            val isMultiplicationEnabled = bigIntString[4]
            Log.d("SettingsLinkUtil", "isMultiplicationEnabled: $isMultiplicationEnabled")
            if (isMultiplicationEnabled != '0' && isMultiplicationEnabled != '1') return false

            val isDivisionEnabled = bigIntString[5]
            Log.d("SettingsLinkUtil", "isDivisionEnabled: $isDivisionEnabled")
            if (isDivisionEnabled != '0' && isDivisionEnabled != '1') return false


            val seed = bigIntString.substring(6, 25)
            Log.d("SettingsLinkUtil", "seed: $seed")
            if (seed.toLongOrNull() == null) return false

            if (bigIntString.length == 31) {
                val totalScoreRaw = bigIntString.substring(25, 31).toInt()
                val totalScore = totalScoreRaw / 100f
                Log.d("SettingsLinkUtil", "totalScoreRaw: $totalScoreRaw, totalScore: $totalScore")
                if (totalScore !in 0f..9999.99f) return false
            }

            return true
        } catch (e: Exception) {
            Log.e("SettingsLinkUtil", "Exception: ${e.message}", e)
            return false
        }
    }
}
