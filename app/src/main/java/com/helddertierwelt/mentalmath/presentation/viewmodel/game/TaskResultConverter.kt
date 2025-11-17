package com.helddertierwelt.mentalmath.presentation.viewmodel.game

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class TaskResultConverter {
    @TypeConverter
    fun fromTaskResultList(value: List<TaskResult>): String {
        return Json.Default.encodeToString(value)
    }

    @TypeConverter
    fun toTaskResultList(value: String): List<TaskResult> {
        return Json.Default.decodeFromString(value)
    }
}