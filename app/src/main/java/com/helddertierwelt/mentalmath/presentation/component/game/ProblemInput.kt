/*
Mental Math - Android app for practicing mental arithmetic
Copyright (C) 2025-2026 HeldDerTierwelt

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

package com.helddertierwelt.mentalmath.presentation.component.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.game.ProblemStatus
import com.helddertierwelt.mentalmath.domain.settings.enums.FormMode
import com.helddertierwelt.mentalmath.domain.settings.enums.InputDirectionMode
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red

@Composable
fun ProblemInput(
    operand1: Int,
    operand2: Int,
    operator: String,
    input: String,
    fontSize: TextUnit,
    width: Dp,
    problemStatus: ProblemStatus,
    inputDirectionMode: InputDirectionMode,
    formMode: FormMode,
    onInputClick: () -> Unit,
    onProblemClick: () -> Unit
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val onPrimary = MaterialTheme.colorScheme.onPrimary
        val top = operand1.toString()
        val bottom = operand2.toString()
        val length = maxOf(top.length, bottom.length)

        val verticalText = buildString {
            appendLine(top.padStart(length + 1))
            append(operator)
            append(bottom.padStart(length))
        }
        val horizontalText = "$operand1 $operator $operand2"
        val robotoMono = FontFamily(Font(R.font.roboto_mono_regular_mental_math, FontWeight.Normal))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(fontSize.value.dp * 2.5f)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onProblemClick() }
        ) {
            Text(
                text = horizontalText,
                fontSize = fontSize,
                lineHeight = fontSize,
                textAlign = TextAlign.Center,
                color = onPrimary,
                modifier = Modifier.alpha(if (formMode == FormMode.VERTICAL) 0f else 1f)
            )

            Text(
                text = verticalText,
                fontFamily = robotoMono,
                fontSize = fontSize,
                lineHeight = fontSize,
                textAlign = TextAlign.Center,
                color = onPrimary,
                modifier = Modifier.alpha(if (formMode == FormMode.VERTICAL) 1f else 0f)
            )
        }
        Box(
            modifier = Modifier.width(width + (1.2 * fontSize.value).dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "=",
                fontSize = fontSize,
                color = onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(width)
                    .height((fontSize * 1.6f).value.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onInputClick() }
                    .drawBehind {
                        val arrowSize = 12.dp.toPx()
                        val y = size.height - 0.75f * arrowSize
                        val start = arrowSize / 2
                        val end = size.width - arrowSize / 2

                        drawLine(
                            color = onPrimary,
                            start = Offset(start + arrowSize / 2, y),
                            end = Offset(end - arrowSize / 2, y),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        val arrowPath = Path().apply {
                            if (inputDirectionMode == InputDirectionMode.RIGHT_TO_LEFT) {
                                moveTo(start, y)
                                lineTo(start + arrowSize, y - arrowSize / 2)
                                lineTo(start + arrowSize, y + arrowSize / 2)
                                close()
                            } else {
                                moveTo(end, y)
                                lineTo(end - arrowSize, y - arrowSize / 2)
                                lineTo(end - arrowSize, y + arrowSize / 2)
                                close()
                            }
                        }
                        drawPath(path = arrowPath, color = onPrimary)
                    }
            ) {
                val color = when(problemStatus){
                    ProblemStatus.CORRECT -> green
                    ProblemStatus.INCORRECT -> red
                    ProblemStatus.SKIPPED -> onPrimary
                    ProblemStatus.TIMEOUT -> onPrimary
                }
                Text(
                    text = input,
                    fontSize = fontSize,
                    modifier = Modifier.align(Alignment.Center),
                    color = color
                )
            }
        }
    }
}