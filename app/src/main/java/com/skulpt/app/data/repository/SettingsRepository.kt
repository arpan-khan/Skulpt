package com.skulpt.app.data.repository

import androidx.lifecycle.LiveData
import com.skulpt.app.data.dao.AppSettingsDao
import com.skulpt.app.data.model.AppSettings

class SettingsRepository(private val dao: AppSettingsDao) {

    val settingsLive: LiveData<AppSettings?> = dao.getSettingsLive()

    suspend fun getSettings(): AppSettings = dao.getSettings() ?: AppSettings()

    suspend fun saveSettings(settings: AppSettings) = dao.upsertSettings(settings)
}
