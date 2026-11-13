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

package com.helddertierwelt.mentalmath.presentation.climb.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.engine.DailyPlan
import com.helddertierwelt.mentalmath.domain.climb.model.DayPlan
import com.helddertierwelt.mentalmath.domain.climb.model.PlanBlock
import com.helddertierwelt.mentalmath.domain.climb.model.TrainingPlan
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow

/** Auswahlfeld (Ziel, Minuten). */
@Composable
fun PlanChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, padding: androidx.compose.ui.unit.Dp = 6.dp) {
    val dimens = MentalMathTheme.dimens
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(dimens.components.cornerRadius * 2))
            .background(if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = padding),
    ) {
        Text(
            text = text,
            fontSize = dimens.fonts.caption,
            color = if (selected) accent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun kindIcon(kind: String): Int = when (kind) {
    DailyPlan.WARMUP -> R.drawable.history_24dp
    DailyPlan.CHECK -> R.drawable.question_mark_24dp
    DailyPlan.NEW -> R.drawable.terrain_24dp
    DailyPlan.WEAK -> R.drawable.refresh_24dp
    DailyPlan.SPEED -> R.drawable.timer_24dp
    else -> R.drawable.casino_24dp
}

/** Eine Zeile des Plans: Haken oder Symbol, Titel und Begründung. */
@Composable
private fun PlanRow(block: PlanBlock, done: Boolean, current: Boolean, label: String?, onClick: () -> Unit) {
    val dimens = MentalMathTheme.dimens
    val tint = when {
        done -> green
        block.kind == DailyPlan.WEAK -> yellow
        block.kind == DailyPlan.SPEED -> red
        else -> accent
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.components.cornerRadius))
            .background(if (current) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.primary)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("planBlock_${block.kind}"),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
        ) {
            Icon(
                painter = painterResource(if (done) R.drawable.check_24dp else kindIcon(block.kind)),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = block.title,
                fontSize = dimens.fonts.sideSheetEntry,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (done) 0.55f else 1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            CaptionText(if (label != null) "$label · ${block.reason}" else block.reason)
        }
        Spacer(Modifier.width(8.dp))
        CaptionText("${block.count}")
    }
}

/** Datum eines Epoch-Tags, z. B. „8.5.2027“. */
fun dayText(day: Long): String {
    val f = java.text.SimpleDateFormat("d.M.yyyy", java.util.Locale.GERMANY)
    f.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return f.format(java.util.Date(day * 86_400_000L))
}

/** Kurzbeschreibung: „20 min · Mo–Fr · bis 8.5.2027“. */
fun planSchedule(plan: TrainingPlan): String {
    val days = when (plan.weekdays.sorted()) {
        (1..7).toList() -> "täglich"
        (1..5).toList() -> "Mo–Fr"
        else -> plan.weekdays.sorted().joinToString(" ") { DailyPlan.WEEKDAY_NAMES[it - 1] }
    }
    val until = plan.endDay?.let { "bis ${dayText(it)}" } ?: "ohne Ende"
    return "${plan.minutes} min · $days · $until"
}

/**
 * Karte „Heute“ auf der Startseite: ohne Plan ein Knopf zum Erstellen, sonst die heutigen Abschnitte
 * aller aktiven Pläne mit Begründung.
 */
@Composable
fun PlanCard(
    plan: DayPlan,
    plans: List<TrainingPlan>,
    onCreate: () -> Unit,
    onStart: (PlanBlock) -> Unit,
    onManage: () -> Unit,
) {
    ClimbCard(modifier = Modifier.testTag("planCard")) {
        if (plans.isEmpty()) {
            CardTitle("Dein Trainingsplan")
            BodyText("Leg fest, was du üben willst, wie viel Zeit du an welchen Tagen hast und bis wann – die App plant jeden Tag eine passende Einheit: wiederholen, was zu verblassen droht, Neues lernen und gezielt üben, wo es hakt.")
            Spacer(Modifier.height(12.dp))
            ActionButton(
                text = "Plan erstellen",
                icon = R.drawable.add_24dp,
                iconTint = green,
                onClick = onCreate,
                modifier = Modifier.testTag("planCreate"),
            )
        } else {
            PlanContent(plan, plans, onStart, onManage)
        }
    }
}

@Composable
private fun PlanContent(plan: DayPlan, plans: List<TrainingPlan>, onStart: (PlanBlock) -> Unit, onManage: () -> Unit) {
    val dimens = MentalMathTheme.dimens
    val names = plans.associate { it.id to it.name }
    val activeIds = plan.blocks.mapNotNull { it.planId }.distinct()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Heute",
                fontSize = dimens.fonts.settingsLabel,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            if (plan.blocks.isNotEmpty()) {
                val doneCount = plan.blocks.count { it.id in plan.done }
                CaptionText("ca. ${DailyPlan.minutesOf(plan.blocks)} min · $doneCount von ${plan.blocks.size} erledigt · ${activeIds.mapNotNull { names[it] }.joinToString(", ")}")
            }
        }
        Icon(
            painter = painterResource(R.drawable.tune_24dp),
            contentDescription = "Pläne verwalten",
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            modifier = Modifier
                .size(dimens.icons.menuItem)
                .clip(CircleShape)
                .clickable { onManage() }
                .testTag("planSettings"),
        )
    }
    Spacer(Modifier.height(10.dp))
    if (plan.blocks.isEmpty()) {
        val text = when {
            plans.all { it.paused } -> "Alle Pläne sind pausiert."
            else -> "Heute ist laut deinen Plänen kein Übungstag. Wer mag, übt trotzdem frei weiter – Wiederholungen findest du unten."
        }
        BodyText(text)
        Spacer(Modifier.height(10.dp))
        ActionButton(text = "Pläne verwalten", icon = R.drawable.tune_24dp, iconTint = accent, onClick = onManage, modifier = Modifier.testTag("planManage"))
        return
    }
    plan.notes.forEach { note ->
        BodyText(note, color = yellow, modifier = Modifier.padding(bottom = 6.dp))
    }
    val next = plan.next
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        plan.blocks.forEach { block ->
            val label = if (activeIds.size > 1) block.planId?.let { names[it] } else null
            PlanRow(block, done = block.id in plan.done, current = block.id == next?.id, label = label, onClick = { onStart(block) })
        }
    }
    Spacer(Modifier.height(12.dp))
    if (next != null) {
        ActionButton(
            text = if (plan.done.isEmpty()) "Plan starten" else "Weiter im Plan",
            subtitle = next.title,
            icon = R.drawable.play_arrow_24dp,
            iconTint = green,
            onClick = { onStart(next) },
            modifier = Modifier.testTag("planStart"),
        )
    } else {
        BodyText("Plan für heute geschafft! Morgen gibt es einen neuen – wer mag, übt frei weiter.")
    }
}
