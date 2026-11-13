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

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.engine.DailyPlan
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.TrainingPlan
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import com.helddertierwelt.mentalmath.presentation.climb.component.ActionButton
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.CaptionText
import com.helddertierwelt.mentalmath.presentation.climb.component.CardGap
import com.helddertierwelt.mentalmath.presentation.climb.component.CardTitle
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.climb.component.PlanChip
import com.helddertierwelt.mentalmath.presentation.climb.component.ShareBar
import com.helddertierwelt.mentalmath.presentation.climb.component.SwitchRow
import com.helddertierwelt.mentalmath.presentation.climb.component.dayText
import com.helddertierwelt.mentalmath.presentation.climb.component.planSchedule
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import kotlin.math.roundToInt

/** Erster Prüfungstag des TMSnat 2027 (8. Mai 2027) als Epoch-Tag. */
private val TMS_DAY: Long = run {
    val c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    c.clear()
    c.set(2027, java.util.Calendar.MAY, 8)
    c.timeInMillis / 86_400_000L
}

/** Übersicht aller Trainingspläne. */
@Composable
fun PlansScreen(navController: NavController) {
    val owner = LocalActivity.current as ViewModelStoreOwner
    val viewModel: ClimbViewModel = hiltViewModel(owner)
    val progress by viewModel.progress.collectAsState()
    var confirm by remember { mutableStateOf<Pair<String, TrainingPlan>?>(null) }

    fun back() {
        viewModel.playTap()
        navController.popBackStack()
    }

    Scaffold(topBar = { ClimbTopBar(title = "Trainingspläne", onNavigation = { back() }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("plansColumn")
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            if (progress.plans.isEmpty()) {
                ClimbCard {
                    BodyText("Noch kein Plan. Wähle unten eine Vorlage – alles lässt sich danach anpassen.")
                }
                CardGap()
            }
            progress.plans.forEach { plan ->
                PlanOverviewCard(
                    plan = plan,
                    status = viewModel.planStatus(progress, plan),
                    onEdit = {
                        viewModel.playTap()
                        navController.navigate("planedit/${plan.id}")
                    },
                    onPause = {
                        viewModel.playTap()
                        viewModel.pausePlan(plan.id, !plan.paused)
                    },
                    onReset = { confirm = "reset" to plan },
                    onDelete = { confirm = "delete" to plan },
                )
                CardGap()
            }
            ClimbCard(modifier = Modifier.testTag("plansNew")) {
                CardTitle("Neuer Plan")
                CaptionText("Vorlage wählen – Auswahl, Zeit und Zeitraum stellst du danach ein.")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (DailyPlan.GOALS + DailyPlan.GOAL_CUSTOM).forEach { goal ->
                        val (icon, sub) = when (goal) {
                            DailyPlan.GOAL_PROFI -> R.drawable.terrain_24dp to "Kletterweg (Welten 1–12) und Rechentricks"
                            DailyPlan.GOAL_TMSNAT -> R.drawable.timer_24dp to "Arithmetisches Problemlösen und Einheiten"
                            DailyPlan.GOAL_ABI -> R.drawable.calculate_24dp to "Oberstufe/LK und Mathe im Alltag"
                            else -> R.drawable.tune_24dp to "Welten und Levels selbst auswählen"
                        }
                        ActionButton(
                            text = DailyPlan.goalName(goal),
                            subtitle = sub,
                            icon = icon,
                            iconTint = accent,
                            onClick = {
                                viewModel.playTap()
                                navController.navigate("planedit/new-$goal")
                            },
                            modifier = Modifier.testTag("plansNew_$goal"),
                        )
                    }
                }
            }
            CardGap()
            ScienceCard()
            Spacer(Modifier.height(24.dp))
        }
    }

    confirm?.let { (action, plan) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            textContentColor = MaterialTheme.colorScheme.onPrimary,
            title = { Text(if (action == "delete") "Plan löschen?" else "Plan zurücksetzen?") },
            text = {
                BodyText(
                    if (action == "delete") {
                        "„${plan.name}“ wird gelöscht. Deine Sterne und dein Fortschritt in den Levels bleiben erhalten."
                    } else {
                        "„${plan.name}“ beginnt ab heute neu (gleiche Dauer, gleiche Auswahl). Sterne und Level-Fortschritt bleiben erhalten."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (action == "delete") viewModel.deletePlan(plan.id) else viewModel.resetPlan(plan.id)
                        confirm = null
                    },
                    modifier = Modifier.testTag("planConfirm"),
                ) { Text(if (action == "delete") "Löschen" else "Zurücksetzen", color = if (action == "delete") red else accent) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Abbrechen", color = MaterialTheme.colorScheme.onPrimary) }
            },
        )
    }
}

@Composable
private fun PlanOverviewCard(
    plan: TrainingPlan,
    status: DailyPlan.Status,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    ClimbCard(modifier = Modifier.testTag("planItem_${plan.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                CardTitle(plan.name)
                CaptionText(planSchedule(plan))
            }
            val (label, color) = when {
                plan.paused -> "pausiert" to MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                status.phase == DailyPlan.Phase.FINAL -> status.phase.title to red
                status.phase == DailyPlan.Phase.CONSOLIDATE -> status.phase.title to yellow
                status.phase == DailyPlan.Phase.DONE -> status.phase.title to green
                else -> status.phase.title to accent
            }
            Text(
                text = label,
                color = color,
                fontSize = MentalMathTheme.dimens.fonts.caption,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        val fraction = if (status.total == 0) 0f else status.mastered.toFloat() / status.total
        ShareBar(fraction, green, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        val since = status.mastered - plan.masteredAtStart
        CaptionText("${status.mastered} von ${status.total} Levels geschafft" + (if (since > 0) " · $since seit Planstart" else ""))
        Spacer(Modifier.height(6.dp))
        BodyText(status.phase.text)
        status.daysLeft?.let { left ->
            if (left >= 0) CaptionText("Noch $left ${if (left == 1L) "Tag" else "Tage"} bis ${dayText(plan.endDay!!)}")
        }
        status.perDay?.let { per ->
            val text = "Bis zum Ende der Aufbauphase etwa ${String.format(java.util.Locale.GERMANY, "%.1f", per)} neue Levels pro Übungstag"
            CaptionText(if (status.tight) "$text – sehr knapp: mehr Minuten, mehr Tage oder späteres Enddatum helfen." else text)
        }
        status.accuracy?.let { acc ->
            val pct = (acc * 100).roundToInt()
            val hint = when {
                acc > 0.93 -> "läuft sehr gut – der Plan legt mehr Neues nach"
                acc < 0.75 -> "etwas viel – der Plan festigt erst, bevor Neues kommt"
                else -> "genau im Zielbereich"
            }
            CaptionText("Trefferquote zuletzt $pct % (Ziel ≈ 85 %): $hint")
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PlanChip("Bearbeiten", false, onEdit, Modifier.weight(1f).testTag("planEdit_${plan.id}"))
            PlanChip(if (plan.paused) "Fortsetzen" else "Pausieren", plan.paused, onPause, Modifier.weight(1f).testTag("planPause_${plan.id}"))
            PlanChip("Neu starten", false, onReset, Modifier.weight(1f).testTag("planReset_${plan.id}"))
            PlanChip("Löschen", false, onDelete, Modifier.weight(1f).testTag("planDelete_${plan.id}"))
        }
    }
}

/** Kurze Erklärung, nach welchen Prinzipien geplant wird. */
@Composable
private fun ScienceCard() {
    ClimbCard(modifier = Modifier.testTag("planScience")) {
        CardTitle("So plant die App")
        Spacer(Modifier.height(6.dp))
        listOf(
            "Verteilt wiederholen" to "Für jedes Level schätzt die App, wie lange es im Gedächtnis bleibt, und holt es kurz vor dem Verblassen zurück. Mit Enddatum so, dass zum Termin alles sitzt (Abstand etwa 10–20 % der Restzeit).",
            "Abrufen statt Nachlesen" to "Fast jede Minute ist eine kleine Abfrage – das verankert stärker als Wiederlesen.",
            "Mischen" to "Verschiedene Methoden durcheinander: So lernst du zu erkennen, welche gerade passt.",
            "Erst sicher, dann schnell" to "Tempo wird erst trainiert, wenn ein Level zu mindestens 90 % sitzt; vor Neuem prüft ein Grundlagen-Check die Basis.",
            "Etwa 85 % richtig" to "Läuft es deutlich besser, kommt mehr Neues; läuft es schlechter, wird erst gefestigt.",
            "Phasen" to "Aufbau → Festigen → Endspurt: In den letzten Tagen vor dem Enddatum keine neuen Levels mehr, dafür Simulationen und leichtes Wiederholen.",
        ).forEach { (title, text) ->
            Spacer(Modifier.height(6.dp))
            Text(title, color = accent, fontSize = MentalMathTheme.dimens.fonts.sideSheetEntry)
            CaptionText(text)
        }
    }
}

/** Plan anlegen oder bearbeiten. [planKey] ist die ID eines Plans oder „new-<Vorlage>“. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditScreen(navController: NavController, planKey: String) {
    val owner = LocalActivity.current as ViewModelStoreOwner
    val viewModel: ClimbViewModel = hiltViewModel(owner)
    val progress by viewModel.progress.collectAsState()
    val isNew = planKey.startsWith("new-")
    val initial = remember(planKey) {
        if (isNew) viewModel.draftPlan(planKey.removePrefix("new-")) else viewModel.plan(planKey) ?: viewModel.draftPlan(DailyPlan.GOAL_CUSTOM)
    }
    var draft by remember(planKey) { mutableStateOf(initial) }
    var name by remember(planKey) { mutableStateOf(initial.name) }
    val expanded = remember(planKey) { mutableStateMapOf<String, Boolean>() }
    var picking by remember { mutableStateOf<String?>(null) }
    val today = viewModel.today()

    fun back() {
        viewModel.playTap()
        navController.popBackStack()
    }

    Scaffold(topBar = { ClimbTopBar(title = if (isNew) "Neuer Plan" else "Plan bearbeiten", onNavigation = { back() }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("planEditColumn")
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            ClimbCard {
                CardTitle("Name")
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        focusedBorderColor = accent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("planName"),
                )
            }
            CardGap()

            // Auswahl
            ClimbCard(modifier = Modifier.testTag("planSelection")) {
                CardTitle("Was geübt wird")
                val selected = draft.skillIds.toSet()
                val masteredSel = draft.skillIds.count { progress.of(it).mastered }
                CaptionText("${selected.size} Levels ausgewählt, davon $masteredSel schon geschafft")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DailyPlan.GOALS.forEach { goal ->
                        PlanChip(
                            text = "+ " + DailyPlan.goalName(goal),
                            selected = false,
                            onClick = { draft = draft.copy(skillIds = DailyPlan.ordered(viewModel.worlds, draft.skillIds + DailyPlan.preset(viewModel.worlds, goal))) },
                            modifier = Modifier.weight(1f).testTag("planAdd_$goal"),
                        )
                    }
                    PlanChip("Leeren", false, { draft = draft.copy(skillIds = emptyList()) }, Modifier.weight(0.8f).testTag("planClear"))
                }
                Spacer(Modifier.height(8.dp))
                viewModel.worlds.forEach { world ->
                    val ids = world.skills.map { it.id }
                    val count = ids.count { it in selected }
                    val open = expanded[world.id] == true
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .padding(vertical = 2.dp),
                    ) {
                        CheckBox(
                            state = when (count) { 0 -> 0; ids.size -> 2; else -> 1 },
                            onClick = {
                                val next = if (count == ids.size) draft.skillIds - ids.toSet() else draft.skillIds + ids
                                draft = draft.copy(skillIds = DailyPlan.ordered(viewModel.worlds, next))
                            },
                            modifier = Modifier.testTag("planWorld_${world.id}"),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable { expanded[world.id] = !open },
                        ) {
                            Text(
                                text = "${world.number}. ${world.title}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = MentalMathTheme.dimens.fonts.sideSheetEntry,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            CaptionText("$count von ${ids.size} Levels")
                        }
                        Icon(
                            painter = painterResource(if (open) R.drawable.keyboard_arrow_up_24dp else R.drawable.keyboard_arrow_down_24dp),
                            contentDescription = if (open) "Zuklappen" else "Aufklappen",
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { expanded[world.id] = !open }
                                .padding(4.dp)
                                .testTag("planExpand_${world.id}"),
                        )
                    }
                    if (open) {
                        world.skills.forEach { skill ->
                            val on = skill.id in selected
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 28.dp)
                                    .clickable {
                                        val next = if (on) draft.skillIds - skill.id else draft.skillIds + skill.id
                                        draft = draft.copy(skillIds = DailyPlan.ordered(viewModel.worlds, next))
                                    }
                                    .padding(vertical = 4.dp)
                                    .testTag("planSkill_${skill.id}"),
                            ) {
                                CheckBox(state = if (on) 2 else 0, onClick = null)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = skill.title,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = MentalMathTheme.dimens.fonts.caption,
                                    modifier = Modifier.weight(1f),
                                )
                                if (progress.of(skill.id).mastered) {
                                    Icon(painterResource(R.drawable.star_24dp), contentDescription = "geschafft", tint = yellow, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
            CardGap()

            // Umfang
            ClimbCard(modifier = Modifier.testTag("planAmount")) {
                CardTitle("Wie viel")
                CaptionText("Minuten an jedem Übungstag")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 15, 20, 30, 45, 60).forEach { m ->
                        PlanChip("$m", draft.minutes == m, { draft = draft.copy(minutes = m) }, Modifier.weight(1f).testTag("planMinutes_$m"))
                    }
                }
                Spacer(Modifier.height(12.dp))
                CaptionText("Übungstage")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DailyPlan.WEEKDAY_NAMES.forEachIndexed { i, label ->
                        val d = i + 1
                        val on = d in draft.weekdays
                        PlanChip(label, on, {
                            val next = if (on) draft.weekdays - d else (draft.weekdays + d).sorted()
                            if (next.isNotEmpty()) draft = draft.copy(weekdays = next)
                        }, Modifier.weight(1f).testTag("planDay_$d"))
                    }
                }
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    title = "Kletterweg der Reihe nach",
                    subtitle = if (draft.followPath) "Neue Levels in der Reihenfolge des Kletterwegs." else "Neue Levels abwechselnd aus den gewählten Welten (Mischen).",
                    checked = draft.followPath,
                    onCheckedChange = { draft = draft.copy(followPath = it) },
                    modifier = Modifier.testTag("planFollowPath"),
                )
            }
            CardGap()

            // Zeitraum
            ClimbCard(modifier = Modifier.testTag("planPeriod")) {
                CardTitle("Zeitraum")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BodyText("Start: ${if (draft.startDay == today) "heute" else dayText(draft.startDay)}", modifier = Modifier.weight(1f))
                    PlanChip("Ändern", false, { picking = "start" }, Modifier.testTag("planStartDate"))
                }
                Spacer(Modifier.height(8.dp))
                BodyText("Ende: ${draft.endDay?.let { dayText(it) } ?: "ohne Ende"}")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlanChip("Ohne Ende", draft.endDay == null, { draft = draft.copy(endDay = null) }, Modifier.weight(1f).testTag("planEndNone"))
                    PlanChip("4 Wochen", false, { draft = draft.copy(endDay = maxOf(today, draft.startDay) + 27) }, Modifier.weight(1f).testTag("planEnd4w"))
                    PlanChip("3 Monate", false, { draft = draft.copy(endDay = maxOf(today, draft.startDay) + 90) }, Modifier.weight(1f).testTag("planEnd3m"))
                    PlanChip("Datum…", false, { picking = "end" }, Modifier.weight(1f).testTag("planEndDate"))
                }
                if (draft.skillIds.any { it.startsWith("w13.") } && TMS_DAY > today) {
                    Spacer(Modifier.height(6.dp))
                    PlanChip("Bis zum TMSnat (${dayText(TMS_DAY)})", draft.endDay == TMS_DAY, { draft = draft.copy(endDay = TMS_DAY) }, Modifier.fillMaxWidth().testTag("planEndTms"))
                }
                val preview = DailyPlan.status(draft, progress, today)
                Spacer(Modifier.height(10.dp))
                val days = draft.endDay?.let { DailyPlan.trainingDays(draft, maxOf(today, draft.startDay), it) }
                if (days != null) CaptionText("$days Übungstage, ca. ${days * draft.minutes / 60} Stunden insgesamt")
                preview.perDay?.let { per ->
                    val text = "Das sind etwa ${String.format(java.util.Locale.GERMANY, "%.1f", per)} neue Levels pro Übungstag in der Aufbauphase (die ersten 75 % der Zeit)."
                    CaptionText(if (per > 2) "$text Sehr knapp – mehr Zeit oder weniger Levels wählen." else text)
                }
            }
            CardGap()

            val valid = draft.skillIds.isNotEmpty() && (draft.endDay == null || draft.endDay!! >= draft.startDay)
            ActionButton(
                text = if (isNew) "Plan speichern" else "Änderungen speichern",
                subtitle = if (draft.skillIds.isEmpty()) "Wähle mindestens ein Level" else planSchedule(draft),
                icon = R.drawable.check_circle_24dp,
                iconTint = green,
                enabled = valid,
                onClick = {
                    viewModel.playTap()
                    viewModel.savePlan(draft.copy(name = name.trim().ifEmpty { "Mein Plan" }))
                    navController.popBackStack()
                },
                modifier = Modifier.testTag("planSave"),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    picking?.let { which ->
        val initialDay = if (which == "start") draft.startDay else draft.endDay ?: (today + 30)
        val state = rememberDatePickerState(initialSelectedDateMillis = initialDay * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { ms ->
                            val day = ms / 86_400_000L
                            draft = if (which == "start") {
                                draft.copy(startDay = day, endDay = draft.endDay?.let { maxOf(it, day) })
                            } else {
                                draft.copy(endDay = maxOf(day, draft.startDay))
                            }
                        }
                        picking = null
                    },
                    modifier = Modifier.testTag("dateOk"),
                ) { Text("OK", color = accent) }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Abbrechen") } },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Ankreuzfeld: 0 leer, 1 teilweise, 2 ganz. */
@Composable
private fun CheckBox(state: Int, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val color = if (state > 0) accent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (state == 2) accent else MaterialTheme.colorScheme.primary)
            .border(2.dp, color, RoundedCornerShape(6.dp))
            .let { if (onClick != null) it.clickable { onClick() } else it },
    ) {
        when (state) {
            2 -> Icon(painterResource(R.drawable.check_24dp), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            1 -> Icon(painterResource(R.drawable.remove_24dp), contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
    }
}
