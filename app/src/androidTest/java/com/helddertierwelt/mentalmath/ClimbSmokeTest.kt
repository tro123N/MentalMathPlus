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

package com.helddertierwelt.mentalmath

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.helddertierwelt.mentalmath.domain.climb.curriculum.Curriculum
import com.helddertierwelt.mentalmath.domain.climb.engine.Phase
import com.helddertierwelt.mentalmath.domain.climb.engine.Progression
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.DayStat
import com.helddertierwelt.mentalmath.domain.climb.model.TestLog
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.SkillProgress
import com.helddertierwelt.mentalmath.domain.climb.model.World
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Klickt den Kletterweg auf einem (virtuellen) Handy durch und speichert dabei Bildschirmfotos.
 * Läuft in der GitHub-Action „smoke“; die Bilder landen auf dem Branch „ci-smoke“.
 *
 * Instrumentierungs-Argument „shots“: Welche Levels ausführlich fotografiert werden,
 * z. B. „all“ oder „w7,w5.dec21“. Alle anderen werden nur kurz geöffnet (Absturztest).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ClimbSmokeTest {

    @get:Rule(order = 0)
    val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 1)
    val watcher = object : TestWatcher() {
        override fun failed(e: Throwable?, description: Description?) {
            try {
                shot("FEHLER_${description?.methodName ?: "test"}")
            } catch (t: Throwable) {
                Log.e(TAG, "Bildschirmfoto nach Fehler nicht möglich", t)
            }
        }
    }

    private val shotFilter: List<String> =
        (InstrumentationRegistry.getArguments().getString("shots") ?: "all")
            .split(',', ' ', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun wantShots(skillId: String): Boolean =
        shotFilter.any { it == "all" || it == skillId || skillId.startsWith("$it.") }

    private val vm: ClimbViewModel by lazy {
        val activity = compose.activity
        compose.runOnUiThread { ViewModelProvider(activity)[ClimbViewModel::class.java] }
    }

    // --- Hilfen -------------------------------------------------------------------------------

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(300)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        val scaled = Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
        counter++
        val file = File(screenDir(), String.format(Locale.ROOT, "%03d_%s.png", counter, name))
        FileOutputStream(file).use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Log.i(TAG, "Bildschirmfoto ${file.name}")
    }

    private fun waitForTag(tag: String, timeout: Long = 10_000) {
        compose.waitUntil(timeout) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun waitForHome() {
        waitForTag("climbList", 30_000)
        compose.waitUntil(10_000) { vm.loaded.value }
        compose.waitForIdle()
    }

    private fun waitForSession() {
        compose.waitUntil(10_000) { vm.session.value != null }
        compose.waitForIdle()
    }

    private fun back() {
        val activity = compose.activity
        compose.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    /** Fotografiert eine lange Seite in mehreren Abschnitten. */
    private fun shootScrolling(tag: String, name: String, maxShots: Int = 6) {
        shot("${name}_1")
        for (i in 2..maxShots) {
            val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
            val range = node.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) ?: return
            if (range.value() >= range.maxValue() - 1f) return
            val height = node.size.height.toFloat()
            compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, height * 0.8f) }
            compose.waitForIdle()
            shot("${name}_$i")
        }
    }

    private fun plain(v: BigDecimal): String = v.stripTrailingZeros().toPlainString().replace('.', ',')

    /** So tippt man die richtige Antwort ein (null bei Auswahlaufgaben). */
    private fun typed(e: Expected): String? = when (e) {
        is Expected.Exact -> plain(e.value)
        is Expected.Approx -> plain(e.value.setScale(maxOf(0, e.tolerance.scale()), RoundingMode.HALF_UP))
        is Expected.Remainder -> if (e.remainder == 0L) "${e.quotient}" else "${e.quotient}R${e.remainder}"
        is Expected.Frac -> "${e.numerator}/${e.denominator}"
        is Expected.Choice -> null
        is Expected.Digits -> e.digits
    }

    /** Eine sicher falsche Eingabe. */
    private fun wrongTyped(e: Expected): String? = when (e) {
        is Expected.Exact -> plain(e.value + BigDecimal.ONE)
        is Expected.Approx -> plain(e.value.setScale(0, RoundingMode.DOWN) + BigDecimal.TEN)
        is Expected.Remainder -> "${e.quotient + 1}"
        is Expected.Frac -> "${e.numerator + 1}/${e.denominator}"
        is Expected.Choice -> null
        is Expected.Digits -> if (e.digits == "1") "2" else "1"
    }

    private fun key(c: Char) {
        val tag = when (c) {
            in '0'..'9' -> "key$c"
            ',', 'R', '/' -> "keyExtra"
            else -> error("Unbekannte Taste $c")
        }
        compose.onNodeWithTag(tag).performClick()
    }

    /** Beantwortet die aktuelle Eingabe über den Ziffernblock bzw. die Auswahlknöpfe. */
    private fun answer(wrong: Boolean = false) {
        val s = vm.session.value ?: error("Keine Übung aktiv")
        check(s.phase == Phase.ANSWERING) { "Falsche Phase ${s.phase}" }
        val expected = s.expected ?: error("Keine erwartete Antwort")
        val problem = s.problem ?: error("Keine Aufgabe")
        val before: Any?
        if (expected is Expected.Choice) {
            val index = if (wrong) (expected.index + 1) % problem.choices.size else expected.index
            before = s
            compose.onNodeWithText(problem.choices[index]).performClick()
        } else {
            val keys = (if (wrong) wrongTyped(expected) else typed(expected)) ?: error("Keine Eingabe für $expected")
            keys.forEach { key(it) }
            compose.waitForIdle()
            val now = vm.session.value
            check(now?.input == keys) { "Eingabe kam nicht an: erwartet $keys, angezeigt ${now?.input} (${problem.prompt.text})" }
            before = now
            compose.onNodeWithTag("keyEnter").performClick()
        }
        compose.waitUntil(5_000) { vm.session.value !== before }
        compose.waitForIdle()
    }

    private fun waitWhileFlash() {
        compose.waitUntil(5_000) { vm.session.value.let { it == null || it.phase != Phase.FLASH } }
        compose.waitForIdle()
    }

    private fun pressWeiter() {
        compose.onNodeWithText("Weiter").performClick()
        compose.waitForIdle()
    }

    /**
     * Löst die laufende Übung bis zum Ende.
     * [wrongAt]: Nummern der Eingaben (ab 0), die absichtlich falsch sind.
     * [shotAt]: Nummern der Eingaben, vor denen fotografiert wird.
     */
    private fun solveSession(name: String? = null, wrongAt: Set<Int> = emptySet(), shotAt: Set<Int> = emptySet(), hintAt: Int? = null) {
        var answered = 0
        var revealShot = false
        repeat(600) {
            val s = vm.session.value ?: return
            when (s.phase) {
                Phase.FINISHED -> return
                Phase.FLASH -> waitWhileFlash()
                Phase.REVEAL -> {
                    if (name != null && !revealShot) {
                        shot("${name}_aufgedeckt")
                        revealShot = true
                    }
                    pressWeiter()
                }

                Phase.ANSWERING -> {
                    if (name != null && answered == hintAt) {
                        compose.onNodeWithText("Tipp").performClick()
                        compose.waitForIdle()
                        shot("${name}_tipp")
                    }
                    if (name != null && answered in shotAt) shot("${name}_$answered")
                    answer(wrong = answered in wrongAt)
                    if (name != null && answered in wrongAt && vm.session.value?.phase == Phase.ANSWERING) {
                        shot("${name}_falsch_$answered")
                    }
                    answered++
                }
            }
        }
        error("Die Übung endet nicht")
    }

    /** Löst nur die aktuelle Aufgabe (alle Schritte). */
    private fun solveCurrentProblem() {
        val start = vm.session.value?.index ?: return
        repeat(80) {
            val s = vm.session.value ?: return
            if (s.index != start || s.phase == Phase.FINISHED) return
            when (s.phase) {
                Phase.ANSWERING -> answer()
                Phase.FLASH -> waitWhileFlash()
                Phase.REVEAL -> pressWeiter()
                Phase.FINISHED -> return
            }
        }
    }

    private fun quitSession() {
        compose.runOnUiThread { vm.quitSession() }
        compose.waitForIdle()
        back()
    }

    private fun waitForResult() {
        compose.waitUntil(10_000) { vm.summary.value != null }
        waitForTag("resultColumn")
    }

    private fun clickButton(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
        compose.waitForIdle()
    }

    /** Vom Kletterweg zum klassischen Modus: erst „Freies Training“, dann fährt „Klassisch“ heraus. */
    private fun openClassic() {
        if (compose.onAllNodesWithTag("freeColumn").fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithTag("tab_free").performClick()
            waitForTag("freeColumn")
        }
        compose.onNodeWithTag("tab_classic").performClick()
    }

    private fun openSkill(world: World, skill: Skill) {
        val list = compose.onNodeWithTag("climbList")
        list.performScrollToNode(hasTestTag("world_${world.id}"))
        if (compose.onAllNodesWithTag("skill_${skill.id}").fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithTag("worldHeader_${world.id}").performClick()
            compose.waitForIdle()
        }
        list.performScrollToNode(hasTestTag("skill_${skill.id}"))
        compose.onNodeWithTag("skill_${skill.id}").performClick()
        waitForTag("levelColumn")
    }

    // --- Tests --------------------------------------------------------------------------------

    /** Neuer Nutzer: erstes Level komplett durchspielen (lernen, geführt, üben, Prüfung). */
    @Test
    fun a_ersterAufstieg() {
        waitForHome()
        compose.runOnUiThread { vm.importProgress(ClimbProgress()) }
        compose.waitUntil(5_000) { vm.progress.value.skills.isEmpty() && vm.session.value == null }
        compose.waitForIdle()
        shot("a_start")
        val first = Curriculum.worlds.first().skills.first()

        compose.onNodeWithText("Weiterklettern").performClick()
        waitForTag("levelColumn")
        shot("a_level")

        clickButton("1. Methode lernen")
        waitForTag("learnColumn")
        shootScrolling("learnColumn", "a_lernen")
        clickButton("Jetzt geführt üben")
        waitForSession()
        solveSession(name = "a_gefuehrt", wrongAt = setOf(3, 4), shotAt = setOf(0, 1), hintAt = 2)
        waitForResult()
        shootScrolling("resultColumn", "a_ergebnis_gefuehrt", maxShots = 3)

        clickButton("Zum Level")
        waitForTag("levelColumn")
        clickButton("3. Frei üben")
        waitForSession()
        solveSession(name = "a_ueben", wrongAt = setOf(1), shotAt = setOf(0))
        waitForResult()
        shootScrolling("resultColumn", "a_ergebnis_ueben", maxShots = 3)

        clickButton("Zum Level")
        waitForTag("levelColumn")
        clickButton("4. Prüfung")
        waitForSession()
        solveSession(name = "a_pruefung", shotAt = setOf(0, 5))
        waitForResult()
        shootScrolling("resultColumn", "a_ergebnis_pruefung", maxShots = 4)
        assertTrue("Prüfung nicht bestanden", vm.progress.value.of(first.id).stars >= 1)

        clickButton("Zum Kletterweg")
        waitForHome()
        shot("a_nach_pruefung")

        openClassic()
        waitForTag("classicIntro")
        compose.onAllNodesWithContentDescription("menu").onFirst().performClick()
        compose.waitForIdle()
        Thread.sleep(500)
        shot("a_klassisch_menue")
        back()
        compose.waitForIdle()
        back()
        waitForHome()
    }

    /** Fortgeschrittener Stand: alle Welten offen, jedes Level einmal öffnen und anspielen. */
    @Test
    fun b_alleWelten() {
        waitForHome()
        val now = System.currentTimeMillis()
        val today = vm.today()
        val skills = Curriculum.skills
        val seeded = skills.mapIndexed { i, s ->
            s.id to SkillProgress(
                stars = i % 3 + 1,
                bestAvgMs = 3_000L + i * 150L,
                testsTaken = 2,
                testsPassed = 2,
                practiced = 30,
                practicedCorrect = 27,
                lastPracticedAt = now - Progression.DAY_MS,
                intervalDays = 7,
                nextReviewAt = if (i % 7 == 3) now - 3_600_000L else now + 3 * Progression.DAY_MS,
                rusty = s.id == "w3.add2",
            )
        }.toMap()
        compose.runOnUiThread {
            vm.importProgress(ClimbProgress(skills = seeded, streakDays = 4, lastActiveDay = today, totalSolved = 1234, totalCorrect = 1180))
        }
        compose.waitUntil(5_000) { vm.progress.value.skills.size == skills.size }
        compose.waitForIdle()
        shot("b_start")

        // Jede Welt aufgeklappt fotografieren (Liste: Übersicht, dann eine Karte pro Welt).
        Curriculum.worlds.forEachIndexed { index, world ->
            val list = compose.onNodeWithTag("climbList")
            list.performScrollToIndex(index + 2)
            compose.waitForIdle()
            if (compose.onAllNodesWithTag("skill_${world.skills.first().id}").fetchSemanticsNodes().isEmpty()) {
                compose.onNodeWithTag("worldHeader_${world.id}").performClick()
                compose.waitForIdle()
            }
            list.performScrollToIndex(index + 2)
            shot("b_welt_${world.number}")
        }

        // Jedes Level öffnen, Erklärung ansehen, eine Aufgabe geführt lösen.
        Curriculum.worlds.forEach { world ->
            world.skills.forEach { skill ->
                val shots = wantShots(skill.id)
                val name = "b_" + skill.id.replace('.', '_')
                Log.i(TAG, "Level ${skill.id}")
                openSkill(world, skill)
                if (shots) shot("${name}_level")

                clickButton("1. Methode lernen")
                waitForTag("learnColumn")
                if (shots) shootScrolling("learnColumn", "${name}_lernen")
                back()
                waitForTag("levelColumn")

                clickButton("2. Geführt üben")
                waitForSession()
                if (shots) shot("${name}_gefuehrt")
                solveCurrentProblem()
                if (shots) shot("${name}_gefuehrt_2")
                quitSession()
                waitForTag("levelColumn")

                if (shots) {
                    clickButton("3. Frei üben")
                    waitForSession()
                    shot("${name}_ueben")
                    answer(wrong = true)
                    if (vm.session.value?.phase == Phase.FLASH) waitWhileFlash()
                    shot("${name}_ueben_falsch")
                    quitSession()
                    waitForTag("levelColumn")
                }
                back()
                waitForHome()
            }
        }

        // Eingerostetes Level auffrischen.
        compose.onNodeWithTag("climbList").performScrollToIndex(0)
        compose.waitForIdle()
        compose.onAllNodesWithText("Auffrischen:", substring = true).fetchSemanticsNodes().let { nodes ->
            if (nodes.isNotEmpty()) {
                compose.onNodeWithText("Auffrischen:", substring = true).performClick()
                waitForTag("levelColumn")
                shot("b_auffrischen_level")
                clickButton("Auffrischen")
                waitForSession()
                solveSession(name = "b_auffrischen", shotAt = setOf(0))
                waitForResult()
                shootScrolling("resultColumn", "b_auffrischen_ergebnis", maxShots = 2)
                clickButton("Zum Kletterweg")
                waitForHome()
            }
        }

        // Fällige Wiederholung.
        compose.onNodeWithTag("climbList").performScrollToIndex(0)
        compose.waitForIdle()
        compose.onNodeWithText("Wiederholen", substring = true).performClick()
        waitForSession()
        solveSession(name = "b_wiederholung", shotAt = setOf(0))
        waitForResult()
        shootScrolling("resultColumn", "b_wiederholung_ergebnis", maxShots = 2)
        clickButton("Zum Kletterweg")
        waitForHome()
        shot("b_ende")
    }

    /** Freies Training: Aufgabenarten wählen, Überleben-Modus spielen, Statistik ansehen. */
    @Test
    fun c_freiesTraining() {
        waitForHome()
        compose.onNodeWithTag("tab_free").performClick()
        waitForTag("freeColumn")
        compose.onNodeWithTag("adaptiveSwitch").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.plusSettings.value.adaptive }
        listOf("PERCENT", "WORD", "ALGEBRA", "GEOMETRY", "NUMBER_THEORY", "FOUR_OPS").forEach { topic ->
            compose.onNodeWithTag("topic_$topic").performScrollTo().performClick()
        }
        compose.onNodeWithTag("mode_UEBERLEBEN").performScrollTo().performClick()
        compose.waitForIdle()
        shootScrolling("freeColumn", "c_einstellungen", maxShots = 4)
        clickButton("Los geht's")
        waitForSession()
        var answered = 0
        repeat(200) {
            val s = vm.session.value ?: return@repeat
            if (s.phase == Phase.FINISHED) return@repeat
            when (s.phase) {
                Phase.ANSWERING -> {
                    if (answered == 1) {
                        compose.onNodeWithTag("trickButton").performClick()
                        compose.waitForIdle()
                        shot("c_trick_tipp")
                    }
                    if (answered < 8) shot("c_aufgabe_$answered")
                    answer(wrong = answered >= 8)
                    answered++
                }
                Phase.FLASH -> waitWhileFlash()
                Phase.REVEAL -> pressWeiter()
                Phase.FINISHED -> Unit
            }
        }
        waitForResult()
        shootScrolling("resultColumn", "c_ergebnis", maxShots = 3)
        clickButton("Einstellungen")
        waitForTag("freeColumn")
        compose.onNodeWithText("Statistik").performScrollTo()
        compose.waitForIdle()
        shot("c_statistik")
        compose.onNodeWithTag("adaptiveSwitch").performScrollTo().performClick()
        compose.waitUntil(5_000) { !vm.plusSettings.value.adaptive }
        back()
        waitForHome()
    }

    /** Handbuch: öffnen, einen Eintrag aufklappen, suchen, zum Level springen. */
    @Test
    fun d_handbuch() {
        waitForHome()
        compose.onNodeWithTag("mini_handbook").performScrollTo().performClick()
        waitForTag("handbookList")
        shot("d_handbuch")
        val list = compose.onNodeWithTag("handbookList")
        Curriculum.worlds.forEach { world ->
            list.performScrollToNode(hasTestTag("hb_${world.skills.first().id}"))
        }
        val skill = Curriculum.worlds.first { it.number == 3 }.skills.first()
        list.performScrollToNode(hasTestTag("hb_${skill.id}"))
        compose.onNodeWithTag("hb_${skill.id}").performClick()
        compose.waitForIdle()
        shootScrolling("handbookList", "d_eintrag", maxShots = 3)
        list.performScrollToIndex(0)
        compose.onNodeWithTag("handbookSearch").performTextInput("Wochentag")
        compose.waitForIdle()
        shot("d_suche")
        val hits = Curriculum.worlds.flatMap { it.skills }.filter { "wochentag" in (it.title + it.method + it.summary).lowercase() }
        assertTrue("Suche findet nichts", hits.isEmpty() || compose.onAllNodesWithTag("hb_${hits.first().id}").fetchSemanticsNodes().isNotEmpty())
        back()
        waitForHome()
    }

    /** Die drei Bereiche: Kletterweg, freies Training, klassischer Modus. */
    @Test
    fun e_bereiche() {
        waitForHome()
        compose.onNodeWithTag("climbList").performScrollToIndex(0)
        shot("e_kletterweg")
        compose.onNodeWithTag("tab_free").performClick()
        waitForTag("freeColumn")
        shot("e_freies_training")
        openClassic()
        waitForTag("classicIntro")
        shot("e_klassisch")
        compose.onNodeWithTag("tab_climb").performClick()
        waitForHome()
        openClassic()
        waitForTag("classicIntro")
        back()
        waitForHome()
    }

    /** Statistik mit Verlauf, Stärken und Schwächen; Verlauf eines Levels. */
    @Test
    fun f_statistik() {
        waitForHome()
        val now = System.currentTimeMillis()
        val today = vm.today()
        val skills = Curriculum.skills
        val seeded = skills.take(24).mapIndexed { i, s ->
            val g = s.goals
            val speed = g.star3 + (g.star1 - g.star3) * (i % 5) / 3.5
            val recent = List(12) { j -> if ((i + j) % 6 == 0 && i % 4 == 1) -2_000L else (speed * 1000).toLong() + j * 37L }
            val tests = List(5) { t ->
                TestLog(
                    at = now - (5 - t) * 3 * Progression.DAY_MS,
                    avgMs = ((g.star1 * 1.15 - t * (g.star1 - g.star3) / 4.5) * 1000).toLong(),
                    correct = 10,
                    total = 10,
                    stars = minOf(3, t / 2 + 1),
                )
            }
            val days = (0 until 8).map { k -> DayStat(today - 20 + k * 3, 6 + (i + k) % 9, 5 + (i + k) % 8, 40_000L) }
            s.id to SkillProgress(
                stars = 1 + i % 3,
                testsTaken = 5,
                testsPassed = 5,
                bestAvgMs = tests.minOf { it.avgMs },
                practiced = 60,
                practicedCorrect = 54,
                lastPracticedAt = now - Progression.DAY_MS,
                intervalDays = 3,
                nextReviewAt = now + Progression.DAY_MS,
                rusty = i == 7,
                recent = recent,
                tests = tests,
                days = days,
            )
        }.toMap()
        val allDays = (0 until 30).mapNotNull { k ->
            val d = today - 29 + k
            val solved = 8 + (k * 7) % 31
            if (k % 5 == 3) null else DayStat(d, solved, minOf(solved, 6 + (k * 5) % 25), 60_000L)
        }
        compose.runOnUiThread {
            vm.importProgress(
                ClimbProgress(
                    skills = seeded,
                    streakDays = 6,
                    bestStreak = 11,
                    lastActiveDay = today,
                    totalSolved = 2345,
                    totalCorrect = 2101,
                    days = allDays,
                ),
            )
        }
        compose.waitUntil(5_000) { vm.progress.value.skills.size == seeded.size }
        compose.waitForIdle()
        compose.onNodeWithTag("action_stats").performClick()
        waitForTag("statsColumn")
        shootScrolling("statsColumn", "f_statistik", maxShots = 6)
        back()
        waitForHome()
        val world = Curriculum.worlds.first()
        openSkill(world, world.skills.first())
        compose.onNodeWithTag("levelHistory").performScrollTo()
        compose.waitForIdle()
        shot("f_level_verlauf")
        back()
        waitForHome()
    }

    /** Einstufungstest für Welt 1 von Anfang an. */
    @Test
    fun g_einstufung() {
        waitForHome()
        compose.runOnUiThread { vm.importProgress(ClimbProgress()) }
        compose.waitUntil(5_000) { vm.progress.value.skills.isEmpty() && vm.session.value == null }
        compose.waitForIdle()
        compose.onNodeWithTag("mini_placement").performScrollTo().performClick()
        waitForTag("placementColumn")
        shootScrolling("placementColumn", "g_einstufung", maxShots = 3)
        compose.onNodeWithTag("place_w1").performScrollTo().performClick()
        waitForSession()
        solveSession(name = "g_test", shotAt = setOf(0, 7))
        waitForResult()
        val placement = vm.summary.value?.placement
        Log.i(TAG, "Einstufung: $placement")
        assertTrue("Keine Auswertung des Einstufungstests", placement != null)
        shootScrolling("resultColumn", "g_ergebnis", maxShots = 2)
        clickButton("Zum Kletterweg")
        waitForHome()
        shot("g_nach_einstufung")
    }

    /** Einstellungen: Farbthema, dunkler Modus, Erinnerung, Sicherung. */
    @Test
    fun h_einstellungen() {
        waitForHome()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(
                instrumentation.targetContext.packageName,
                android.Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        compose.onNodeWithTag("action_settings").performClick()
        waitForTag("settingsColumn")
        shot("h_einstellungen")
        compose.onNodeWithTag("palette_WALD").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.plusSettings.value.palette == "WALD" }
        compose.onNodeWithTag("theme_DARK").performScrollTo().performClick()
        compose.waitForIdle()
        Thread.sleep(500)
        compose.onNodeWithTag("settingsLook").performScrollTo()
        compose.waitForIdle()
        shot("h_dunkel_wald")
        compose.onNodeWithTag("reminderSwitch").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.plusSettings.value.reminderOn }
        compose.onNodeWithTag("timePlus").performClick()
        compose.waitUntil(5_000) { vm.plusSettings.value.reminderMinute == 15 }
        compose.onNodeWithTag("settingsReminder").performScrollTo()
        compose.waitForIdle()
        shot("h_erinnerung")
        compose.onNodeWithTag("reminderTest").performScrollTo().performClick()
        compose.waitForIdle()

        // Sicherung ohne Dateiauswahl: als Text schreiben, lesen und wieder einspielen.
        val before = vm.progress.value
        val text = vm.backupText()
        val backup = vm.readBackup(text).getOrThrow()
        assertTrue("Sicherung verändert den Fortschritt", backup.progress == before)
        compose.runOnUiThread { vm.importProgress(ClimbProgress()) }
        compose.waitUntil(5_000) { vm.progress.value.skills.isEmpty() }
        compose.runOnUiThread { vm.restoreBackup(backup) }
        compose.waitUntil(5_000) { vm.progress.value == before }
        compose.onNodeWithTag("settingsLook").performScrollTo()
        compose.waitForIdle()
        shootScrolling("settingsColumn", "h_alles", maxShots = 6)

        back()
        waitForHome()
        shot("h_start_dunkel")
        compose.onNodeWithTag("action_stats").performClick()
        waitForTag("statsColumn")
        shot("h_statistik_dunkel")
        back()
        waitForHome()
        openClassic()
        waitForTag("classicIntro")
        shot("h_klassisch_dunkel")
        back()
        waitForHome()

        // Helles Farbthema ansehen.
        compose.onNodeWithTag("action_settings").performClick()
        waitForTag("settingsColumn")
        compose.onNodeWithTag("palette_ABENDROT").performScrollTo().performClick()
        compose.onNodeWithTag("theme_LIGHT").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.plusSettings.value.palette == "ABENDROT" }
        back()
        waitForHome()
        Thread.sleep(500)
        shot("h_start_hell_abendrot")

        // Zurück zum Standard.
        compose.onNodeWithTag("action_settings").performClick()
        waitForTag("settingsColumn")
        compose.onNodeWithTag("palette_STANDARD").performScrollTo().performClick()
        compose.onNodeWithTag("theme_SYSTEM").performScrollTo().performClick()
        compose.onNodeWithTag("reminderSwitch").performScrollTo().performClick()
        compose.waitUntil(5_000) { !vm.plusSettings.value.reminderOn && vm.plusSettings.value.palette == "STANDARD" }
        back()
        waitForHome()
    }

    /** TMSnat-Training: Infos, Kurztest-Simulation mit „Später“, Verlauf, gemischtes Üben mit Trick-Tipp und Lösungsweg. */
    @Test
    fun i_medizinertest() {
        waitForHome()
        compose.runOnUiThread { vm.importProgress(ClimbProgress()) }
        compose.waitUntil(5_000) { vm.progress.value.skills.isEmpty() && vm.session.value == null }
        compose.onNodeWithTag("tab_free").performClick()
        waitForTag("freeColumn")
        shot("i_freies_training")
        compose.onNodeWithTag("apEntry").performScrollTo().performClick()
        waitForTag("apColumn")
        shootScrolling("apColumn", "i_training", maxShots = 6)

        compose.onNodeWithTag("simShort").performScrollTo().performClick()
        waitForSession()
        shot("i_sim_0")
        compose.onNodeWithTag("skipButton").performClick()
        compose.waitForIdle()
        shot("i_sim_spaeter")
        var answered = 0
        repeat(80) {
            val s = vm.session.value ?: return@repeat
            when (s.phase) {
                Phase.ANSWERING -> {
                    if (answered == 3) shot("i_sim_3")
                    answer(wrong = answered % 4 == 1)
                    answered++
                }
                Phase.FLASH -> waitWhileFlash()
                Phase.REVEAL -> pressWeiter()
                Phase.FINISHED -> Unit
            }
        }
        waitForResult()
        assertTrue("Keine gespeicherte Simulation", vm.summary.value?.apRun != null)
        shootScrolling("resultColumn", "i_ergebnis", maxShots = 5)
        clickButton("Zum TMSnat-Training")
        waitForTag("apColumn")
        compose.onNodeWithTag("apHistory").performScrollTo()
        compose.waitForIdle()
        shot("i_verlauf")

        compose.onNodeWithTag("apPracticeButton").performScrollTo().performClick()
        waitForSession()
        compose.onNodeWithTag("trickButton").performClick()
        compose.waitForIdle()
        shot("i_ueben_trick")
        answer(wrong = true)
        compose.waitForIdle()
        // Nach einem Fehler kommt der Lösungsweg, weiter geht es erst mit „Weiter“.
        assertTrue("Kein Lösungsweg nach falscher Antwort", vm.session.value?.phase == Phase.REVEAL)
        shot("i_ueben_falsch")
        val before = vm.session.value?.index
        pressWeiter()
        compose.waitUntil(5_000) { vm.session.value?.let { it.phase == Phase.ANSWERING && it.index != before } == true }
        answer()
        waitWhileFlash()
        shot("i_ueben_weiter")
        compose.runOnUiThread { vm.endFree() }
        waitForResult()
        shot("i_ueben_ergebnis")
        clickButton("Zum Kletterweg")
        waitForHome()
    }

    companion object {
        private const val TAG = "ClimbSmokeTest"
        private var counter = 0

        private fun screenDir(): File =
            File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "screens").apply { mkdirs() }

        @BeforeClass
        @JvmStatic
        fun cleanScreens() {
            screenDir().deleteRecursively()
            screenDir()
        }
    }
}
