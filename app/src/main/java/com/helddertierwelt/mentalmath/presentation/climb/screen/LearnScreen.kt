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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionType
import com.helddertierwelt.mentalmath.domain.climb.model.Block
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import com.helddertierwelt.mentalmath.presentation.climb.component.ActionButton
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.CaptionText
import com.helddertierwelt.mentalmath.presentation.climb.component.CardGap
import com.helddertierwelt.mentalmath.presentation.climb.component.CardTitle
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.climb.component.SolutionSteps
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.yellow

/** Erklärung einer Methode in eigenen Worten, mit durchgerechneten Beispielen. */
@Composable
fun LearnScreen(navController: NavController, skillId: String) {
    val dimens = MentalMathTheme.dimens
    val owner = LocalActivity.current as ViewModelStoreOwner
    val viewModel: ClimbViewModel = hiltViewModel(owner)
    val skill = viewModel.skill(skillId)
    val mono = FontFamily(Font(R.font.roboto_mono_regular_mental_math, FontWeight.Normal))

    fun back() {
        viewModel.playTap()
        if (!navController.popBackStack()) navController.navigate("climb") { launchSingleTop = true }
    }
    BackHandler { back() }

    Scaffold(
        topBar = { ClimbTopBar(title = skill?.title ?: "Methode", onNavigation = { back() }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("learnColumn")
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            if (skill == null) {
                BodyText("Dieses Level gibt es nicht.")
            } else {
            ClimbCard {
                CardTitle(skill.method)
                CaptionText("Im Buch: ${skill.book.label}")
                Spacer(Modifier.height(10.dp))
                skill.explanation.forEach { block ->
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
                                        color = blue,
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
            CardGap()
            ActionButton(
                text = "Jetzt geführt üben",
                subtitle = "Die Methode Schritt für Schritt anwenden",
                icon = R.drawable.list_numbered_24dp,
                iconTint = blue,
                onClick = {
                    if (viewModel.startSession(SessionType.GUIDED, skill.id)) navController.navigate("climbplay")
                },
            )
            Spacer(Modifier.height(24.dp))
            }
        }
    }
}
