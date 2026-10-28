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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.engine.Evaluation
import com.helddertierwelt.mentalmath.domain.climb.engine.ProblemResult
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionType
import com.helddertierwelt.mentalmath.domain.climb.engine.Summary
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import com.helddertierwelt.mentalmath.presentation.climb.component.ActionButton
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.CaptionText
import com.helddertierwelt.mentalmath.presentation.climb.component.CardGap
import com.helddertierwelt.mentalmath.presentation.climb.component.CardTitle
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.climb.component.SolutionSteps
import com.helddertierwelt.mentalmath.presentation.climb.component.StarRow
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow

/** Auswertung einer Übungseinheit mit Zielen, Sternen, Hinweisen und allen Aufgaben. */
@Composable
fun ClimbResultScreen(navController: NavController) {
    val owner = LocalActivity.current as ViewModelStoreOwner
    val viewModel: ClimbViewModel = hiltViewModel(owner)
    val summaryOrNull by viewModel.summary.collectAsState()
    val progress by viewModel.progress.collectAsState()

    fun toHome() {
        viewModel.playTap()
        viewModel.clearSummary()
        navController.navigate("climb") {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    BackHandler { toHome() }

    val summary = summaryOrNull
    Scaffold(topBar = { ClimbTopBar(title = "Ergebnis", navigationIcon = null) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("resultColumn")
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            if (summary == null) {
                BodyText("Kein Ergebnis vorhanden.")
                Spacer(Modifier.height(16.dp))
                ActionButton(text = "Zum Kletterweg", icon = R.drawable.terrain_24dp, iconTint = green, onClick = { toHome() })
            } else {
                SummaryCard(summary = summary, viewModel = viewModel)
                CardGap()
                if (summary.free?.isAp == true) {
                    ApCategoryCard(summary = summary, onPractice = { skillId ->
                        viewModel.playTap()
                        viewModel.clearSummary()
                        navController.navigate("level/$skillId") {
                            popUpTo("climb") { inclusive = false }
                            launchSingleTop = true
                        }
                    })
                    CardGap()
                }
                MessagesCard(summary = summary)
                ProblemsCard(results = summary.results)
                CardGap()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val skillId = summary.skillId
                    val next = summary.unlocked ?: viewModel.nextSkill(progress)?.takeIf { it.id != skillId && viewModel.isUnlocked(progress, it.id) && summary.outcome?.passed == true }
                    if (next != null) {
                        ActionButton(
                            text = "Weiter: ${next.title}",
                            subtitle = "Das nächste Level",
                            icon = R.drawable.terrain_24dp,
                            iconTint = green,
                            onClick = {
                                viewModel.playTap()
                                viewModel.clearSummary()
                                navController.navigate("level/${next.id}") {
                                    popUpTo("climb") { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    ActionButton(
                        text = "Nochmal",
                        subtitle = "Gleiche Übung mit neuen Aufgaben",
                        icon = R.drawable.refresh_24dp,
                        iconTint = accent,
                        onClick = {
                            val started = when {
                                summary.type == SessionType.FREE -> viewModel.restartFree()
                                summary.type == SessionType.PLACEMENT -> summary.placement?.let { viewModel.startPlacement(it.worldId) } ?: false
                                skillId != null -> viewModel.startSession(summary.type, skillId)
                                else -> viewModel.startReview()
                            }
                            if (started) {
                                navController.navigate("climbplay") {
                                    popUpTo("climbresult") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                    )
                    if (skillId != null) {
                        ActionButton(
                            text = "Zum Level",
                            subtitle = viewModel.skill(skillId)?.title,
                            icon = R.drawable.arrow_back_24dp,
                            iconTint = MaterialTheme.colorScheme.onPrimary,
                            onClick = {
                                viewModel.playTap()
                                viewModel.clearSummary()
                                navController.navigate("level/$skillId") {
                                    popUpTo("climb") { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    if (summary.type == SessionType.FREE) {
                        val ap = summary.free?.isAp == true
                        ActionButton(
                            text = if (ap) "Zum TMSnat-Training" else "Einstellungen",
                            subtitle = if (ap) "Simulationen, Kategorien und Verlauf" else "Aufgabenarten und Modus ändern",
                            icon = R.drawable.tune_24dp,
                            iconTint = MaterialTheme.colorScheme.onPrimary,
                            onClick = {
                                viewModel.playTap()
                                viewModel.clearSummary()
                                navController.navigate(if (ap) "aptraining" else "free") {
                                    popUpTo("climb") { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    ActionButton(
                        text = "Zum Kletterweg",
                        icon = R.drawable.terrain_24dp,
                        iconTint = green,
                        onClick = { toHome() },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun typeTitle(type: SessionType): String = when (type) {
    SessionType.GUIDED -> "Geführtes Üben"
    SessionType.PRACTICE -> "Freies Üben"
    SessionType.TEST -> "Prüfung"
    SessionType.REFRESH -> "Auffrischung"
    SessionType.REVIEW -> "Wiederholung"
    SessionType.FREE -> "Freies Training"
    SessionType.PLACEMENT -> "Einstufungstest"
}

/** Eine Zeile für die Aufgabenliste: Aufgabe mit kurzer Beschreibung, ohne doppelte Fragezeichen. */
private fun promptLine(prompt: com.helddertierwelt.mentalmath.domain.climb.model.Prompt): String {
    val caption = prompt.caption
    val lines = if (prompt.lines.isNotEmpty() && prompt.hideAfterMs == null) " (" + prompt.lines.joinToString("; ") { it.trim() } + ")" else ""
    return when {
        caption == null -> prompt.text + lines
        prompt.text.startsWith("?") -> caption
        caption.length <= 8 -> prompt.text + lines
        else -> "$caption: ${prompt.text}$lines"
    }
}

/** Schulnote aus dem Anteil richtiger Antworten. */
fun grade(correct: Int, total: Int): Int {
    if (total == 0) return 6
    val pct = correct * 100.0 / total
    return when {
        pct >= 92 -> 1
        pct >= 81 -> 2
        pct >= 67 -> 3
        pct >= 50 -> 4
        pct >= 30 -> 5
        else -> 6
    }
}

@Composable
private fun SummaryCard(summary: Summary, viewModel: ClimbViewModel) {
    val dimens = MentalMathTheme.dimens
    val results = summary.results
    val correct = results.count { it.correct }
    val total = results.size
    val avgMs = if (total == 0) 0L else results.sumOf { it.timeMs } / total
    val skill = summary.skillId?.let { viewModel.skill(it) }
    ClimbCard {
        CardTitle(
            when {
                summary.free?.isAp != true -> "${typeTitle(summary.type)} · ${summary.title}"
                summary.title.startsWith("TMSnat") -> summary.title
                else -> "TMSnat · ${summary.title}"
            },
        )
        val outcome = summary.outcome
        val placement = summary.placement
        if (placement != null) {
            Text(
                text = if (placement.passed) "Bestanden – Welt übersprungen" else "Noch nicht bestanden",
                fontSize = dimens.fonts.settingsLabel,
                color = if (placement.passed) green else red,
            )
            Spacer(Modifier.height(10.dp))
            GoalRow(
                ok = placement.accuracyOk,
                text = "Richtig: ${placement.correct} von ${placement.total} (Ziel: höchstens ${placement.allowedMistakes} " +
                    (if (placement.allowedMistakes == 1) "Fehler)" else "Fehler)"),
            )
            val pct = if (placement.pace > 99) "–" else "${kotlin.math.round(placement.pace * 100).toInt()} %"
            GoalRow(
                ok = placement.paceOk,
                text = "Tempo: $pct der Zeit für einen Stern (Ziel: höchstens 100 %)",
            )
        } else if (outcome != null && skill != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRow(stars = outcome.stars, size = dimens.icons.scoreHeader * 0.6f)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (outcome.passed) "Bestanden" else "Noch nicht bestanden",
                    fontSize = dimens.fonts.settingsLabel,
                    color = if (outcome.passed) green else red,
                )
            }
            Spacer(Modifier.height(10.dp))
            val goals = skill.goals
            val allowed = Evaluation.allowedMistakes(goals)
            val accuracyOk = outcome.accuracy + 1e-9 >= goals.minAccuracy
            GoalRow(ok = accuracyOk, text = "Richtig: $correct von $total (Ziel: höchstens $allowed Fehler)")
            GoalRow(ok = outcome.avgMs / 1000.0 <= goals.star1, text = "Ø Zeit: ${secondsText(outcome.avgMs / 1000.0)} (★ bei ${secondsText(goals.star1)}, ★★ bei ${secondsText(goals.star2)}, ★★★ bei ${secondsText(goals.star3)})")
        } else {
            summary.free?.takeIf { it.mode == com.helddertierwelt.mentalmath.domain.climb.engine.FreeMode.SIMULATION }?.let { rules ->
                val run = summary.apRun
                val usedSec = (run?.usedMs ?: 0L) / 1000
                Text(
                    text = "$correct von $total Punkten",
                    fontSize = dimens.fonts.settingsLabel,
                    color = if (correct * 4 >= total * 3) green else if (correct * 2 >= total) yellow else red,
                )
                CaptionText(apRating(correct, total))
                Spacer(Modifier.height(6.dp))
                BodyText("Zeit: ${usedSec / 60}:${(usedSec % 60).toString().padStart(2, '0')} von ${rules.param / 60}:${(rules.param % 60).toString().padStart(2, '0')} min")
                val open = results.count { it.timedOut && it.answer.isEmpty() }
                if (open > 0) BodyText(if (open == 1) "1 Aufgabe nicht bearbeitet" else "$open Aufgaben nicht bearbeitet", color = yellow)
                CaptionText("${if (rules.param < 1_500) "Im echten Test hast du 25 Minuten für 16 Aufgaben. " else ""}Die Bewertung ist eine Orientierung, keine offizielle Auswertung.")
            }
            summary.free?.takeIf { it.mode != com.helddertierwelt.mentalmath.domain.climb.engine.FreeMode.SIMULATION }?.let { rules ->
                val p = paramLabel(rules.mode, rules.param)
                CaptionText(rules.mode.title + (if (p.isNotEmpty()) " · $p" else ""))
                summary.freeEndLevel?.let { lvl ->
                    CaptionText("Stufe automatisch angepasst – zuletzt: ${listOf("Leicht", "Mittel", "Schwer")[(lvl - 1).coerceIn(0, 2)]}")
                }
                if (rules.mode == com.helddertierwelt.mentalmath.domain.climb.engine.FreeMode.PRUEFUNG) {
                    Text(
                        text = "Note ${grade(correct, total)}",
                        fontSize = dimens.fonts.settingsLabel,
                        color = if (grade(correct, total) <= 4) green else red,
                    )
                } else {
                    Text(
                        text = "${summary.freeScore} richtig",
                        fontSize = dimens.fonts.settingsLabel,
                        color = green,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            if (summary.free?.mode != com.helddertierwelt.mentalmath.domain.climb.engine.FreeMode.SIMULATION) {
                BodyText("Richtig: $correct von $total")
                BodyText("Ø Zeit pro Aufgabe: ${secondsText(avgMs / 1000.0)}")
            } else {
                val answered = results.filter { !(it.timedOut && it.answer.isEmpty()) }
                if (answered.isNotEmpty()) BodyText("Ø Zeit pro bearbeiteter Aufgabe: ${secondsText(answered.sumOf { it.timeMs } / answered.size / 1000.0)}")
            }
            val hints = results.sumOf { it.hintsUsed }
            if (summary.type == SessionType.GUIDED) {
                CaptionText(if (hints == 0) "Alle Schritte ohne Hilfe gelöst." else "Hilfen gebraucht: $hints")
            }
        }
    }
}

@Composable
private fun GoalRow(ok: Boolean, text: String) {
    val dimens = MentalMathTheme.dimens
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Icon(
            painter = painterResource(if (ok) R.drawable.check_24dp else R.drawable.close_24dp),
            contentDescription = null,
            tint = if (ok) green else red,
            modifier = Modifier.size(dimens.icons.problemStatus * 0.8f),
        )
        Spacer(Modifier.width(10.dp))
        BodyText(text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MessagesCard(summary: Summary) {
    val messages = mutableListOf<Pair<String, androidx.compose.ui.graphics.Color>>()
    val outcome = summary.outcome
    if (summary.type == SessionType.TEST && outcome != null) {
        when {
            outcome.passed && summary.starsBefore == 0 -> messages += "Level geschafft!" to green
            outcome.passed && outcome.stars > summary.starsBefore -> messages += "Neuer Rekord: ${outcome.stars} Sterne!" to green
            outcome.passed -> messages += "Wieder bestanden." to green
            else -> messages += "Fast! Übe noch ein wenig und versuch es dann noch einmal." to yellow
        }
    }
    if (summary.free != null && summary.freeScore > summary.freeBestBefore && summary.freeBestBefore > 0) {
        messages += "Neuer Bestwert! Vorher: ${summary.freeBestBefore}" to green
    }
    summary.placement?.let { placement ->
        messages += if (placement.passed) {
            val levels = if (placement.newlyMastered == 1) "1 Level gilt" else "${placement.newlyMastered} Levels gelten"
            Pair(
                "$levels jetzt als geschafft (je ein Stern). In den nächsten Tagen kommen sie in die Wiederholung – " +
                    "so zeigt sich, ob alles sitzt. Mehr Sterne holst du dir in den Prüfungen der Levels.",
                green,
            )
        } else {
            Pair(
                "Kein Problem: Klettere die Welt Level für Level – dort lernst du jede Methode in Ruhe. " +
                    "Den Test kannst du jederzeit wiederholen.",
                yellow,
            )
        }
    }
    summary.unlocked?.let { messages += "Neu freigeschaltet: ${it.title}" to green }
    summary.sentBackTo?.let {
        messages += "Zweimal nicht bestanden – das ist ein Zeichen, die Grundlage zu festigen. Frische zuerst „${it.title}“ auf, dann geht die Prüfung wieder." to yellow
    }
    if (summary.type == SessionType.REFRESH) {
        messages += if (summary.refreshed) {
            Pair("Aufgefrischt! Das Level ist wieder fit.", green)
        } else {
            Pair("Noch nicht ganz – übe das Level noch einmal und frische es dann erneut auf.", yellow)
        }
    }
    if (summary.becameRusty.isNotEmpty()) {
        messages += "Eingerostet: ${summary.becameRusty.joinToString { it.title }}. Diese Levels bitte auffrischen." to yellow
    }
    if (messages.isEmpty()) return
    ClimbCard {
        messages.forEach { (text, color) ->
            BodyText(text, color = color, modifier = Modifier.padding(vertical = 3.dp))
        }
    }
    CardGap()
}

@Composable
private fun ProblemsCard(results: List<ProblemResult>) {
    val dimens = MentalMathTheme.dimens
    val open = remember { mutableStateMapOf<Int, Boolean>() }
    ClimbCard {
        CardTitle("Aufgaben")
        CaptionText("Tippe eine Aufgabe an, um den Lösungsweg zu sehen.")
        Spacer(Modifier.height(8.dp))
        results.forEachIndexed { i, r ->
            if (i > 0) {
                HorizontalDivider(
                    thickness = dimens.components.dividerThickness / 2,
                    color = MaterialTheme.colorScheme.background,
                )
            }
            val expanded = open[i] ?: false
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { open[i] = !expanded }
                    .padding(vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(if (r.correct) R.drawable.check_24dp else R.drawable.close_24dp),
                        contentDescription = null,
                        tint = if (r.correct) green else red,
                        modifier = Modifier.size(dimens.icons.problemStatus * 0.8f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val prompt = r.problem.prompt
                        Text(
                            text = promptLine(prompt),
                            fontSize = dimens.fonts.sideSheetEntry,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Row {
                            if (!r.correct) {
                                Text(
                                    text = if (r.timedOut) "Zeit abgelaufen" else r.answer.ifEmpty { "–" },
                                    fontSize = dimens.fonts.caption,
                                    color = red,
                                    textDecoration = if (r.timedOut) null else TextDecoration.LineThrough,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = Fmt.answer(r.problem.expected, r.problem.choices),
                                fontSize = dimens.fonts.caption,
                                color = if (r.correct) green else MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(8.dp))
                            CaptionText(secondsText(r.timeMs / 1000.0))
                        }
                    }
                    Icon(
                        painter = painterResource(if (expanded) R.drawable.keyboard_arrow_up_24dp else R.drawable.keyboard_arrow_down_24dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.size(dimens.icons.problemStatus * 0.8f),
                    )
                }
                if (expanded) {
                    Spacer(Modifier.height(6.dp))
                    SolutionSteps(problem = r.problem, showHints = true, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/** Einordnung eines Simulationsergebnisses (eigene Faustregel, keine offizielle Norm). */
fun apRating(correct: Int, total: Int): String {
    if (total == 0) return ""
    val share = correct.toDouble() / total
    return when {
        share >= 0.875 -> "Sehr stark – so kann es im Test laufen."
        share >= 0.69 -> "Gut – mit etwas Tempo-Training ist noch mehr drin."
        share >= 0.5 -> "Solide Grundlage – übe gezielt die schwachen Kategorien."
        else -> "Weiter üben – nimm dir die Kategorien einzeln vor."
    }
}

/** Ergebnis je Kategorie einer Einheit des TMSnat-Trainings. */
@Composable
private fun ApCategoryCard(summary: Summary, onPractice: (String) -> Unit) {
    val dimens = MentalMathTheme.dimens
    val byCategory = summary.results.groupBy { com.helddertierwelt.mentalmath.domain.climb.curriculum.ApCategory.of(it.problem.skillId) }
        .filterKeys { it != null }
    if (byCategory.isEmpty()) return
    ClimbCard {
        CardTitle("Nach Kategorien")
        com.helddertierwelt.mentalmath.domain.climb.curriculum.ApCategory.entries.forEach { c ->
            val list = byCategory[c] ?: return@forEach
            val ok = list.count { it.correct }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPractice(c.skillId) }
                    .padding(vertical = 5.dp),
            ) {
                Icon(
                    painter = painterResource(if (ok == list.size) R.drawable.check_24dp else R.drawable.close_24dp),
                    contentDescription = null,
                    tint = if (ok == list.size) green else if (ok > 0) yellow else red,
                    modifier = Modifier.size(dimens.icons.problemStatus * 0.7f),
                )
                Spacer(Modifier.width(10.dp))
                Text(c.title, fontSize = dimens.fonts.sideSheetEntry, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.weight(1f))
                CaptionText("$ok/${list.size}")
            }
        }
        val weakest = byCategory.entries
            .map { (c, list) -> c!! to list.count { it.correct }.toDouble() / list.size }
            .filter { it.second < 1.0 }
            .minByOrNull { it.second }
        if (weakest != null) {
            Spacer(Modifier.height(8.dp))
            ActionButton(
                text = "Üben: ${weakest.first.title}",
                subtitle = "Erklärung, geführtes Üben und Prüfung",
                icon = R.drawable.terrain_24dp,
                iconTint = accent,
                onClick = { onPractice(weakest.first.skillId) },
            )
        } else {
            CaptionText("Tippe eine Kategorie an, um sie gezielt zu üben.")
        }
    }
}
