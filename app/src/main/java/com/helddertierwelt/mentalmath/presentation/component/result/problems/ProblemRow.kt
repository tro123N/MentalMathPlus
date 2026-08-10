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

package com.helddertierwelt.mentalmath.presentation.component.result.problems

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout

@Composable
fun ProblemRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->

        require(measurables.size == 2) {
            "ProblemRow expects exactly 2 children"
        }

        val maxWidth = constraints.maxWidth

        val placeableA = measurables[0].measure(constraints)
        val placeableB = measurables[1].measure(constraints)

        val fitsInOneRow =
            placeableA.width + placeableB.width <= maxWidth

        if (fitsInOneRow) {
            val height = maxOf(placeableA.height, placeableB.height)

            layout(width = maxWidth, height = height) {
                val yA = (height - placeableA.height) / 2
                placeableA.placeRelative(0, yA)

                val yB = (height - placeableB.height) / 2
                placeableB.placeRelative(
                    x = maxWidth - placeableB.width,
                    y = yB
                )
            }
        } else {
            val height = placeableA.height + placeableB.height

            layout(width = maxWidth, height = height) {
                placeableA.placeRelative(0, 0)
                placeableB.placeRelative(
                    x = maxWidth - placeableB.width,
                    y = placeableA.height
                )
            }
        }
    }
}