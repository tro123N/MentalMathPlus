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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.engine.Form
import com.helddertierwelt.mentalmath.domain.climb.engine.Stats
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import com.helddertierwelt.mentalmath.presentation.climb.component.BarChart
import com.helddertierwelt.mentalmath.presentation.climb.component.BarRow
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.CaptionText
import com.helddertierwelt.mentalmath.presentation.climb.component.CardGap
import com.helddertierwelt.mentalmath.presentation.climb.component.CardTitle
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.climb.component.LineChart
import com.helddertierwelt.mentalmath.presentation.climb.component.PillButton
import com.helddertierwelt.mentalmath.presentation.climb.component.ShareBar
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import kotlin.math.roundToInt

/** Farbe für einen Sicherheitswert von 0 bis 100. */
fun formColor(score: Int) = when {
    score >= 85 -> green
    score >= 70 -> com.helddertierwelt.mentalmath.presentation.theme.blue
    score >= 50 -> yellow
    else -> red
}

/** Tag als „27.10.“. */
private fun dayLabel(day: Long): String {
    val c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    c.timeInMillis = day * 86_400_000L
    return "${c.get(java.util.Calendar.DAY_OF_MONTH)}.${c.get(java.util.Calendar.MONTH) + 1}."
}

/** Statistik des Kletterwegs: Überblick, Verlauf, Stärken und Schwächen, Welten. */
@Composable
fun StatsScreen(navController: NavController) {
    val owner = LocalActivity.current as ViewModelStoreOwner
    val viewModel: ClimbViewModel = hiltViewModel(owner)
    val progress by viewModel.progress.collectAsState()
    var range by rememberSaveable { mutableIntStateOf(14) }
    val today = viewModel.today()

    fun back() {
        viewModel.playTap()
        if (!navController.popBackStack()) navController.navigate("climb") { launchSingleTop = true }
    }
    BackHandler { back() }

    fun openLevel(id: String) {
        viewModel.playTap()
        navController.navigate("level/$id")
    }

    Scaffold(topBar = { ClimbTopBar(title = "Statistik", onNavigation = { back() }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("statsColumn")
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            // Überblick
            ClimbCard {
                CardTitle("Überblick")
                val solved = progress.totalSolved
                val pct = if (solved == 0L) null else (100.0 * progress.totalCorrect / solved).roundToInt()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(Fmt.int(solved), "Aufgaben gelöst", Modifier.weight(1f))
                    StatTile(if (pct == null) "–" else "$pct %", "richtig", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val streak = viewModel.streak(progress)
                    StatTile(
                        if (streak == 1) "1 Tag" else "$streak Tage",
                        "in Folge · Rekord ${maxOf(progress.bestStreak, progress.streakDays)}",
                        Modifier.weight(1f),
                    )
                    StatTile(Stats.activeDays(progress).toString(), "Übungstage", Modifier.weight(1f))
                }
            }
            CardGap()

            // Verlauf
            ClimbCard(modifier = Modifier.testTag("statsHistory")) {
                CardTitle("Verlauf")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(14, 30).forEach { d ->
                        PillButton(
                            text = "$d Tage",
                            onClick = {
                                viewModel.playTap()
                                range = d
                            },
                            color = if (range == d) accent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                val days = Stats.lastDays(progress.days, today, range)
                if (days.all { it.solved == 0 }) {
                    BodyText("In diesem Zeitraum hast du noch nicht geübt.")
                } else {
                    CaptionText("Gelöste Aufgaben pro Tag")
                    val step = if (range <= 14) 2 else 5
                    BarChart(
                        values = days.map { it.solved.toFloat() },
                        labels = days.mapIndexed { i, d -> if ((days.size - 1 - i) % step == 0) dayLabel(d.day) else "" },
                        color = accent,
                    )
                    val active = days.filter { it.solved > 0 }
                    val dayWord = if (active.size == 1) "1 Tag" else "${active.size} Tagen"
                    CaptionText("${active.sumOf { it.solved }} Aufgaben an $dayWord · Ø ${active.sumOf { it.solved } / active.size.coerceAtLeast(1)} pro Übungstag")
                    if (active.size >= 2) {
                        Spacer(Modifier.height(12.dp))
                        CaptionText("Richtig in Prozent (an Übungstagen)")
                        LineChart(
                            values = active.map { (100f * it.correct / it.solved).coerceIn(0f, 100f) },
                            color = green,
                            labels = active.mapIndexed { i, d -> if (i == 0 || i == active.lastIndex) dayLabel(d.day) else "" },
                            height = 120.dp,
                            valueText = { "${it.roundToInt()} %" },
                        )
                    }
                }
            }
            CardGap()

            // Stärken und Schwächen
            val strengths = Stats.strengths(viewModel.worlds, progress)
            val weaknesses = Stats.weaknesses(viewModel.worlds, progress)
            ClimbCard(modifier = Modifier.testTag("statsStrengths")) {
                CardTitle("Stärken und Schwächen")
                CaptionText("Sicherheit aus deinen letzten Aufgaben je Level: Trefferquote zählt 60 %, Tempo 40 %. 100 = alles richtig und so schnell wie für drei Sterne.")
                Spacer(Modifier.height(8.dp))
                if (strengths.isEmpty() && weaknesses.isEmpty()) {
                    BodyText("Noch zu wenig Ergebnisse. Übe ein paar Levels – ab ${Stats.MIN_SAMPLE} Aufgaben pro Level siehst du hier, was sitzt und was nicht.")
                } else {
                    if (strengths.isNotEmpty()) {
                        Text("Das sitzt", fontSize = MentalMathTheme.dimens.fonts.sideSheetEntry, color = green)
                        strengths.forEach { FormRow(it) { openLevel(it.skill.id) } }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (weaknesses.isNotEmpty()) {
                        Text("Hier lohnt sich Übung", fontSize = MentalMathTheme.dimens.fonts.sideSheetEntry, color = yellow)
                        weaknesses.forEach { FormRow(it) { openLevel(it.skill.id) } }
                    }
                }
            }
            CardGap()

            // Welten
            ClimbCard(modifier = Modifier.testTag("statsWorlds")) {
                CardTitle("Welten")
                viewModel.worlds.forEach { world ->
                    val summary = Stats.worldSummary(world, progress)
                    val fraction = summary.mastered.toFloat() / world.skills.size
                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${world.number}. ${world.title}",
                                fontSize = MentalMathTheme.dimens.fonts.caption,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            if (summary.attention > 0) {
                                Icon(
                                    painter = painterResource(R.drawable.refresh_24dp),
                                    contentDescription = "auffrischen",
                                    tint = yellow,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            CaptionText("${summary.mastered}/${world.skills.size} · ${summary.stars}★" + (summary.form?.let { " · Sicherheit $it" } ?: ""))
                        }
                        Spacer(Modifier.height(3.dp))
                        ShareBar(fraction, if (fraction >= 1f) green else accent, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val dimens = MentalMathTheme.dimens
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(dimens.components.cornerRadius))
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 10.dp, horizontal = 6.dp),
    ) {
        Text(value, fontSize = dimens.fonts.settingsLabel, color = MaterialTheme.colorScheme.onPrimary, textAlign = TextAlign.Center)
        CaptionText(label)
    }
}

@Composable
private fun FormRow(form: Form, onClick: () -> Unit) {
    val dimens = MentalMathTheme.dimens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.components.cornerRadius))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (form.needsAttention) {
                Icon(
                    painter = painterResource(R.drawable.refresh_24dp),
                    contentDescription = "auffrischen",
                    tint = yellow,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = form.skill.title,
                fontSize = dimens.fonts.sideSheetEntry,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f),
            )
        }
        val pct = (form.accuracy * 100).roundToInt()
        val time = form.medianSec?.let { " · Ø ${secondsText(it)}" } ?: ""
        val state = if (form.needsAttention) "${form.label}, eingerostet" else form.label
        BarRow(
            label = "$pct % richtig$time · $state",
            fraction = form.score / 100f,
            value = "${form.score}",
            color = formColor(form.score),
        )
    }
}
