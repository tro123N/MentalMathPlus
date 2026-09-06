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

import android.content.Context
import android.media.SoundPool
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .build()

    private data class Sound(
        val resourceId: Int,
        val volume: Float = 1f
    )

    private val sounds = mapOf(
        FeedbackType.TAP to Sound(soundPool.load(context, R.raw.tap, 1), volume = 1f),
        FeedbackType.ENABLE to Sound(soundPool.load(context, R.raw.enable, 1), volume = 1f),
        FeedbackType.DISABLE to Sound(soundPool.load(context, R.raw.disable, 1), volume = 1f),
        FeedbackType.SLIDER to Sound(soundPool.load(context, R.raw.slider, 1), volume = 1f),
        FeedbackType.START to Sound(soundPool.load(context, R.raw.start, 1), volume = 1f),
        FeedbackType.NUMPAD to Sound(soundPool.load(context, R.raw.numpad, 1), volume = 1f),
        FeedbackType.BACKSPACE to Sound(soundPool.load(context, R.raw.backspace, 1), volume = 1f),
        FeedbackType.CORRECT to Sound(soundPool.load(context, R.raw.correct, 1), volume = 1f),
        FeedbackType.SKIPPED to Sound(soundPool.load(context, R.raw.skip, 1), volume = 1f),
        FeedbackType.WRONG to Sound(soundPool.load(context, R.raw.wrong, 1), volume = 1f),
        FeedbackType.STOP to Sound(soundPool.load(context, R.raw.stop, 1), volume = 1f),
        FeedbackType.NEUTRAL to Sound(soundPool.load(context, R.raw.neutral, 1), volume = 1f),
        FeedbackType.BAD to Sound(soundPool.load(context, R.raw.bad, 1), volume = 1f),
        FeedbackType.OKAY to Sound(soundPool.load(context, R.raw.okay, 1), volume = 1f),
        FeedbackType.GOOD to Sound(soundPool.load(context, R.raw.good, 1), volume = 1f),
        FeedbackType.BEST to Sound(soundPool.load(context, R.raw.best, 1), volume = 1f),
    )

    fun play(sound: FeedbackType) {
        sounds[sound]?.let {
            soundPool.play(
                it.resourceId,
                it.volume,
                it.volume,
                1,
                0,
                1f
            )
        }
    }
}