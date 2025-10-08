package com.helddertierwelt.mentalmath.util

import com.helddertierwelt.mentalmath.data.entity.GameRecord

object GameStringSerializer {
    fun serialize(record: GameRecord): String {
        return listOf(
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
            record.seed.toString()
        ).joinToString(";")
    }
}
