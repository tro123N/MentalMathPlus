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

import android.util.Base64
import android.util.Log
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import java.math.BigInteger
import java.util.Locale

object SettingsLinkUtil {

    const val NO_SCORE = -1f
    private const val LINK_PREFIX = "https://mental-math.codeberg.page/app?s="

    fun createLink(record: GameRecord): String {
        return LINK_PREFIX + serialize(record)
    }

    fun serialize(record: GameRecord): String {
        val settingsStringByteArray = buildList {
            add(
                when (record.gameMode) {
                    GameMode.TIMED -> "0"
                    GameMode.PROBLEMS -> "1"
                    GameMode.SURVIVAL -> "2"
                }
            )
            add(record.modeLimit.toString())
            add(record.additionRangeStart.toString())
            add(record.additionRangeEnd.toString())
            add(record.subtractionRangeStart.toString())
            add(record.subtractionRangeEnd.toString())
            add(record.multiplicationRangeStart.toString())
            add(record.multiplicationRangeEnd.toString())
            add(record.divisionRangeStart.toString())
            add(record.divisionRangeEnd.toString())
            add("%019d".format(Locale.ENGLISH, record.seed))
            if (record.activeScore >= 0f) {
                add("%06d".format(Locale.ENGLISH, (record.activeScore * 100).toInt()))
            }
        }.joinToString("").toBigInteger().toByteArray()

        val encoded = Base64.encodeToString(settingsStringByteArray, Base64.URL_SAFE or Base64.NO_WRAP)
        return encoded
    }

    fun deserialize(encoded: String): GameRecord {
        val byteArray = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP)
        val rawString = BigInteger(byteArray).toString()
        val bigIntString = when (rawString.length) {
            in 28..29 -> rawString.padStart(29, '0') // without score
            in 34..35 -> rawString.padStart(35, '0') // with score
            else -> rawString
        }

        val hasScore = bigIntString.length == 35

        return GameRecord(
            id = 0,
            gameMode = when (bigIntString[0]) {
                '0' -> GameMode.TIMED
                '1' -> GameMode.PROBLEMS
                '2' -> GameMode.SURVIVAL
                else -> GameMode.PROBLEMS
            },
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
            activeScore = if (hasScore) bigIntString.substring(29, 35).toInt() / 100f else NO_SCORE,
            seed = bigIntString.substring(10, 29).toLong(),
            totalScore = 0f,
            solvedProblems = emptyList()
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

            val gameMode = bigIntString.take(1)
            Log.d("SettingsLinkUtil", "gameMode: $gameMode")
            if (gameMode !in setOf("0", "1", "2")) return false

            val modeLimit = bigIntString[1].digitToInt()
            Log.d("SettingsLinkUtil", "modeLimit: $modeLimit")
            if (modeLimit !in 1..5) return false

            val addStart = bigIntString[2].digitToInt()
            val addEnd = bigIntString[3].digitToInt()
            Log.d("SettingsLinkUtil", "addStart: $addStart, addEnd: $addEnd")
            if (addStart !in 0..9 || addEnd !in 0..9 || addStart > addEnd) return false

            val subStart = bigIntString[4].digitToInt()
            val subEnd = bigIntString[5].digitToInt()
            Log.d("SettingsLinkUtil", "subStart: $subStart, subEnd: $subEnd")
            if (subStart !in 0..9 || subEnd !in 0..9 || subStart > subEnd) return false

            val mulStart = bigIntString[6].digitToInt()
            val mulEnd = bigIntString[7].digitToInt()
            Log.d("SettingsLinkUtil", "mulStart: $mulStart, mulEnd: $mulEnd")
            if (mulStart !in 0..9 || mulEnd !in 0..9 || mulStart > mulEnd) return false

            val divStart = bigIntString[8].digitToInt()
            val divEnd = bigIntString[9].digitToInt()
            Log.d("SettingsLinkUtil", "divStart: $divStart, divEnd: $divEnd")
            if (divStart !in 0..9 || divEnd !in 0..9 || divStart > divEnd) return false

            if (addStart == 0 && subStart == 0 && mulStart == 0 && divStart == 0) return false

            val seed = bigIntString.substring(10, 29)
            Log.d("SettingsLinkUtil", "seed: $seed")
            if (seed.toLongOrNull() == null) return false

            if (bigIntString.length == 35) {
                val activeScoreRaw = bigIntString.substring(29, 35).toInt()
                val activeScore = activeScoreRaw / 100f
                Log.d("SettingsLinkUtil", "activeScoreRaw: $activeScoreRaw, activeScore: $activeScore")
                if (activeScore !in 0f..9999.99f) return false
            }

            return true
        } catch (e: Exception) {
            Log.e("SettingsLinkUtil", "Exception: ${e.message}", e)
            return false
        }
    }
}