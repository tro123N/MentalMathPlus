package com.helddertierwelt.mentalmath.util

object LinkValidator {
    fun isValidSettingsString(settingsString: String): Boolean {
        val parts = settingsString.split(";")
        if (parts.size != 11) return false
        if (parts[0] != "0" && parts[0] != "1") return false

        val limit = parts[1].toIntOrNull() ?: return false
        if (limit !in 1..5) return false

        val addStart = parts[2].toIntOrNull() ?: return false
        val addEnd = parts[3].toIntOrNull() ?: return false
        if (addStart !in 0..9 || addEnd !in 0..9 || addStart > addEnd) return false

        val subStart = parts[4].toIntOrNull() ?: return false
        val subEnd = parts[5].toIntOrNull() ?: return false
        if (subStart !in 0..9 || subEnd !in 0..9 || subStart > subEnd) return false

        val mulStart = parts[6].toIntOrNull() ?: return false
        val mulEnd = parts[7].toIntOrNull() ?: return false
        if (mulStart !in 0..9 || mulEnd !in 0..9 || mulStart > mulEnd) return false

        val divStart = parts[8].toIntOrNull() ?: return false
        val divEnd = parts[9].toIntOrNull() ?: return false
        if (divStart !in 0..9 || divEnd !in 0..9 || divStart > divEnd) return false

        if (parts[10].toLongOrNull() == null) return false
        return true
    }
}

