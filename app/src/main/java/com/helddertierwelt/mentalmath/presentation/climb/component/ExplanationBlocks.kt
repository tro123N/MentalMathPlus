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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.model.Block
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.yellow

/** Erklärung einer Methode: Texte, Schritte, Formeln, Tipps und durchgerechnete Beispiele. */
@Composable
fun ExplanationBlocks(blocks: List<Block>) {
    val dimens = MentalMathTheme.dimens
    val mono = monoFont()
    Column {
        blocks.forEach { block ->
            when (block) {
                is Block.Text -> {
                    BodyText(block.text)
                    Spacer(Modifier.height(10.dp))
                }

                is Block.Steps -> {
                    block.items.forEachIndexed { i, item ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(
                                text = "${i + 1}.",
                                fontSize = dimens.fonts.sideSheetEntry,
                                color = accent,
                                modifier = Modifier.width(28.dp),
                            )
                            BodyText(item, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                is Block.Formula -> {
                    Text(
                        text = block.text,
                        fontFamily = mono,
                        fontSize = dimens.fonts.sideSheetEntry,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(dimens.components.cornerRadius))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                }

                is Block.Tip -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(yellow.copy(alpha = 0.12f), RoundedCornerShape(dimens.components.cornerRadius))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lightbulb_24dp),
                            contentDescription = "Tipp",
                            tint = yellow,
                            modifier = Modifier.size(dimens.icons.problemStatus),
                        )
                        Spacer(Modifier.width(10.dp))
                        BodyText(block.text, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }

                is Block.Example -> {
                    val p = block.problem
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(dimens.components.cornerRadius))
                            .padding(12.dp),
                    ) {
                        CaptionText("Beispiel")
                        p.prompt.caption?.let { CaptionText(it) }
                        p.prompt.lines.forEach { line ->
                            Text(
                                text = line,
                                fontFamily = mono,
                                fontSize = dimens.fonts.sideSheetEntry,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Text(
                            text = p.prompt.text,
                            fontSize = dimens.fonts.settingsLabel,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        SolutionSteps(problem = p, showHints = true)
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}
