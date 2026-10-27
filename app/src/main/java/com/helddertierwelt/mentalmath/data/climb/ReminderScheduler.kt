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

package com.helddertierwelt.mentalmath.data.climb

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.helddertierwelt.mentalmath.domain.climb.model.PlusSettings
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Plant die tägliche Erinnerung von Mental Math Plus. */
object ReminderScheduler {

    private const val WORK_NAME = "mmp_daily_reminder"

    /**
     * Erinnerung für den nächsten Termin einplanen oder (wenn ausgeschaltet) entfernen.
     * [replace] = false lässt einen schon geplanten Termin unverändert (z. B. beim App-Start).
     */
    fun schedule(context: Context, settings: PlusSettings, replace: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (!settings.reminderOn) {
            manager.cancelUniqueWork(WORK_NAME)
            return
        }
        val delay = nextTrigger(System.currentTimeMillis(), settings.reminderHour, settings.reminderMinute) - System.currentTimeMillis()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .build()
        manager.enqueueUniqueWork(
            WORK_NAME,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Nächster Zeitpunkt (ms) mit der Uhrzeit [hour]:[minute], frühestens eine Minute nach [now]. */
    fun nextTrigger(now: Long, hour: Int, minute: Int): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= now + 60_000L) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }
}
