package com.skulpt.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.skulpt.app.data.model.Exercise

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE dayId = :dayId ORDER BY orderIndex ASC")
    fun getExercisesForDayLive(dayId: Long): LiveData<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE dayId = :dayId ORDER BY orderIndex ASC")
    suspend fun getExercisesForDay(dayId: Long): List<Exercise>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): Exercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("DELETE FROM exercises WHERE dayId = :dayId")
    suspend fun deleteAllExercisesForDay(dayId: Long)

    @Query("UPDATE exercises SET isCompleted = :completed, completedSets = CASE WHEN :completed = 1 THEN sets ELSE completedSets END WHERE id = :exerciseId")
    suspend fun setCompleted(exerciseId: Long, completed: Boolean)

    @Query("UPDATE exercises SET completedSets = :sets, isCompleted = CASE WHEN :sets >= sets THEN 1 ELSE 0 END WHERE id = :exerciseId")
    suspend fun updateCompletedSets(exerciseId: Long, sets: Int)

    @Query("UPDATE exercises SET lastTrackedSets = :sets WHERE id = :exerciseId")
    suspend fun updateLastTrackedSets(exerciseId: Long, sets: Int)

    @Query("UPDATE exercises SET isCompleted = 0, completedSets = 0, lastTrackedSets = 0 WHERE dayId = :dayId")
    suspend fun resetCompletionForDay(dayId: Long)

    @Query("UPDATE exercises SET isCompleted = 0, completedSets = 0, lastTrackedSets = 0")
    suspend fun resetAllCompletion()

    @Query("UPDATE exercises SET orderIndex = :orderIndex WHERE id = :exerciseId")
    suspend fun updateOrderIndex(exerciseId: Long, orderIndex: Int)

    @Query("UPDATE exercises SET imageUri = :imageUri WHERE id = :exerciseId")
    suspend fun updateExerciseImage(exerciseId: Long, imageUri: String?)

    @Query("SELECT COUNT(*) FROM exercises WHERE dayId = :dayId AND isCompleted = 1")
    suspend fun getCompletedCount(dayId: Long): Int

    @Query("SELECT COUNT(*) FROM exercises WHERE dayId = :dayId")
    suspend fun getTotalCount(dayId: Long): Int
}
