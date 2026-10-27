/*
Mental Math Plus - Kletterweg zum Kopfrechnen
Copyright (C) 2026 tro123N
Based on Mental Math, Copyright (C) 2025-2026 HeldDerTierwelt

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

package com.helddertierwelt.mentalmath.domain.climb.engine

import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.World

/** Text der täglichen Erinnerung, passend zum aktuellen Stand. */
object Reminder {

    data class Message(val title: String, val text: String)

    /** Soll heute erinnert werden? */
    fun shouldNotify(progress: ClimbProgress, today: Long, onlyIfIdle: Boolean): Boolean =
        !onlyIfIdle || progress.lastActiveDay != today

    fun message(worlds: List<World>, progress: ClimbProgress, now: Long, today: Long): Message {
        val streak = Progression.visibleStreak(progress, today)
        val atRisk = streak >= 2 && progress.lastActiveDay == today - 1
        val refresh = Progression.needsRefresh(worlds, progress)
        val due = Progression.dueSkills(worlds, progress, now)
        val next = Progression.nextSkill(worlds, progress)
        val title = if (atRisk) "Deine Serie: $streak Tage" else "Zeit fürs Kopfrechnen"
        val main = when {
            refresh.isNotEmpty() -> "„${refresh.first().title}“ ist eingerostet – eine kurze Auffrischung bringt es zurück."
            due.size == 1 -> "Ein Level wartet auf die Wiederholung: „${due.first().title}“."
            due.size > 1 -> "${due.size} Levels warten auf die Wiederholung."
            next != null -> "Weiter geht's mit „${next.title}“."
            else -> "Halte deine Tricks frisch – ein paar Minuten reichen."
        }
        val text = if (atRisk) "$main Übe heute, damit deine Serie hält." else main
        return Message(title, text)
    }
}
