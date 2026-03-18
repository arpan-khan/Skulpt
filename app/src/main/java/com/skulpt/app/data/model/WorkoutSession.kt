package com.skulpt.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayId: Long,
    val dayName: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val totalExercises: Int,
    val completedExercises: Int,
    val totalSets: Int = 0,
    val completedSets: Int = 0,
    val totalReps: Int = 0,
    val completedReps: Int = 0,
    val durationSeconds: Long = 0
) {
    val completionPercent: Int
        get() = if (totalExercises == 0) 0
                else (completedExercises * 100 / totalExercises)
}
