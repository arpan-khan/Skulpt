package com.skulpt.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_days")
data class WorkoutDay(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayIndex: Int,       // 0=Monday…6=Sunday, 7=Custom
    val name: String,
    val colorHex: String = "#6750A4" // default accent
)
