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

package com.helddertierwelt.mentalmath.data.sound

import android.content.Context
import android.media.SoundPool
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.sound.SoundType
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
        SoundType.ENABLE to Sound(soundPool.load(context, R.raw.enable, 1), volume = 1f),
        SoundType.DISABLE to Sound(soundPool.load(context, R.raw.disable, 1), volume = 1f),
        SoundType.SLIDER to Sound(soundPool.load(context, R.raw.slider, 1), volume = 1f),
        SoundType.START to Sound(soundPool.load(context, R.raw.start, 1), volume = 1f),
        SoundType.NUMPAD to Sound(soundPool.load(context, R.raw.numpad, 1), volume = 1f),
        SoundType.BACKSPACE to Sound(soundPool.load(context, R.raw.backspace, 1), volume = 1f),
        SoundType.CORRECT to Sound(soundPool.load(context, R.raw.correct, 1), volume = 1f),
        SoundType.SKIPPED to Sound(soundPool.load(context, R.raw.skip, 1), volume = 1f),
        SoundType.WRONG to Sound(soundPool.load(context, R.raw.wrong, 1), volume = 1f),
        SoundType.STOP to Sound(soundPool.load(context, R.raw.stop, 1), volume = 1f),
        SoundType.NEUTRAL to Sound(soundPool.load(context, R.raw.neutral, 1), volume = 1f),
        SoundType.BAD to Sound(soundPool.load(context, R.raw.bad, 1), volume = 1f),
        SoundType.OKAY to Sound(soundPool.load(context, R.raw.okay, 1), volume = 1f),
        SoundType.GOOD to Sound(soundPool.load(context, R.raw.good, 1), volume = 1f),
        SoundType.BEST to Sound(soundPool.load(context, R.raw.best, 1), volume = 1f),
    )

    fun play(sound: SoundType) {
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