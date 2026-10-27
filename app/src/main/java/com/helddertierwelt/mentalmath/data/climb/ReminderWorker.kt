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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.helddertierwelt.mentalmath.MainActivity
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.curriculum.Curriculum
import com.helddertierwelt.mentalmath.domain.climb.engine.Progression
import com.helddertierwelt.mentalmath.domain.climb.engine.Reminder
import java.util.TimeZone

/** Zeigt die tägliche Erinnerung an und plant gleich die nächste. */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val context = applicationContext
        val settings = PlusSettingsRepository.readStored(context)
        if (!settings.reminderOn) return Result.success()
        try {
            val progress = ClimbRepository.readStored(context)
            if (Reminder.shouldNotify(progress, ReminderNotification.today(), settings.reminderOnlyIfIdle)) {
                ReminderNotification.show(context)
            }
        } finally {
            ReminderScheduler.schedule(context, settings, replace = true)
        }
        return Result.success()
    }
}

/** Die Mitteilung der Erinnerung. */
object ReminderNotification {

    private const val CHANNEL_ID = "mmp_reminder"
    private const val NOTIFICATION_ID = 4711
    private const val REQUEST_CODE = 4711

    fun today(): Long {
        val now = System.currentTimeMillis()
        return (now + TimeZone.getDefault().getOffset(now)) / Progression.DAY_MS
    }

    /** Dürfen Mitteilungen gezeigt werden? */
    fun allowed(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Mitteilung mit einem Text passend zum aktuellen Fortschritt zeigen. Gibt false zurück, wenn es nicht erlaubt ist. */
    fun show(context: Context): Boolean {
        if (!allowed(context)) return false
        val progress = ClimbRepository.readStored(context)
        val message = Reminder.message(Curriculum.worlds, progress, System.currentTimeMillis(), today())
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Tägliche Erinnerung", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Erinnert einmal am Tag ans Kopfrechnen"
            }
            manager.createNotificationChannel(channel)
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(message.title)
            .setContentText(message.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.text))
            .setSmallIcon(R.drawable.icon_notification_monochrome)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return try {
            manager.notify(NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
