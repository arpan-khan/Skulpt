package com.skulpt.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_days")
data class WorkoutDay(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayIndex: Int,
    val name: String,
    val colorHex: String = "#6750A4"
)
