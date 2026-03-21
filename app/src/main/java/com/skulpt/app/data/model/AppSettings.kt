package com.skulpt.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,

    val themeMode: Int = 0,
    val useDynamicColor: Boolean = true,
    val accentColorHex: String = "#6750A4",

    val restTimerSeconds: Int = 60,
    val autoScrollExercises: Boolean = true,
    val animationIntensity: Int = 1,

    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,

    val showExerciseImages: Boolean = true,
    val defaultImageQuery: String = "gym,exercise",

    val autoMarkCompleted: Boolean = false,

    val customUserAgent: String = "",
    val webViewHardwareAcceleration: Boolean = true
)
