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
            "%06d".format((record.totalScore * 100).toInt()),
            "%019d".format(record.seed)
        ).joinToString("").toBigInteger().toByteArray()

        val encoded = Base64.encodeToString(settingsStringByteArray, Base64.URL_SAFE or Base64.NO_WRAP)
        return encoded
    }

    fun deserialize(encoded: String): GameRecord {
        val byteArray = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP)
        val bigIntString = BigInteger(byteArray).toString().padStart(35, '0')

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
            totalScore = bigIntString.substring(10, 16).toInt() / 100f,
            seed = bigIntString.substring(16, 35).toLong(),
            taskResults = emptyList()
        )
    }

    fun isValidSettingsString(settingsString: String): Boolean {
        try {
            Log.d("SettingsLinkUtil", "Input: $settingsString")
            val byteArray = Base64.decode(settingsString, Base64.URL_SAFE or Base64.NO_WRAP)
            val bigIntString = BigInteger(byteArray).toString().padStart(35, '0')
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

            val totalScoreRaw = bigIntString.substring(10, 16).toInt()
            val totalScore = totalScoreRaw / 100f
            Log.d("SettingsLinkUtil", "totalScoreRaw: $totalScoreRaw, totalScore: $totalScore")
            if (totalScore < 0f || totalScore > 9999.99f) return false

            val seed = bigIntString.substring(16, 35)
            Log.d("SettingsLinkUtil", "seed: $seed")
            val valid = seed.toLongOrNull() != null
            return valid
        } catch (e: Exception) {
            Log.e("SettingsLinkUtil", "Exception: ${e.message}", e)
            return false
        }
    }
}
