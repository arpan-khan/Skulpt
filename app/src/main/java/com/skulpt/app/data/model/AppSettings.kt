package com.skulpt.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,   // singleton row

    // Appearance
    val themeMode: Int = 0,               // 0=System, 1=Light, 2=Dark
    val useDynamicColor: Boolean = true,
    val accentColorHex: String = "#6750A4",

    // Workout Behavior
    val restTimerSeconds: Int = 60,
    val autoScrollExercises: Boolean = true,
    val animationIntensity: Int = 1,       // 0=None, 1=Normal, 2=High

    // Notifications
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,

    // Media
    val showExerciseImages: Boolean = true,
    val defaultImageQuery: String = "gym,exercise",

    // Session
    val autoMarkCompleted: Boolean = false,

    // Advanced / WebView
    val customUserAgent: String = "",
    val webViewHardwareAcceleration: Boolean = true
)
