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
import com.helddertierwelt.mentalmath.presentation.theme.blue
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
                        iconTint = blue,
                        onClick = {
                            val started = if (skillId != null) {
                                viewModel.startSession(summary.type, skillId)
                            } else {
                                viewModel.startReview()
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
        CardTitle("${typeTitle(summary.type)} · ${summary.title}")
        val outcome = summary.outcome
        if (outcome != null && skill != null) {
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
            BodyText("Richtig: $correct von $total")
            BodyText("Ø Zeit pro Aufgabe: ${secondsText(avgMs / 1000.0)}")
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
                            text = (prompt.caption?.let { "$it: " } ?: "") + prompt.text,
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
                                text = "= " + Fmt.expected(r.problem.expected, r.problem.choices),
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
