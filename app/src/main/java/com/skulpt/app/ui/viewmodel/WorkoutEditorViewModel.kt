package com.skulpt.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.skulpt.app.SkulptApplication
import com.skulpt.app.data.model.Exercise
import com.skulpt.app.data.repository.WorkoutRepository
import kotlinx.coroutines.launch

class WorkoutEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as SkulptApplication).database
    val repository = WorkoutRepository(
        db.workoutDayDao(), db.exerciseDao(), db.workoutSessionDao()
    )

    private var dayId: Long = -1L

    private val _exercises = MutableLiveData<List<Exercise>>()
    val exercises: LiveData<List<Exercise>> = _exercises

    private val _dayName = MutableLiveData<String>()
    val dayName: LiveData<String> = _dayName

    private val _defaultImageQuery = MutableLiveData<String>()
    val defaultImageQuery: LiveData<String> = _defaultImageQuery

    private val _dayColor = MutableLiveData<String>()
    val dayColor: LiveData<String> = _dayColor

    fun loadDay(dayId: Long) {
        this.dayId = dayId
        viewModelScope.launch {
            val settings = db.appSettingsDao().getSettings()
            _defaultImageQuery.postValue(settings?.defaultImageQuery ?: "")
            
            val dayWithExercises = repository.getDayWithExercises(dayId)
            _dayName.postValue(dayWithExercises?.day?.name ?: "")
            _dayColor.postValue(dayWithExercises?.day?.colorHex ?: "#6750A4")
            _exercises.postValue(dayWithExercises?.exercises?.sortedBy { it.orderIndex } ?: emptyList())
        }
    }

    fun addExercise(name: String, sets: Int, reps: Int, notes: String = "", timerSeconds: Int = 0) {
        viewModelScope.launch {
            val currentList = _exercises.value ?: emptyList()
            val newOrder = currentList.size
            val exercise = Exercise(
                dayId = dayId,
                name = name,
                sets = sets,
                reps = reps,
                orderIndex = newOrder,
                timerSeconds = timerSeconds,
                notes = notes
            )
            val id = repository.insertExercise(exercise)
            val updated = currentList + exercise.copy(id = id)
            _exercises.postValue(updated)
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.updateExercise(exercise)
            val current = _exercises.value?.toMutableList() ?: return@launch
            val idx = current.indexOfFirst { it.id == exercise.id }
            if (idx >= 0) {
                current[idx] = exercise
                _exercises.postValue(current.toList())
            }
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
            val current = _exercises.value?.toMutableList() ?: return@launch
            current.remove(exercise)
            reindexAndSave(current)
        }
    }

    fun duplicateExercise(exercise: Exercise) {
        viewModelScope.launch {
            val current = _exercises.value?.toMutableList() ?: return@launch
            val copy = exercise.copy(id = 0, orderIndex = current.size)
            val newId = repository.insertExercise(copy)
            current.add(copy.copy(id = newId))
            _exercises.postValue(current.toList())
        }
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        val current = _exercises.value?.toMutableList() ?: return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _exercises.value = current.toList()
    }

    fun moveExerciseStateOnly(fromIndex: Int, toIndex: Int) {
        val current = _exercises.value?.toMutableList() ?: return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        // We still update the LiveData but the Activity will skip submitList if isDragging is true
        _exercises.value = current.toList()
    }

    fun saveOrder() {
        viewModelScope.launch {
            val current = _exercises.value ?: return@launch
            reindexAndSave(current.toMutableList())
        }
    }

    private suspend fun reindexAndSave(list: MutableList<Exercise>) {
        list.forEachIndexed { index, exercise ->
            val updated = exercise.copy(orderIndex = index)
            repository.updateExercise(updated)
        }
        _exercises.postValue(list.mapIndexed { i, e -> e.copy(orderIndex = i) })
    }

    fun updateDayName(newName: String) {
        viewModelScope.launch {
            val day = repository.getDayWithExercises(dayId)?.day ?: return@launch
            if (day.name != newName) {
                repository.updateDay(day.copy(name = newName))
                _dayName.postValue(newName)
            }
        }
    }

    fun updateDayColor(newColor: String) {
        viewModelScope.launch {
            val day = repository.getDayWithExercises(dayId)?.day ?: return@launch
            if (day.colorHex != newColor) {
                repository.updateDay(day.copy(colorHex = newColor))
                _dayColor.postValue(newColor)
            }
        }
    }

    fun updateExerciseImage(exerciseId: Long, imageUri: String?) {
        viewModelScope.launch {
            val exercise = repository.getExercisesForDay(dayId).firstOrNull { it.id == exerciseId }
                ?: return@launch
            repository.updateExercise(exercise.copy(imageUri = imageUri))
            loadDay(dayId)
        }
    }
}
