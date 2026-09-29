package com.helddertierwelt.mentalmath.domain.settings.enums

import com.helddertierwelt.mentalmath.R

enum class GameMode(
    val icon: Int
) {
    TIMED(R.drawable.hourglass_bottom_24dp),
    PROBLEMS(R.drawable.tag_24dp),
    SURVIVAL(R.drawable.heart_24dp);

    fun next() = entries[(ordinal + 1) % entries.size]
}