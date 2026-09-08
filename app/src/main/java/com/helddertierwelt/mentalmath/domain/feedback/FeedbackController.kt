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

package com.helddertierwelt.mentalmath.domain.feedback

import com.helddertierwelt.mentalmath.data.feedback.SoundManager
import com.helddertierwelt.mentalmath.data.feedback.VibrationManager
import com.helddertierwelt.mentalmath.data.repository.SettingsRepository
import com.helddertierwelt.mentalmath.domain.settings.FeedbackMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackController @Inject constructor(
    private val soundManager: SoundManager,
    private val vibrationManager: VibrationManager,
    settingsRepository: SettingsRepository
) {

    private var feedbackMode = FeedbackMode.FEEDBACK_OFF

    init {
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.settingsFlow.collect {
                feedbackMode = it.feedbackMode
            }
        }
    }

    fun setMode(mode: FeedbackMode) {
        feedbackMode = mode
    }

    fun play(feedback: FeedbackType) {
        when (feedbackMode) {
            FeedbackMode.FEEDBACK_OFF -> Unit
            FeedbackMode.SOUND -> soundManager.play(feedback)
            FeedbackMode.VIBRATION -> vibrationManager.perform(feedback)
        }
    }
}