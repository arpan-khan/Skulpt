package com.skulpt.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.skulpt.app.data.model.WorkoutSession

@Dao
interface WorkoutSessionDao {

    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis DESC")
    fun getAllSessionsLive(): LiveData<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis DESC LIMIT 30")
    suspend fun getRecentSessions(): List<WorkoutSession>

    @Query("SELECT COUNT(*) FROM workout_sessions")
    fun getTotalCountLive(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE completedExercises >= totalExercises AND totalExercises > 0")
    fun getFullyCompletedCountLive(): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis DESC")
    suspend fun getAllSessions(): List<WorkoutSession>

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllSessions()
}
