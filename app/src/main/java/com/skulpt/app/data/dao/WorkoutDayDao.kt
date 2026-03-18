package com.skulpt.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.skulpt.app.data.model.DayWithExercises
import com.skulpt.app.data.model.WorkoutDay

@Dao
interface WorkoutDayDao {

    @Query("SELECT * FROM workout_days ORDER BY dayIndex ASC")
    fun getAllDaysLive(): LiveData<List<WorkoutDay>>

    @Transaction
    @Query("SELECT * FROM workout_days ORDER BY dayIndex ASC")
    fun getAllDaysWithExercisesLive(): LiveData<List<DayWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_days ORDER BY dayIndex ASC")
    suspend fun getAllDaysWithExercises(): List<DayWithExercises>

    @Query("SELECT * FROM workout_days WHERE dayIndex = :dayIndex LIMIT 1")
    suspend fun getDayByIndex(dayIndex: Int): WorkoutDay?

    @Transaction
    @Query("SELECT * FROM workout_days WHERE id = :dayId")
    fun getDayWithExercisesLive(dayId: Long): LiveData<DayWithExercises>

    @Transaction
    @Query("SELECT * FROM workout_days WHERE id = :dayId")
    suspend fun getDayWithExercises(dayId: Long): DayWithExercises?

    @Query("SELECT * FROM workout_days WHERE id = :dayId")
    suspend fun getDayById(dayId: Long): WorkoutDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: WorkoutDay): Long

    @Update
    suspend fun updateDay(day: WorkoutDay)

    @Delete
    suspend fun deleteDay(day: WorkoutDay)

    @Query("SELECT COUNT(*) FROM workout_days")
    suspend fun getDayCount(): Int
}
