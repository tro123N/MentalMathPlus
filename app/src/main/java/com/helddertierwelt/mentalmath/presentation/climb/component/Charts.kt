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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt

/** Waagrechte Hilfslinie in einem Liniendiagramm, z. B. das Ziel für einen Stern. */
data class ChartGuide(val value: Float, val label: String, val color: Color)

/**
 * Säulendiagramm, z. B. gelöste Aufgaben pro Tag.
 * [labels] hat dieselbe Länge wie [values]; leere Beschriftungen werden ausgelassen.
 */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    valueText: (Float) -> String = { it.roundToInt().toString() },
) {
    val measurer = rememberTextMeasurer()
    val textColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
    val gridColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
    val labelStyle = TextStyle(fontSize = 10.sp, color = textColor)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        if (values.isEmpty()) return@Canvas
        val top = 18.dp.toPx()
        val bottom = 18.dp.toPx()
        val chartHeight = size.height - top - bottom
        val maxValue = max(values.maxOrNull() ?: 0f, 1f)
        val slot = size.width / values.size
        val barWidth = slot * 0.62f
        // Grundlinie und halbe Höhe
        drawLine(gridColor, Offset(0f, top + chartHeight), Offset(size.width, top + chartHeight), strokeWidth = 1.dp.toPx())
        drawLine(
            gridColor,
            Offset(0f, top + chartHeight / 2),
            Offset(size.width, top + chartHeight / 2),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
        )
        val maxLayout = measurer.measure("max ${valueText(maxValue)}", labelStyle)
        drawText(maxLayout, topLeft = Offset(0f, 0f))
        values.forEachIndexed { i, v ->
            val x = slot * i + (slot - barWidth) / 2
            if (v > 0f) {
                val h = (v / maxValue) * chartHeight
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, top + chartHeight - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                )
            } else {
                drawLine(gridColor, Offset(x, top + chartHeight - 1.dp.toPx()), Offset(x + barWidth, top + chartHeight - 1.dp.toPx()), strokeWidth = 2.dp.toPx())
            }
            val label = labels.getOrNull(i).orEmpty()
            if (label.isNotEmpty()) {
                val layout = measurer.measure(label, labelStyle)
                val lx = (slot * i + slot / 2 - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                drawText(layout, topLeft = Offset(lx, size.height - layout.size.height))
            }
        }
    }
}

/**
 * Liniendiagramm mit Punkten, z. B. die Durchschnittszeit der Prüfungen.
 * [guides] zeichnet gestrichelte Ziel-Linien mit Beschriftung.
 */
@Composable
fun LineChart(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    guides: List<ChartGuide> = emptyList(),
    labels: List<String> = emptyList(),
    height: Dp = 150.dp,
    minY: Float? = 0f,
    valueText: (Float) -> String = { it.roundToInt().toString() },
) {
    val measurer = rememberTextMeasurer()
    val textColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
    val gridColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
    val dotFill = MaterialTheme.colorScheme.primary
    val labelStyle = TextStyle(fontSize = 10.sp, color = textColor)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        if (values.isEmpty()) return@Canvas
        val top = 14.dp.toPx()
        val bottom = if (labels.any { it.isNotEmpty() }) 18.dp.toPx() else 6.dp.toPx()
        val left = 6.dp.toPx()
        val right = 44.dp.toPx()
        val chartHeight = size.height - top - bottom
        val chartWidth = size.width - left - right
        val all = values + guides.map { it.value }
        var lo = minY ?: (all.minOrNull() ?: 0f)
        var hi = all.maxOrNull() ?: 1f
        if (hi - lo < 1e-3f) {
            hi += 1f
            lo -= if (minY == null) 1f else 0f
        }
        hi += (hi - lo) * 0.1f
        fun y(v: Float) = top + chartHeight * (1f - (v - lo) / (hi - lo))
        fun x(i: Int) = if (values.size == 1) left + chartWidth / 2 else left + chartWidth * i / (values.size - 1)

        drawLine(gridColor, Offset(left, top + chartHeight), Offset(left + chartWidth, top + chartHeight), strokeWidth = 1.dp.toPx())
        guides.forEach { g ->
            val gy = y(g.value)
            drawLine(
                g.color.copy(alpha = 0.8f),
                Offset(left, gy),
                Offset(left + chartWidth, gy),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
            val layout = measurer.measure(g.label, labelStyle.copy(color = g.color))
            drawText(layout, topLeft = Offset(left + chartWidth + 4.dp.toPx(), gy - layout.size.height / 2f))
        }
        if (values.size > 1) {
            val path = Path()
            values.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
            drawPath(path, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        }
        values.forEachIndexed { i, v ->
            drawCircle(color, radius = 4.dp.toPx(), center = Offset(x(i), y(v)))
            drawCircle(dotFill, radius = 2.dp.toPx(), center = Offset(x(i), y(v)))
        }
        val last = values.last()
        val lastLayout = measurer.measure(valueText(last), labelStyle.copy(color = color))
        val lx = (x(values.lastIndex) - lastLayout.size.width / 2f).coerceIn(0f, size.width - lastLayout.size.width)
        val ly = (y(last) - lastLayout.size.height - 4.dp.toPx()).coerceAtLeast(0f)
        drawText(lastLayout, topLeft = Offset(lx, ly))
        labels.forEachIndexed { i, label ->
            if (label.isNotEmpty() && i < values.size) {
                val layout = measurer.measure(label, labelStyle)
                val tx = (x(i) - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                drawText(layout, topLeft = Offset(tx, size.height - layout.size.height))
            }
        }
    }
}

/** Waagrechter Balken mit Anteil 0 bis 1. */
@Composable
fun ShareBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(height / 2))
                    .background(color),
            )
        }
    }
}

/** Kleiner Abstand in einer Zeile. */
@Composable
fun RowGap(width: Dp = 10.dp) = Spacer(Modifier.width(width))

/** Zeile aus Beschriftung, Balken und Wert. */
@Composable
fun BarRow(label: String, fraction: Float, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        CaptionText(label, modifier = Modifier.weight(0.42f))
        RowGap(8.dp)
        ShareBar(fraction, color, modifier = Modifier.weight(0.4f))
        RowGap(8.dp)
        CaptionText(value, modifier = Modifier.weight(0.18f))
    }
}
