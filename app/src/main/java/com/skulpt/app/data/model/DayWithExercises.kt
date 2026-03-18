package com.skulpt.app.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class DayWithExercises(
    @Embedded val day: WorkoutDay,
    @Relation(
        parentColumn = "id",
        entityColumn = "dayId"
    )
    val exercises: List<Exercise>
) {
    val completedCount: Int get() = exercises.count { it.isCompleted }
    val totalCount: Int get() = exercises.size
    val completionPercent: Int
        get() = if (totalCount == 0) 0 else (completedCount * 100 / totalCount)
}
