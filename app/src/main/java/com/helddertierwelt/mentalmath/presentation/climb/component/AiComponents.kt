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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.data.climb.ai.ModelState
import com.helddertierwelt.mentalmath.domain.climb.ai.AiAction
import com.helddertierwelt.mentalmath.domain.climb.ai.AiActions
import com.helddertierwelt.mentalmath.domain.climb.ai.AiMarkdown
import com.helddertierwelt.mentalmath.domain.climb.ai.AiMessage
import com.helddertierwelt.mentalmath.domain.climb.ai.AiModel
import com.helddertierwelt.mentalmath.domain.climb.ai.AiQuiz
import com.helddertierwelt.mentalmath.domain.climb.ai.AiStatus
import com.helddertierwelt.mentalmath.domain.climb.ai.AiText
import com.helddertierwelt.mentalmath.domain.climb.ai.AiToolUse
import com.helddertierwelt.mentalmath.domain.climb.ai.AiToolbox
import com.helddertierwelt.mentalmath.domain.climb.ai.MdBlock
import com.helddertierwelt.mentalmath.domain.climb.ai.MdSpan
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow

// --- Formatierter Text ---------------------------------------------------------------------------

private fun annotated(spans: List<MdSpan>, codeBackground: Color): AnnotatedString = buildAnnotatedString {
    for (s in spans) {
        withStyle(
            SpanStyle(
                fontWeight = if (s.bold) FontWeight.Bold else null,
                fontStyle = if (s.italic) FontStyle.Italic else null,
                fontFamily = if (s.code) FontFamily.Monospace else null,
                background = if (s.code) codeBackground else Color.Unspecified,
            ),
        ) { append(s.text) }
    }
}

/** Antwort der KI mit Absätzen, Listen, Tabellen, fett und kursiv. */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onPrimary) {
    val dimens = MentalMathTheme.dimens
    val blocks = remember(text) { AiMarkdown.parse(text) }
    val size = dimens.fonts.sideSheetEntry
    val codeBg = MaterialTheme.colorScheme.background
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (b in blocks) {
            when (b) {
                is MdBlock.Paragraph -> Text(annotated(b.spans, codeBg), fontSize = size, lineHeight = size * 1.4f, color = color)
                is MdBlock.Heading -> Text(
                    annotated(b.spans, codeBg),
                    fontSize = if (b.level <= 2) dimens.fonts.settingsLabel else size,
                    lineHeight = dimens.fonts.settingsLabel * 1.25f,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                is MdBlock.Bullet -> Row(Modifier.padding(start = (14 * b.level).dp)) {
                    Text("•", fontSize = size, lineHeight = size * 1.4f, color = accent)
                    Spacer(Modifier.width(8.dp))
                    Text(annotated(b.spans, codeBg), fontSize = size, lineHeight = size * 1.4f, color = color, modifier = Modifier.weight(1f))
                }
                is MdBlock.Numbered -> Row(Modifier.padding(start = (14 * b.level).dp)) {
                    Text(b.number, fontSize = size, lineHeight = size * 1.4f, color = accent, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(annotated(b.spans, codeBg), fontSize = size, lineHeight = size * 1.4f, color = color, modifier = Modifier.weight(1f))
                }
                is MdBlock.Code -> Text(
                    b.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = dimens.fonts.caption,
                    color = color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(codeBg)
                        .padding(10.dp),
                )
                is MdBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(accent),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(annotated(b.spans, codeBg), fontSize = size, lineHeight = size * 1.4f, color = color, fontStyle = FontStyle.Italic)
                }
                is MdBlock.Table -> MdTable(b, color, codeBg)
                MdBlock.Rule -> HorizontalDivider(color = color.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
private fun MdTable(t: MdBlock.Table, color: Color, codeBg: Color) {
    val dimens = MentalMathTheme.dimens
    val columns = maxOf(t.header.size, t.rows.maxOfOrNull { it.size } ?: 0)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
    ) {
        (listOf(t.header) + t.rows).forEachIndexed { i, row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (i == 0) codeBg else Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                for (c in 0 until columns) {
                    Text(
                        annotated(row.getOrElse(c) { listOf(MdSpan("")) }, codeBg),
                        fontSize = dimens.fonts.caption,
                        lineHeight = dimens.fonts.caption * 1.3f,
                        fontWeight = if (i == 0) FontWeight.Bold else null,
                        color = color,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                    )
                }
            }
        }
    }
}

// --- Nachrichten ---------------------------------------------------------------------------------

/** Nachricht der Person (rechts). */
@Composable
fun AiUserBubble(m: AiMessage, modifier: Modifier = Modifier) {
    val dimens = MentalMathTheme.dimens
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(
            Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                .background(accent.copy(alpha = 0.16f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            m.task?.let { task ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.question_mark_24dp),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(task, fontSize = dimens.fonts.caption, lineHeight = dimens.fonts.caption * 1.3f, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(m.text, fontSize = dimens.fonts.sideSheetEntry, lineHeight = dimens.fonts.sideSheetEntry * 1.35f, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

/** Symbol zu einem Werkzeug. */
private fun toolIcon(name: String): Int = when (name) {
    AiToolbox.CALCULATE -> R.drawable.calculate_24dp
    AiToolbox.TMSNAT_STATUS, AiToolbox.CLIMB_STATUS -> R.drawable.bar_chart_24dp
    AiToolbox.SEARCH_METHODS -> R.drawable.book_24dp
    AiToolbox.CREATE_TASK, AiToolbox.CHECK_ANSWER -> R.drawable.question_mark_24dp
    AiToolbox.GET_HINT -> R.drawable.lightbulb_24dp
    AiToolbox.CHANGE_SETTING -> R.drawable.settings_24dp
    else -> R.drawable.play_arrow_24dp
}

/** Kleine Hinweise, welche Werkzeuge die KI benutzt hat (z. B. „96 : 0,8 = 120“). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiToolChips(tools: List<AiToolUse>) {
    if (tools.isEmpty()) return
    val dimens = MentalMathTheme.dimens
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Gleiche Hinweise nur einmal zeigen.
        tools.distinctBy { it.name + it.label }.take(8).forEach { t ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("aiTool_${t.name}"),
            ) {
                Icon(
                    painter = painterResource(toolIcon(t.name)),
                    contentDescription = null,
                    tint = if (t.ok) accent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    t.label,
                    fontSize = dimens.fonts.caption * 0.92f,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 240.dp),
                )
            }
        }
    }
}

/** Vorschlag der KI mit Knopf. */
@Composable
fun AiActionCard(action: AiAction, done: Boolean, enabled: Boolean, onRun: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = MentalMathTheme.dimens
    val label = remember(action) { AiActions.label(action) }
    val icon = when (action) {
        is AiAction.Setting -> when (action.key) {
            AiAction.SETTING_REMINDER -> R.drawable.notifications_active_24dp
            AiAction.SETTING_THEME -> R.drawable.palette_24dp
            else -> R.drawable.tune_24dp
        }
        is AiAction.ApSimulation -> R.drawable.timer_24dp
        AiAction.ApReview -> R.drawable.refresh_24dp
        is AiAction.Level -> if (action.mode == AiAction.MODE_LEARN) R.drawable.book_24dp else R.drawable.terrain_24dp
        else -> R.drawable.play_arrow_24dp
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.07f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("aiAction"),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(dimens.icons.menuItem * 0.9f))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label.title, fontSize = dimens.fonts.sideSheetEntry, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Medium)
            CaptionText(label.detail)
        }
        Spacer(Modifier.width(8.dp))
        if (done) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(R.drawable.check_24dp), contentDescription = null, tint = green, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(label.doneText, fontSize = dimens.fonts.caption, color = green)
            }
        } else {
            Button(
                onClick = onRun,
                enabled = enabled,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .testTag("aiActionRun"),
            ) {
                Text(label.button, fontSize = dimens.fonts.caption)
            }
        }
    }
}

/** Eingeklappte Gedanken der KI (Denkmodus). */
@Composable
fun AiThoughts(thought: String, thinkingNow: Boolean) {
    val dimens = MentalMathTheme.dimens
    var open by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("aiThoughtsToggle"),
        ) {
            Icon(painter = painterResource(R.drawable.lightbulb_24dp), contentDescription = null, tint = yellow, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (thinkingNow) "Gedanken mitlesen" else "Gedanken",
                fontSize = dimens.fonts.caption,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(if (open) R.drawable.keyboard_arrow_up_24dp else R.drawable.keyboard_arrow_down_24dp),
                contentDescription = if (open) "Gedanken ausblenden" else "Gedanken zeigen",
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
        AnimatedVisibility(visible = open) {
            Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp)) {
                CaptionText("Die KI denkt meist auf Englisch nach – die Antwort kommt auf Deutsch.")
                Spacer(Modifier.height(4.dp))
                Text(
                    AiText.clean(thought).let { if (it.length > 6_000) "… " + it.takeLast(6_000) else it },
                    fontSize = dimens.fonts.caption,
                    lineHeight = dimens.fonts.caption * 1.35f,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/** „Denkt nach …“ mit kleinem Kreisel. */
@Composable
fun AiTyping(text: String) {
    val dimens = MentalMathTheme.dimens
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("aiTyping")) {
        CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = dimens.fonts.caption, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f))
    }
}

/** Eine Aufgabe, die die KI im Chat gestellt hat – mit Antwortknöpfen bei A–E-Aufgaben. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiQuizCard(card: AiQuiz.Card, enabled: Boolean, onAnswer: (String) -> Unit, modifier: Modifier = Modifier) {
    val dimens = MentalMathTheme.dimens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.07f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("aiQuizCard"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.question_mark_24dp), contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Aufgabe ${card.id}" + if (card.title.isNotEmpty()) " · ${card.title}" else "",
                fontSize = dimens.fonts.caption,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (card.answered) {
                Icon(painter = painterResource(R.drawable.check_24dp), contentDescription = "beantwortet", tint = green, modifier = Modifier.size(16.dp))
            }
        }
        Text(card.text, fontSize = dimens.fonts.sideSheetEntry, lineHeight = dimens.fonts.sideSheetEntry * 1.4f, color = MaterialTheme.colorScheme.onPrimary)
        if (card.choices.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                card.choices.forEachIndexed { i, choice ->
                    val active = enabled && !card.answered
                    Text(
                        choice,
                        fontSize = dimens.fonts.caption,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (active) 1f else 0.55f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, accent.copy(alpha = if (active) 0.6f else 0.25f), RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(enabled = active) { onAnswer("Meine Antwort: $choice") }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("aiQuizChoice$i"),
                    )
                }
            }
        } else if (!card.answered) {
            CaptionText("Antwort: ${card.format} – schreib sie unten ins Feld.")
        }
    }
}

/** Antwort der KI (links): Gedanken, Text, gestellte Aufgaben, Werkzeuge, Vorschläge. */
@Composable
fun AiAssistantBubble(
    m: AiMessage,
    activity: String?,
    isLast: Boolean,
    busy: Boolean,
    onAction: (Int, AiAction) -> Unit,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
    quiz: List<AiQuiz.Card> = emptyList(),
    onAnswer: (String) -> Unit = {},
) {
    val dimens = MentalMathTheme.dimens
    val streaming = m.status == AiStatus.STREAMING
    val shown = remember(m.text) { AiText.clean(m.text) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(if (streaming) "aiReplyStreaming" else "aiReply"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.auto_awesome_24dp), contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("KI-Coach", fontSize = dimens.fonts.caption, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
            if (!streaming && shown.isNotBlank()) {
                Icon(
                    painter = painterResource(R.drawable.content_copy_24dp),
                    contentDescription = "Antwort kopieren",
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onCopy(AiText.plain(m.text)) }
                        .padding(6.dp),
                )
            }
        }
        if (m.thought.isNotBlank()) AiThoughts(m.thought, thinkingNow = streaming && m.text.isEmpty())
        if (shown.isNotEmpty()) MarkdownText(shown, modifier = Modifier.testTag("aiReplyText"))
        if (streaming && (shown.isEmpty() || activity != null)) AiTyping(activity ?: "Schreibt …")
        quiz.forEach { AiQuizCard(it, enabled = !busy, onAnswer = onAnswer) }
        AiToolChips(m.tools)
        m.actions.forEachIndexed { i, a ->
            AiActionCard(a, done = i in m.done, enabled = !busy || !streaming, onRun = { onAction(i, a) })
        }
        when (m.status) {
            AiStatus.STOPPED -> Row(verticalAlignment = Alignment.CenterVertically) {
                CaptionText("Abgebrochen.", modifier = Modifier.weight(1f))
                if (isLast) TextButton(onClick = onRetry) { Text("Nochmal", color = accent) }
            }
            AiStatus.ERROR -> Column {
                Text(m.error ?: "Da ist etwas schiefgelaufen.", fontSize = dimens.fonts.caption, color = red)
                if (isLast) TextButton(onClick = onRetry, modifier = Modifier.testTag("aiRetry")) { Text("Nochmal", color = accent) }
            }
            else -> {}
        }
    }
}

/** Vorschläge zum Antippen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSuggestions(items: List<String>, enabled: Boolean, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    val dimens = MentalMathTheme.dimens
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { i, s ->
            Text(
                s,
                fontSize = dimens.fonts.caption,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(enabled = enabled) { onPick(s) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("aiSuggestion$i"),
            )
        }
    }
}

// --- Modelle -------------------------------------------------------------------------------------

/** Ein Modell mit Zustand und Knöpfen (Download, Verwenden, Löschen). */
@Composable
fun AiModelCard(
    model: AiModel,
    state: ModelState,
    selected: Boolean,
    deviceRamGb: Int,
    crashed: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val dimens = MentalMathTheme.dimens
    val tooSmall = deviceRamGb in 1 until model.minRamGb
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("aiModel_${model.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.memory_24dp), contentDescription = null, tint = if (selected) accent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(model.title, fontSize = dimens.fonts.settingsLabel, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.weight(1f))
            Text(
                model.badge,
                fontSize = dimens.fonts.caption * 0.9f,
                color = if (model.experimental) yellow else accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background((if (model.experimental) yellow else accent).copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        BodyText(model.summary)
        if (model.phone) CaptionText("Download ${model.sizeLabel} · empfohlen ab ${model.minRamGb} GB Arbeitsspeicher")
        if (tooSmall && model.phone) Text("Dein Handy hat etwa $deviceRamGb GB Arbeitsspeicher – das kann knapp werden.", fontSize = dimens.fonts.caption, color = yellow)
        if (crashed) Text("Beim Laden wurde die App auf diesem Handy beendet. Nimm lieber ein kleineres Modell.", fontSize = dimens.fonts.caption, color = red)
        when (state) {
            ModelState.Missing -> Button(
                onClick = onDownload,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                modifier = Modifier.testTag("aiDownload_${model.id}"),
            ) {
                Icon(painter = painterResource(R.drawable.download_24dp), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Herunterladen (${model.sizeLabel})", fontSize = dimens.fonts.caption)
            }
            is ModelState.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(progress = { state.fraction }, color = accent, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CaptionText(
                        "${AiModel.gigabytes(state.bytes)} von ${AiModel.gigabytes(state.total)} (${(state.fraction * 100).toInt()} %)" + (state.waiting?.let { " · $it" } ?: ""),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onCancel) { Text("Abbrechen", color = red, fontSize = dimens.fonts.caption) }
                }
            }
            is ModelState.Verifying -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(progress = { state.fraction }, color = green, modifier = Modifier.fillMaxWidth())
                CaptionText("Datei wird geprüft … ${(state.fraction * 100).toInt()} %")
            }
            is ModelState.Ready -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(R.drawable.check_circle_24dp), contentDescription = null, tint = green, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                CaptionText("Auf dem Handy (${AiModel.gigabytes(state.bytes)})", modifier = Modifier.weight(1f))
                if (onDelete != null) TextButton(onClick = onDelete, modifier = Modifier.testTag("aiDelete_${model.id}")) { Text("Löschen", color = red, fontSize = dimens.fonts.caption) }
                if (!selected && model.phone) {
                    Button(
                        onClick = onSelect,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                        modifier = Modifier.testTag("aiSelect_${model.id}"),
                    ) { Text("Verwenden", fontSize = dimens.fonts.caption) }
                }
            }
            is ModelState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.message, fontSize = dimens.fonts.caption, color = red)
                if (model.phone) Button(
                    onClick = onDownload,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                ) { Text("Noch einmal laden", fontSize = dimens.fonts.caption) }
            }
        }
    }
}
