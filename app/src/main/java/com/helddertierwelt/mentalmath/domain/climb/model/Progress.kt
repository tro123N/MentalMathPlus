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

package com.helddertierwelt.mentalmath.domain.climb.model

import kotlinx.serialization.Serializable

/** Gespeicherter Fortschritt eines Levels. */
@Serializable
data class SkillProgress(
    /** Beste erreichte Sterne in einer Prüfung (0 bis 3). */
    val stars: Int = 0,
    /** Beste Durchschnittszeit pro Aufgabe in einer bestandenen Prüfung (ms), 0 = keine. */
    val bestAvgMs: Long = 0,
    val testsTaken: Int = 0,
    val testsPassed: Int = 0,
    /** Nicht bestandene Prüfungen in Folge. */
    val failStreak: Int = 0,
    /** Geübte Aufgaben insgesamt (Üben, geführt, Wiederholung). */
    val practiced: Int = 0,
    val practicedCorrect: Int = 0,
    val lastPracticedAt: Long = 0,
    /** Aktueller Wiederholungsabstand in Tagen (0 = noch nicht eingeplant). */
    val intervalDays: Int = 0,
    /** Zeitpunkt (ms), ab dem das Level wieder zur Auffrischung fällig ist. */
    val nextReviewAt: Long = 0,
    /** Das Level ist „eingerostet“ und muss aufgefrischt werden. */
    val rusty: Boolean = false,
    /** Ein darauf aufbauendes Level ist mehrfach nicht bestanden worden; dieses Level zuerst auffrischen. */
    val refreshPending: Boolean = false,
    /** Letzte Ergebnisse: positive Zahl = richtig in so vielen ms, negative Zahl = falsch. */
    val recent: List<Long> = emptyList(),
) {
    val mastered: Boolean get() = stars >= 1
    val needsAttention: Boolean get() = rusty || refreshPending
}

/** Gesamter Fortschritt im Kletterweg. */
@Serializable
data class ClimbProgress(
    val version: Int = 1,
    val skills: Map<String, SkillProgress> = emptyMap(),
    /** Tage in Folge mit mindestens einer abgeschlossenen Übung. */
    val streakDays: Int = 0,
    /** Letzter aktiver Tag (Epoch-Tag), -1 = noch nie. */
    val lastActiveDay: Long = -1,
    val totalSolved: Long = 0,
    val totalCorrect: Long = 0,
) {
    fun of(skillId: String): SkillProgress = skills[skillId] ?: SkillProgress()

    fun with(skillId: String, update: (SkillProgress) -> SkillProgress): ClimbProgress =
        copy(skills = skills + (skillId to update(of(skillId))))
}
