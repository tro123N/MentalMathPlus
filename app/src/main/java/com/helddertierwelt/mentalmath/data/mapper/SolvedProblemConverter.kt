package com.helddertierwelt.mentalmath.data.mapper

import androidx.room.TypeConverter
import com.helddertierwelt.mentalmath.domain.game.SolvedProblem
import kotlinx.serialization.json.Json

class SolvedProblemConverter {
    @TypeConverter
    fun fromSolvedProblemList(value: List<SolvedProblem>): String {
        return Json.Default.encodeToString(value)
    }

    @TypeConverter
    fun toSolvedProblemList(value: String): List<SolvedProblem> {
        return Json.Default.decodeFromString(value)
    }
}