package com.skulpt.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.skulpt.app.SkulptApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Re-schedule alarm after boot
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = SkulptApplication.instance.database
                        val settings = db.appSettingsDao().getSettings()
                        if (settings?.remindersEnabled == true) {
                            NotificationHelper.scheduleReminder(context, settings)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            else -> {
                // Alarm fired - show notification
                NotificationHelper.showReminder(context)
                // Re-schedule for next day
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = SkulptApplication.instance.database
                        val settings = db.appSettingsDao().getSettings()
                        if (settings?.remindersEnabled == true) {
                            NotificationHelper.scheduleReminder(context, settings)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
