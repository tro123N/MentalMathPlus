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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.helddertierwelt.mentalmath.MainActivity
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.component.settings.menu.NotificationMode

private const val CHANNEL_ID = "mental_math"
private const val CHANNEL_NAME = "Mental Math Reminders"

class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        createNotificationChannel()
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Reminder")
            .setContentText("Take a moment to practice mental math + − × ÷")
            .setSmallIcon(R.drawable.icon_notification_monochrome)
            .setColor(Color(0xFF3C3F41).toArgb())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .setLights(0, 0, 0)
            .build()

        notificationManager.notify(0, notification)

        val repeatInterval = inputData.getInt("repeat_interval_days", 0)
        val anchorTime = inputData.getLong("anchor_time", 0L)
        NotificationScheduler.scheduleNotificationWork(
            applicationContext,
            repeatIntervalToMode(repeatInterval),
            anchorTime
        )

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for mental math reminders"
            }

            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun repeatIntervalToMode(repeatInterval: Int): NotificationMode {
        return when (repeatInterval) {
            1 -> NotificationMode.DAILY
            3 -> NotificationMode.THREEDAILY
            7 -> NotificationMode.WEEKLY
            else -> NotificationMode.OFF
        }
    }
}