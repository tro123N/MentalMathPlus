/*
Mental Math - Android app for practicing mental arithmetic
Copyright (C) 2025 HeldDerTierwelt

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red

@Composable
fun MathTaskDisplay(
    operand1: Int,
    operand2: Int,
    operator: String,
    input: String,
    fontSize: TextUnit,
    width: Dp,
    isCorrect: Boolean?,
    isRightToLeft: Boolean,
    onClick: () -> Unit
) {

    val color = MaterialTheme.colorScheme.onPrimary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Text(
            text = "$operand1 $operator $operand2",
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            lineHeight = fontSize,
            color = color
        )
        Spacer(modifier = Modifier.height((0.5 * fontSize.value).dp))
        Box(
            modifier = Modifier.width(width + (1.2 * fontSize.value).dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "=",
                fontSize = fontSize,
                color = color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(width)
                    .height((fontSize * 1.5f).value.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onClick() }
                    .drawBehind {
                        val arrowSize = 12.dp.toPx()
                        val y = size.height - 0.75f * arrowSize
                        val start = arrowSize / 2
                        val end = size.width - arrowSize / 2

                        drawLine(
                            color = color,
                            start = Offset(start + arrowSize / 2, y),
                            end = Offset(end - arrowSize / 2, y),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        val arrowPath = Path().apply {
                            if (isRightToLeft) {
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
                        drawPath(path = arrowPath, color = color)
                    }
            ) {
                Text(
                    text = input,
                    fontSize = fontSize,
                    modifier = Modifier.align(Alignment.Center),
                    color = if (isCorrect == true) green else if (isCorrect == false) red else color
                )
            }
        }
    }
}