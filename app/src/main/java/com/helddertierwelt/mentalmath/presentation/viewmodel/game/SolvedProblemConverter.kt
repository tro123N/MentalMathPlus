package com.helddertierwelt.mentalmath.presentation.viewmodel.game

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class SolvedProblemConverter {
    @TypeConverter
    fun fromSolvedProblemList(value: List<SolvedProblem>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toSolvedProblemList(value: String): List<SolvedProblem> {
        return Json.Default.decodeFromString(value)
    }
}