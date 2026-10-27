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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.engine.Phase
import com.helddertierwelt.mentalmath.domain.climb.engine.Session
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.settings.enums.InputDirectionMode
import com.helddertierwelt.mentalmath.domain.settings.enums.InputMode
import com.helddertierwelt.mentalmath.presentation.component.game.BackspaceButton
import com.helddertierwelt.mentalmath.presentation.component.game.EnterButton
import com.helddertierwelt.mentalmath.presentation.component.game.NumberButton
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow

@Composable
fun monoFont(): FontFamily = FontFamily(Font(R.font.roboto_mono_regular_mental_math, FontWeight.Normal))

/** Schriftgröße der Aufgabe passend zur Länge. */
@Composable
fun problemFontSize(text: String): TextUnit {
    val base = MentalMathTheme.dimens.fonts.problem
    return when {
        text.length <= 9 -> base
        text.length <= 13 -> base * 0.8f
        text.length <= 18 -> base * 0.62f
        else -> base * 0.48f
    }
}

/** Die Aufgabe: kleine Überschrift, optionale Zeilen und die große Hauptzeile. */
@Composable
fun ProblemDisplay(problem: Problem, modifier: Modifier = Modifier, hidden: Boolean = false) {
    val dimens = MentalMathTheme.dimens
    if (hidden) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Jetzt aus dem Gedächtnis!",
                fontSize = dimens.fonts.sideSheetEntry,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(6.dp))
            Text(text = "?", fontSize = dimens.fonts.problem, color = MaterialTheme.colorScheme.onPrimary)
        }
        return
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        problem.prompt.caption?.let {
            Text(
                text = it,
                fontSize = dimens.fonts.sideSheetEntry,
                lineHeight = dimens.fonts.sideSheetEntry * 1.25f,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(6.dp))
        }
        if (problem.prompt.lines.isNotEmpty()) {
            val mono = monoFont()
            val lineSize = if (problem.prompt.lines.size > 4) dimens.fonts.sideSheetEntry else dimens.fonts.settingsLabel
            problem.prompt.lines.forEach { line ->
                Text(
                    text = line,
                    fontFamily = mono,
                    fontSize = lineSize,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.height(6.dp))
        }
        val size = problemFontSize(problem.prompt.text)
        Text(
            text = problem.prompt.text,
            fontSize = size,
            lineHeight = size * 1.1f,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** Eingabefeld „= …“ mit Farbe je nach Rückmeldung. */
@Composable
fun AnswerField(session: Session, unit: String?, modifier: Modifier = Modifier) {
    val dimens = MentalMathTheme.dimens
    val feedback = session.feedback
    val color = when {
        session.phase == Phase.FLASH && feedback?.correct == true -> green
        session.phase == Phase.FLASH || session.phase == Phase.REVEAL -> red
        else -> MaterialTheme.colorScheme.onPrimary
    }
    val shown = when {
        session.phase == Phase.ANSWERING -> Fmt.input(session.input)
        feedback?.timedOut == true -> "Zeit!"
        feedback != null && feedback.userAnswer.isNotEmpty() -> feedback.userAnswer
        else -> Fmt.input(session.input)
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "=",
            fontSize = dimens.fonts.problem * 0.8f,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Spacer(Modifier.width(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(dimens.components.answerLineWidth * 1.2f)
                .heightIn(min = 48.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(dimens.components.cornerRadius))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = shown,
                fontSize = dimens.fonts.problem * 0.8f,
                color = color,
                maxLines = 1,
            )
        }
        if (unit != null) {
            Spacer(Modifier.width(8.dp))
            Text(text = unit, fontSize = dimens.fonts.problem * 0.6f, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

/** Lösungsweg: Schritte als „Rechnung = Ergebnis“, auf Wunsch mit Hinweisen. */
@Composable
fun SolutionSteps(
    problem: Problem,
    showHints: Boolean,
    modifier: Modifier = Modifier,
    upTo: Int = problem.steps.size,
    revealedColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val dimens = MentalMathTheme.dimens
    val mono = monoFont()
    Column(modifier = modifier) {
        problem.method?.let {
            Text(
                text = it,
                fontSize = dimens.fonts.caption,
                color = accent,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        problem.steps.take(upTo).forEach { step ->
            Text(
                text = Fmt.equation(step.label, step.expected, problem.choices),
                fontFamily = mono,
                fontSize = dimens.fonts.sideSheetEntry,
                lineHeight = dimens.fonts.sideSheetEntry * 1.3f,
                color = revealedColor,
            )
            if (showHints && step.hint != null) {
                Text(
                    text = step.hint,
                    fontSize = dimens.fonts.caption,
                    lineHeight = dimens.fonts.caption * 1.35f,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                )
            }
        }
        problem.notes.forEach { note ->
            Text(
                text = note,
                fontSize = dimens.fonts.caption,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            )
        }
    }
}

/** Geführtes Üben: erledigte Schritte, aktueller Schritt mit Eingabe, Hinweis. */
@Composable
fun GuidedSteps(session: Session, modifier: Modifier = Modifier) {
    val dimens = MentalMathTheme.dimens
    val problem = session.problem ?: return
    val mono = monoFont()
    val remaining = problem.steps.size - session.stepIndex - 1
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        problem.steps.forEachIndexed { i, step ->
            val labelSize = if (step.label.length > 22) dimens.fonts.sideSheetEntry else dimens.fonts.settingsLabel
            when {
                i < session.stepIndex -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.check_24dp),
                        contentDescription = null,
                        tint = green,
                        modifier = Modifier.size(dimens.icons.problemStatus * 0.7f),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = Fmt.equation(step.label, step.expected, problem.choices),
                        fontFamily = mono,
                        fontSize = dimens.fonts.sideSheetEntry,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    )
                }

                i == session.stepIndex -> {
                    val revealed = session.phase == Phase.REVEAL
                    val finished = session.phase == Phase.FLASH
                    val value = when {
                        revealed -> Fmt.value(step.expected, problem.choices)
                        finished -> session.feedback?.userAnswer ?: ""
                        else -> Fmt.input(session.input).ifEmpty { "?" }
                    }
                    val color = when {
                        revealed -> yellow
                        finished -> green
                        session.feedback?.correct == false -> red
                        else -> MaterialTheme.colorScheme.onPrimary
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(dimens.components.cornerRadius))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "${step.label} ${if (step.expected is Expected.Approx) "≈" else "="} ",
                            fontFamily = mono,
                            fontSize = labelSize,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            text = value,
                            fontFamily = mono,
                            fontSize = labelSize,
                            color = color,
                        )
                    }
                    if ((session.showHint || revealed) && step.hint != null) {
                        Text(
                            text = step.hint,
                            fontSize = dimens.fonts.caption,
                            lineHeight = dimens.fonts.caption * 1.35f,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(yellow.copy(alpha = 0.15f), RoundedCornerShape(dimens.components.cornerRadius))
                                .padding(8.dp),
                        )
                    }
                }

                i == session.stepIndex + 1 -> Text(
                    text = if (remaining == 1) "… noch 1 Schritt" else "… noch $remaining Schritte",
                    fontSize = dimens.fonts.caption,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 4.dp),
                )

                else -> Unit
            }
        }
    }
}

/** Kleiner Zusatzknopf (Komma, Rest, Bruchstrich, Tipp …). */
@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onPrimary, enabled: Boolean = true) {
    val dimens = MentalMathTheme.dimens
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(dimens.components.cornerRadius * 2),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = color,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor = color.copy(alpha = 0.38f),
        ),
        modifier = modifier,
    ) {
        Text(text = text, fontSize = dimens.fonts.sideSheetEntry, color = if (enabled) color else color.copy(alpha = 0.38f))
    }
}

/** Ziffernblock der App, ergänzt um die Sondertasten der jeweiligen Eingabeart. */
@Composable
fun ClimbKeypad(
    inputKind: InputKind,
    inputMode: InputMode,
    inputEmpty: Boolean,
    enabled: Boolean,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
) {
    val extra: Pair<Char, String>? = when (inputKind) {
        InputKind.DECIMAL -> ',' to ","
        InputKind.REMAINDER -> 'R' to "Rest"
        InputKind.FRACTION -> '/' to "Bruch /"
        else -> null
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (extra != null) {
            PillButton(text = extra.second, onClick = { onKey(extra.first) }, enabled = enabled, modifier = Modifier.testTag("keyExtra"))
        }
        listOf(inputMode.upperNumberRow, 4..6, inputMode.lowerNumberRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { number ->
                    Box(Modifier.testTag("key$number")) {
                        NumberButton(
                            number = number,
                            onPress = {},
                            onClick = { if (enabled) onKey('0' + number) },
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.testTag("keyBack")) {
                BackspaceButton(
                    onClick = { if (enabled) onBackspace() },
                    onPress = {},
                    inputDirectionMode = InputDirectionMode.LEFT_TO_RIGHT,
                )
            }
            Box(Modifier.testTag("key0")) {
                NumberButton(
                    number = 0,
                    onPress = {},
                    onClick = { if (enabled) onKey('0') },
                )
            }
            Box(Modifier.testTag("keyEnter")) {
                EnterButton(
                    iconId1 = R.drawable.keyboard_double_arrow_right_24dp,
                    iconId2 = R.drawable.keyboard_tab_24dp,
                    onClick = onEnter,
                    isToggled = inputEmpty,
                    enabled = enabled,
                )
            }
        }
    }
}

/** Antwortknöpfe für Auswahlaufgaben. */
@Composable
fun ChoiceButtons(choices: List<String>, enabled: Boolean, onChoose: (Int) -> Unit) {
    val dimens = MentalMathTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        choices.forEachIndexed { i, choice ->
            Button(
                onClick = { onChoose(i) },
                enabled = enabled,
                shape = RoundedCornerShape(dimens.components.cornerRadius * 2),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dimens.components.button * 0.75f),
            ) {
                Text(text = choice, fontSize = dimens.fonts.settingsLabel)
            }
        }
    }
}

/** Kurze Rückmeldung unter der Aufgabe (Prüfung): richtig oder die richtige Lösung. */
@Composable
fun FlashFeedback(session: Session, modifier: Modifier = Modifier) {
    val dimens = MentalMathTheme.dimens
    val feedback = session.feedback ?: return
    if (session.phase != Phase.FLASH) return
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(if (feedback.correct) R.drawable.check_24dp else R.drawable.close_24dp),
            contentDescription = null,
            tint = if (feedback.correct) green else red,
            modifier = Modifier.size(dimens.icons.problemStatus),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = when {
                feedback.correct -> feedback.note ?: "Richtig!"
                feedback.timedOut -> "Zeit abgelaufen – richtig wäre ${feedback.correctAnswer}"
                else -> "Richtig wäre ${feedback.correctAnswer}"
            },
            fontSize = dimens.fonts.sideSheetEntry,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
