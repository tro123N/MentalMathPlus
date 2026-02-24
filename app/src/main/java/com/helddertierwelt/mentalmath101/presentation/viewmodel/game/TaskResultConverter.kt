package com.helddertierwelt.mentalmath101.presentation.viewmodel.game

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class TaskResultConverter {
    @TypeConverter
    fun fromTaskResultList(value: List<TaskResult>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toTaskResultList(value: String): List<TaskResult> {
        return Json.Default.decodeFromString(value)
    }
}