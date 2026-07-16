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

    private val sounds = mapOf(
        SoundType.CORRECT to soundPool.load(context, R.raw.correct1, 1),
        SoundType.WRONG to soundPool.load(context, R.raw.wrong1, 1),
        SoundType.SKIPPED to soundPool.load(context, R.raw.skip3, 1),
        SoundType.CLICK to soundPool.load(context, R.raw.click1_disable, 1),
        SoundType.CANCEL to soundPool.load(context, R.raw.wrong5, 1),
        )

    fun play(sound: SoundType) {
        sounds[sound]?.let {
            soundPool.play(it, 1f, 1f, 1, 0, 1f)
        }
    }
}