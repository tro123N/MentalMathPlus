/*
Mental Math - Android app for practicing mental arithmetic
Copyright (C) 2025 HeldDerTierwelt

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

package com.helddertierwelt.mentalmath.util

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.helddertierwelt.mentalmath.presentation.component.settings.menu.NotificationMode
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val MENTAL_MATH_REMINDER_TAG = "mental_math_reminder_tag"

object NotificationScheduler {

    fun scheduleNotificationWork(context: Context, notificationMode: NotificationMode, anchorTime: Long) {

        if (notificationMode == NotificationMode.OFF) {
            WorkManager.getInstance(context).cancelAllWorkByTag(MENTAL_MATH_REMINDER_TAG)
            return
        }

        val repeatInterval: Int = notificationMode.intervalDays
        val triggerAt = nextTriggerTime(anchorTime, repeatInterval)
        val delay = triggerAt - System.currentTimeMillis()

        val workRequest =
            OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        "anchor_time" to anchorTime,
                        "repeat_interval_days" to repeatInterval
                    )
                )
                .addTag(MENTAL_MATH_REMINDER_TAG)
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "mental_math_reminder",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelNotificationWork(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(MENTAL_MATH_REMINDER_TAG)
    }

    private fun nextTriggerTime(anchorTimeMillis: Long, intervalDays: Int): Long {
        val now = System.currentTimeMillis()
        if (now < anchorTimeMillis) return anchorTimeMillis

        val intervalMillis = intervalDays * 24L * 60 * 60 * 1000
        val passedIntervals = (now - anchorTimeMillis) / intervalMillis + 1
        return anchorTimeMillis + passedIntervals * intervalMillis
    }

     fun calculateAnchorTime(hour: Int, notificationMode: NotificationMode): Long {
        val anchor = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, notificationMode.intervalDays)
        }

        return anchor.timeInMillis
    }
}