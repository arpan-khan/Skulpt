package com.skulpt.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.skulpt.app.SkulptApplication
import com.skulpt.app.data.model.Exercise
import com.skulpt.app.data.model.WorkoutSession
import com.skulpt.app.data.repository.WorkoutRepository
import kotlinx.coroutines.launch

class CustomDayViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as SkulptApplication).database
    private val repository = WorkoutRepository(
        db.workoutDayDao(), db.exerciseDao(), db.workoutSessionDao()
    )

    // In-memory only — not stored in DB
    private val _exercises = MutableLiveData<List<Exercise>>(emptyList())
    val exercises: LiveData<List<Exercise>> = _exercises

    private val _sessionSaved = MutableLiveData(false)
    val sessionSaved: LiveData<Boolean> = _sessionSaved

    private var idCounter = -1L  // negative IDs = in-memory only

    fun addExercise(name: String, sets: Int, reps: Int) {
        val current = _exercises.value?.toMutableList() ?: mutableListOf()
        val exercise = Exercise(
            id = idCounter--,
            dayId = -1L,
            name = name,
            sets = sets,
            reps = reps,
            orderIndex = current.size
        )
        current.add(exercise)
        _exercises.value = current
    }

    fun toggleCompleted(exerciseId: Long) {
        val current = _exercises.value?.map { ex ->
            if (ex.id == exerciseId) ex.copy(isCompleted = !ex.isCompleted) else ex
        } ?: return
        _exercises.value = current
    }

    fun removeExercise(exerciseId: Long) {
        val current = _exercises.value?.filter { it.id != exerciseId } ?: return
        _exercises.value = current
    }

    fun saveSession() {
        viewModelScope.launch {
            val exList = _exercises.value ?: emptyList()
            val session = WorkoutSession(
                dayId = -1L,
                dayName = "Custom",
                totalExercises = exList.size,
                completedExercises = exList.count { it.isCompleted }
            )
            repository.insertSession(session)
            _sessionSaved.postValue(true)
        }
    }

    val completedCount: Int get() = _exercises.value?.count { it.isCompleted } ?: 0
    val totalCount: Int get() = _exercises.value?.size ?: 0
}
