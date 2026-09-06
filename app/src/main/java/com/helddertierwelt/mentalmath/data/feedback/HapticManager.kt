package com.helddertierwelt.mentalmath.data.feedback

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticManager @Inject constructor() {

    private var view: View? = null

    fun attach(view: View) {
        this.view = view
    }

    fun detach() {
        view = null
    }

    fun perform(feedback: FeedbackType) {
        view?.performHapticFeedback(
            getHapticFeedback(feedback)
        )
    }

    private fun getHapticFeedback(feedback: FeedbackType): Int {
        return when (feedback) {
            FeedbackType.CORRECT,
            FeedbackType.GOOD,
            FeedbackType.BEST -> confirmOrFallback()

            FeedbackType.SKIPPED,
            FeedbackType.WRONG,
            FeedbackType.BAD -> rejectOrFallback()

            FeedbackType.START -> gestureStartOrFallback()
            FeedbackType.STOP -> gestureEndOrFallback()

            FeedbackType.NUMPAD,
            FeedbackType.BACKSPACE -> HapticFeedbackConstants.KEYBOARD_TAP

            FeedbackType.SLIDER -> HapticFeedbackConstants.CLOCK_TICK

            FeedbackType.ENABLE,
            FeedbackType.DISABLE,
            FeedbackType.TAP,
            FeedbackType.NEUTRAL,
            FeedbackType.OKAY -> HapticFeedbackConstants.CONTEXT_CLICK
        }
    }

    private fun confirmOrFallback(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
    }

    private fun rejectOrFallback(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
    }

    private fun gestureStartOrFallback(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_START
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        }
    }

    private fun gestureEndOrFallback(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_END
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        }
    }
}
