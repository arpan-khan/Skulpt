package com.skulpt.app.data.repository

import androidx.lifecycle.LiveData
import com.skulpt.app.data.dao.ExerciseDao
import com.skulpt.app.data.dao.WorkoutDayDao
import com.skulpt.app.data.dao.WorkoutSessionDao
import com.skulpt.app.data.model.DayWithExercises
import com.skulpt.app.data.model.Exercise
import com.skulpt.app.data.model.WorkoutDay
import com.skulpt.app.data.model.WorkoutSession

class WorkoutRepository(
    private val dayDao: WorkoutDayDao,
    private val exerciseDao: ExerciseDao,
    private val sessionDao: WorkoutSessionDao
) {

    // Days
    val allDaysWithExercises: LiveData<List<DayWithExercises>> =
        dayDao.getAllDaysWithExercisesLive()

    fun getDayWithExercisesLive(dayId: Long): LiveData<DayWithExercises> =
        dayDao.getDayWithExercisesLive(dayId)

    suspend fun getDayWithExercises(dayId: Long): DayWithExercises? =
        dayDao.getDayWithExercises(dayId)

    suspend fun insertDay(day: WorkoutDay): Long = dayDao.insertDay(day)
    suspend fun updateDay(day: WorkoutDay) = dayDao.updateDay(day)
    suspend fun deleteDay(day: WorkoutDay) = dayDao.deleteDay(day)

    // Exercises
    fun getExercisesForDayLive(dayId: Long): LiveData<List<Exercise>> =
        exerciseDao.getExercisesForDayLive(dayId)

    suspend fun getExercisesForDay(dayId: Long): List<Exercise> =
        exerciseDao.getExercisesForDay(dayId)

    suspend fun insertExercise(exercise: Exercise): Long =
        exerciseDao.insertExercise(exercise)

    suspend fun insertExercises(exercises: List<Exercise>) =
        exerciseDao.insertExercises(exercises)

    suspend fun updateExercise(exercise: Exercise) =
        exerciseDao.updateExercise(exercise)

    suspend fun deleteExercise(exercise: Exercise) =
        exerciseDao.deleteExercise(exercise)

    suspend fun deleteAllExercisesForDay(dayId: Long) =
        exerciseDao.deleteAllExercisesForDay(dayId)

    suspend fun setExerciseCompleted(exerciseId: Long, completed: Boolean) =
        exerciseDao.setCompleted(exerciseId, completed)

    suspend fun resetDayCompletion(dayId: Long) =
        exerciseDao.resetCompletionForDay(dayId)

    suspend fun updateExerciseOrder(exerciseId: Long, orderIndex: Int) =
        exerciseDao.updateOrderIndex(exerciseId, orderIndex)

    suspend fun updateExerciseImage(exerciseId: Long, imageUri: String?) =
        exerciseDao.updateExerciseImage(exerciseId, imageUri)

    // Sessions
    val allSessions: LiveData<List<WorkoutSession>> = sessionDao.getAllSessionsLive()
    val totalSessionCount: LiveData<Int> = sessionDao.getTotalCountLive()
    val fullyCompletedCount: LiveData<Int> = sessionDao.getFullyCompletedCountLive()

    suspend fun insertSession(session: WorkoutSession): Long =
        sessionDao.insertSession(session)

    suspend fun getAllSessionsOnce(): List<WorkoutSession> =
        sessionDao.getAllSessions()

    suspend fun deleteAllSessions() = sessionDao.deleteAllSessions()

    suspend fun getDayCount(): Int = dayDao.getDayCount()
}
