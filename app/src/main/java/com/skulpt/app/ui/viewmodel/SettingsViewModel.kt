package com.skulpt.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.skulpt.app.SkulptApplication
import com.skulpt.app.data.model.AppSettings
import com.skulpt.app.data.repository.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as SkulptApplication).database
    private val repository = SettingsRepository(db.appSettingsDao())

    val settings: LiveData<AppSettings?> = repository.settingsLive

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }

    suspend fun getSettingsOnce(): AppSettings = repository.getSettings()
}
