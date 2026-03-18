package com.skulpt.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.skulpt.app.R
import com.skulpt.app.data.model.AppSettings
import com.skulpt.app.ui.MainActivity
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_ID = "skulpt_reminders"
    const val NOTIFICATION_ID = 1001
    private const val REQUEST_CODE = 2001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily workout reminder notifications"
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun showReminder(context: Context) {
        createNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to Sculpt!")
            .setContentText("Your workout is waiting. Let's crush it today!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    fun scheduleReminder(context: Context, settings: AppSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = getReminderIntent(context)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.reminderHour)
            set(Calendar.MINUTE, settings.reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If time has passed today, schedule for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    intent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    intent
                )
            }
        } catch (e: SecurityException) {
            // Exact alarm not permitted on some devices - fall back to inexact
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, intent)
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(getReminderIntent(context))
    }

    private fun getReminderIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
