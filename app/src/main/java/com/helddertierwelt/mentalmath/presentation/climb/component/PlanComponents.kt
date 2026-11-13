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
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow

/** Auswahlfeld (Ziel, Minuten). */
@Composable
fun PlanChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = MentalMathTheme.dimens
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(dimens.components.cornerRadius * 2))
            .background(if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 6.dp),
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

/**
 * Einstellungen des Trainingsplans: Ziele (mehrere möglich), Minuten pro Tag und ob neue Levels
 * dem Kletterweg der Reihe nach folgen.
 */
@Composable
fun PlanSetup(
    goals: List<String>,
    minutes: Int,
    followPath: Boolean,
    onChange: (goals: List<String>, minutes: Int, followPath: Boolean) -> Unit,
) {
    CaptionText("Mein Ziel (mehrere möglich)")
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DailyPlan.GOALS.forEach { goal ->
            val on = goal in goals
            PlanChip(
                text = DailyPlan.goalName(goal),
                selected = on,
                onClick = {
                    val next = if (on) goals - goal else goals + goal
                    if (next.isNotEmpty()) onChange(next, minutes, followPath)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("planGoal_$goal"),
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    CaptionText("Zeit pro Tag")
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(10, 20, 30).forEach { m ->
            PlanChip(
                text = "$m min",
                selected = minutes == m,
                onClick = { onChange(goals, m, followPath) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("planMinutes_$m"),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    SwitchRow(
        title = "Kletterweg der Reihe nach",
        subtitle = if (followPath) {
            "Neue Levels kommen in der Reihenfolge des Kletterwegs."
        } else {
            "Der Plan wählt neue Levels frei aus den Welten, die zu deinem Ziel passen."
        },
        checked = followPath,
        onCheckedChange = { onChange(goals, minutes, it) },
        modifier = Modifier.testTag("planFollowPath"),
    )
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
private fun PlanRow(block: PlanBlock, done: Boolean, current: Boolean, onClick: () -> Unit) {
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
            CaptionText(block.reason)
        }
        Spacer(Modifier.width(8.dp))
        CaptionText("${block.count}")
    }
}

/**
 * Karte „Heute“ auf der Startseite. Beim ersten Mal fragt sie nach Ziel und Zeit, danach zeigt
 * sie den Tagesplan mit Begründungen und startet den nächsten Baustein.
 */
@Composable
fun PlanCard(
    plan: DayPlan,
    setUp: Boolean,
    goals: List<String>,
    minutes: Int,
    followPath: Boolean,
    onSetUp: (goals: List<String>, minutes: Int, followPath: Boolean) -> Unit,
    onStart: (PlanBlock) -> Unit,
    onSettings: () -> Unit,
) {
    ClimbCard(modifier = Modifier.testTag("planCard")) {
        if (!setUp) {
            var g by remember(goals) { mutableStateOf(goals) }
            var m by remember(minutes) { mutableStateOf(minutes) }
            var f by remember(followPath) { mutableStateOf(followPath) }
            CardTitle("Dein Trainingsplan")
            BodyText("Jeden Tag ein kurzer Plan: Wiederholen, was gerade zu verblassen droht, Neues lernen und gezielt üben, wo es hakt.")
            Spacer(Modifier.height(12.dp))
            PlanSetup(g, m, f) { ng, nm, nf -> g = ng; m = nm; f = nf }
            Spacer(Modifier.height(12.dp))
            ActionButton(
                text = "Plan erstellen",
                icon = R.drawable.check_circle_24dp,
                iconTint = green,
                onClick = { onSetUp(g, m, f) },
                modifier = Modifier.testTag("planCreate"),
            )
        } else {
            PlanContent(plan, goals, onStart, onSettings)
        }
    }
}

@Composable
private fun PlanContent(plan: DayPlan, goals: List<String>, onStart: (PlanBlock) -> Unit, onSettings: () -> Unit) {
    val dimens = MentalMathTheme.dimens
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Heute",
                fontSize = dimens.fonts.settingsLabel,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            val doneCount = plan.blocks.count { it.id in plan.done }
            CaptionText("ca. ${DailyPlan.minutesOf(plan.blocks)} min · $doneCount von ${plan.blocks.size} erledigt · Ziel: ${goals.joinToString(", ") { DailyPlan.goalName(it) }}")
        }
        Icon(
            painter = painterResource(R.drawable.tune_24dp),
            contentDescription = "Plan einstellen",
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            modifier = Modifier
                .size(dimens.icons.menuItem)
                .clip(CircleShape)
                .clickable { onSettings() }
                .testTag("planSettings"),
        )
    }
    Spacer(Modifier.height(10.dp))
    val next = plan.next
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        plan.blocks.forEach { block ->
            PlanRow(block, done = block.id in plan.done, current = block.id == next?.id, onClick = { onStart(block) })
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
    } else if (plan.blocks.isEmpty()) {
        BodyText("Für heute gibt es nichts Neues – alles sitzt. Schau morgen wieder vorbei.")
    } else {
        BodyText("Plan für heute geschafft! Morgen gibt es einen neuen – wer mag, übt frei weiter.")
    }
}
