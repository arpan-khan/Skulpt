package com.skulpt.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDay::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dayId")]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayId: Long,
    val name: String,
    val sets: Int = 3,
    val reps: Int = 10,
    val imageUri: String? = null,
    val orderIndex: Int = 0,
    val isCompleted: Boolean = false,
    val completedSets: Int = 0,
    val lastTrackedSets: Int = 0,
    val timerSeconds: Int = 0,
    val hexcolor: String = "#6750A4",
    val notes: String = ""
)
