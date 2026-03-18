package com.skulpt.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.skulpt.app.data.db.SkulptDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SkulptApplication : Application() {

    val database: SkulptDatabase by lazy {
        SkulptDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        applySavedTheme()
    }

    private fun applySavedTheme() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = database
            val settings = db.appSettingsDao().getSettings()
            val mode = settings?.themeMode ?: 0
            withContext(Dispatchers.Main) {
                when (mode) {
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }
    }

    companion object {
        lateinit var instance: SkulptApplication
            private set
    }
}
