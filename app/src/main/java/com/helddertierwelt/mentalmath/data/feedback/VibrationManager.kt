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

package com.helddertierwelt.mentalmath.data.feedback

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VibrationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                        as VibratorManager

            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun perform(feedback: FeedbackType) {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(getEffect(feedback))
        } else {
            vibrateLegacy(feedback)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getEffect(feedback: FeedbackType): VibrationEffect {
        return when (feedback) {
            FeedbackType.RESULT_GOOD,
            FeedbackType.RESULT_PERFECT,
            FeedbackType.LINK_GAME_BEST,
            FeedbackType.LINK_GAME_GOOD,
            FeedbackType.CORRECT -> waveform(
                longArrayOf(0, 30, 20, 60),
                intArrayOf(0, 60, 0, 140)
            )

            FeedbackType.RESULT_BAD,
            FeedbackType.LINK_GAME_BAD,
            FeedbackType.WRONG -> waveform(
                longArrayOf(0, 180),
                intArrayOf(0, 120)
            )

            FeedbackType.SLIDER,
            FeedbackType.NUMPAD,
            FeedbackType.BACKSPACE -> oneShot(10, 60)

            FeedbackType.SKIPPED,
            FeedbackType.STOP,
            FeedbackType.START,
            FeedbackType.ENABLE,
            FeedbackType.DISABLE,
            FeedbackType.TAP,
            FeedbackType.RESULT_OKAY,
            FeedbackType.LINK_GAME_NEUTRAL,
            FeedbackType.LINK_GAME_OKAY -> oneShot(20, 80)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun oneShot(
        duration: Long,
        amplitude: Int
    ): VibrationEffect {
        return VibrationEffect.createOneShot(
            duration,
            amplitude
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun waveform(
        timings: LongArray,
        amplitudes: IntArray
    ): VibrationEffect {
        return VibrationEffect.createWaveform(
            timings,
            amplitudes,
            -1
        )
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    @Suppress("DEPRECATION")
    private fun vibrateLegacy(feedback: FeedbackType) {
        val duration = when (feedback) {
            FeedbackType.RESULT_GOOD,
            FeedbackType.RESULT_PERFECT,
            FeedbackType.LINK_GAME_BEST,
            FeedbackType.LINK_GAME_GOOD,
            FeedbackType.CORRECT -> 90L

            FeedbackType.RESULT_BAD,
            FeedbackType.LINK_GAME_BAD,
            FeedbackType.WRONG -> 180L

            FeedbackType.SLIDER,
            FeedbackType.NUMPAD,
            FeedbackType.BACKSPACE -> 10L

            FeedbackType.SKIPPED,
            FeedbackType.STOP,
            FeedbackType.START,
            FeedbackType.ENABLE,
            FeedbackType.DISABLE,
            FeedbackType.TAP,
            FeedbackType.RESULT_OKAY,
            FeedbackType.LINK_GAME_NEUTRAL,
            FeedbackType.LINK_GAME_OKAY -> 20L
        }

        vibrator.vibrate(duration)
    }
}
