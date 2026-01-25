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
import android.widget.Toast
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.helddertierwelt.mentalmath.presentation.component.settings.menu.NotificationMode
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleNotificationWork(context: Context, notificationMode: NotificationMode) {

        val workManager = WorkManager.getInstance(context)
        if (notificationMode == NotificationMode.OFF) {
            workManager.cancelAllWorkByTag("mental_math_reminder_tag")
            return
        }


        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val repeatInterval: Long = when (notificationMode) {
            NotificationMode.DAILY -> 1L
            NotificationMode.THREEDAILY -> 3L
            NotificationMode.WEEKLY -> 7L
        }
        calendar.add(Calendar.DAY_OF_YEAR, repeatInterval.toInt())
        val delay = calendar.timeInMillis - System.currentTimeMillis()

        val workRequest =
            PeriodicWorkRequestBuilder<NotificationWorker>(repeatInterval, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("mental_math_reminder_tag")
                .build()


        workManager.cancelAllWorkByTag("mental_math_reminder_tag")
        workManager.enqueue(workRequest)
        if(notificationMode == NotificationMode.DAILY){
            Toast.makeText(context, "Reminders will start in the morning after the selected interval.", Toast.LENGTH_SHORT).show()
        }
    }
}