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

import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.helddertierwelt.mentalmath.R


class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, "mental_math")
            .setContentTitle("Mental Math: Reminder")
            .setContentText("Take a moment for your mental math practice.")
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setLargeIcon(Icon.createWithResource(applicationContext, R.drawable.ic_launcher))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setDefaults(0)
            .build()

        notificationManager.notify(0, notification)

        return Result.success()
    }

}