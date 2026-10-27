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

package com.helddertierwelt.mentalmath.presentation.climb.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.curriculum.ApCategory
import com.helddertierwelt.mentalmath.domain.climb.engine.Stats
import com.helddertierwelt.mentalmath.domain.climb.model.ApRun
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import com.helddertierwelt.mentalmath.presentation.climb.component.ActionButton
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.CaptionText
import com.helddertierwelt.mentalmath.presentation.climb.component.CardGap
import com.helddertierwelt.mentalmath.presentation.climb.component.CardTitle
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.climb.component.LineChart
import com.helddertierwelt.mentalmath.presentation.climb.component.ShareBar
import com.helddertierwelt.mentalmath.presentation.climb.component.StarRow
import com.helddertierwelt.mentalmath.presentation.climb.component.SwitchRow
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import kotlin.math.roundToInt

private fun runDate(ms: Long): String {
    val c = java.util.Calendar.getInstance()
    c.timeInMillis = ms
    return "${c.get(java.util.Calendar.DAY_OF_MONTH)}.${c.get(java.util.Calendar.MONTH) + 1}."
}

private fun runName(run: ApRun): String = when {
    run.total < 16 -> "Kurztest"
    run.options == 4 -> "HAM-Nat"
    else -> "TMSnat"
}

/** Medizinertest-Training: alles für den Untertest „Arithmetisches Problemlösen“ (TMSnat, HAM-Nat). */
@Composable
fun ApTrainingScreen(navController: NavController) {
    val dimens = MentalMathTheme.dimens
    val owner = LocalActivity.current as ViewModelStoreOwner
    val viewModel: ClimbViewModel = hiltViewModel(owner)
    val progress by viewModel.progress.collectAsState()
    val plus by viewModel.plusSettings.collectAsState()

    fun back() {
        viewModel.playTap()
        if (!navController.popBackStack()) navController.navigate("climb") { launchSingleTop = true }
    }
    BackHandler { back() }

    fun play(started: Boolean) {
        if (started) navController.navigate("climbplay")
    }

    fun openLevel(id: String) {
        viewModel.playTap()
        navController.navigate("level/$id")
    }

    Scaffold(topBar = { ClimbTopBar(title = "Medizinertest-Training", onNavigation = { back() }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("apColumn")
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            ClimbCard(modifier = Modifier.testTag("apInfo")) {
                CardTitle("Arithmetisches Problemlösen")
                BodyText("Textaufgaben, die du ohne Taschenrechner lösen musst – ein Untertest im HAM-Nat und ab 2027 im neuen TMSnat.")
                Spacer(Modifier.height(10.dp))
                InfoRow("TMSnat (ab 2027)", "16 Aufgaben in 25 Minuten, also gut 90 Sekunden pro Aufgabe. Es ist der letzte Untertest des Testtags.")
                InfoRow("HAM-Nat (bis 2026)", "16 Aufgaben, empfohlen sind 30 Minuten; 4 Antworten (A–D).")
                InfoRow("Hilfsmittel", "Kein Taschenrechner. Nebenrechnungen im Testheft sind erlaubt.")
                InfoRow("Inhalte", "Mathe der Mittelstufe in Textaufgaben: Dreisatz, Prozente, Anteile, Einheiten, Tempo, gemeinsame Arbeit, Mischungen, Wachstum, Gleichungen, Kombinatorik, Wahrscheinlichkeit – oft mit unwichtigen Zusatzangaben.")
                CaptionText("Stand: Oktober 2026, nach der offiziellen Übersicht zum TMSnat vom 26.06.2026. Einzelheiten wie die Zahl der Antworten im TMSnat können sich noch ändern.")
            }
            CardGap()

            ClimbCard(modifier = Modifier.testTag("apSimulation")) {
                CardTitle("Testsimulation")
                CaptionText("Wie im echten Test: Die Gesamtzeit läuft, es gibt keine Rückmeldung, und schwere Aufgaben kannst du mit „Später bearbeiten“ ans Ende schieben. Am Ende siehst du alle Lösungswege.")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        text = "TMSnat · 16 Aufgaben · 25 min",
                        subtitle = "5 Antworten (A–E)",
                        icon = R.drawable.trophy_24dp,
                        iconTint = yellow,
                        onClick = { play(viewModel.startApSimulation(1_500, 5)) },
                        modifier = Modifier.testTag("simTms"),
                    )
                    ActionButton(
                        text = "HAM-Nat · 16 Aufgaben · 30 min",
                        subtitle = "4 Antworten (A–D)",
                        icon = R.drawable.trophy_24dp,
                        iconTint = accent,
                        onClick = { play(viewModel.startApSimulation(1_800, 4)) },
                        modifier = Modifier.testTag("simHam"),
                    )
                    ActionButton(
                        text = "Kurztest · 8 Aufgaben · 12½ min",
                        subtitle = "Für zwischendurch, 5 Antworten",
                        icon = R.drawable.timer_24dp,
                        iconTint = accent,
                        onClick = { play(viewModel.startApSimulation(750, 5, 8)) },
                        modifier = Modifier.testTag("simShort"),
                    )
                }
            }
            CardGap()

            ClimbCard(modifier = Modifier.testTag("apPractice")) {
                CardTitle("Üben")
                ActionButton(
                    text = "Gemischt üben",
                    subtitle = "Ohne Ende, mit Rückmeldung, Lösungsweg und Trick-Tipp",
                    icon = R.drawable.play_arrow_24dp,
                    iconTint = green,
                    onClick = { play(viewModel.startApPractice()) },
                    modifier = Modifier.testTag("apPracticeButton"),
                )
                val forms = ApCategory.entries.map { c -> c to Stats.form(viewModel.skill(c.skillId)!!, progress.of(c.skillId)) }
                val weakest = forms.filter { it.second != null }.minByOrNull { it.second!!.score }
                    ?: forms.firstOrNull { progress.of(it.first.skillId).practiced == 0 }?.let { it.first to null }
                if (weakest != null) {
                    Spacer(Modifier.height(8.dp))
                    ActionButton(
                        text = "Gezielt: ${weakest.first.title}",
                        subtitle = weakest.second?.let { "Deine schwächste Kategorie (Sicherheit ${it.score})" } ?: "Noch nicht geübt",
                        icon = R.drawable.terrain_24dp,
                        iconTint = accent,
                        onClick = { openLevel(weakest.first.skillId) },
                    )
                }
                Spacer(Modifier.height(6.dp))
                SwitchRow(
                    title = "Schwierigkeit automatisch anpassen",
                    subtitle = "Beim gemischten Üben: schwerer, wenn es gut läuft, leichter nach Fehlern.",
                    checked = plus.adaptive,
                    onCheckedChange = { viewModel.setAdaptive(it) },
                )
            }
            CardGap()

            ClimbCard(modifier = Modifier.testTag("apCategories")) {
                CardTitle("Kategorien")
                CaptionText("Jede Kategorie ist ein Level mit Erklärung, geführtem Üben und Prüfung. Tippe eine an.")
                Spacer(Modifier.height(6.dp))
                val ids = ApCategory.entries.map { it.skillId } + "w13.mixed"
                ids.forEach { id ->
                    val skill = viewModel.skill(id) ?: return@forEach
                    val sp = progress.of(id)
                    val form = Stats.form(skill, sp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.components.cornerRadius))
                            .clickable { openLevel(id) }
                            .testTag("apCat_$id")
                            .padding(vertical = 7.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(skill.title, fontSize = dimens.fonts.sideSheetEntry, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.weight(1f))
                            StarRow(stars = sp.stars, size = dimens.icons.problemStatus * 0.6f)
                        }
                        if (form != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ShareBar(form.score / 100f, formColor(form.score), modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(10.dp))
                                CaptionText("Sicherheit ${form.score}")
                            }
                        } else {
                            CaptionText(if (sp.practiced > 0) "Noch zu wenig Ergebnisse" else "Noch nicht geübt")
                        }
                    }
                }
            }
            CardGap()

            ClimbCard(modifier = Modifier.testTag("apHistory")) {
                CardTitle("Deine Simulationen")
                val runs = progress.apRuns
                if (runs.isEmpty()) {
                    BodyText("Noch keine Simulation. Starte mit dem Kurztest oder gleich mit einer ganzen Simulation.")
                } else {
                    val recent = runs.takeLast(20)
                    if (recent.size >= 2) {
                        CaptionText("Punkte (auf 16 Aufgaben gerechnet)")
                        LineChart(
                            values = recent.map { it.score * 16f / it.total },
                            color = accent,
                            labels = recent.mapIndexed { i, r -> if (i == 0 || i == recent.lastIndex) runDate(r.at) else "" },
                            height = 130.dp,
                            valueText = { "${it.roundToInt()}" },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    runs.takeLast(5).reversed().forEach { run ->
                        val sec = run.usedMs / 1000
                        BodyText("${runDate(run.at)} · ${runName(run)}: ${run.score} von ${run.total} Punkten")
                        CaptionText("Zeit ${sec / 60}:${(sec % 60).toString().padStart(2, '0')} von ${run.seconds / 60} min · ${apRating(run.score, run.total)}")
                        Spacer(Modifier.height(4.dp))
                    }
                    listOf(5 to "TMSnat", 4 to "HAM-Nat").forEach { (options, name) ->
                        runs.filter { it.options == options && it.total == 16 }.maxByOrNull { it.score }?.let { best ->
                            CaptionText("Bestes Ergebnis $name: ${best.score} von 16")
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(title: String, text: String) {
    val dimens = MentalMathTheme.dimens
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(title, fontSize = dimens.fonts.sideSheetEntry, color = accent)
        CaptionText(text)
    }
}
