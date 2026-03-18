package com.skulpt.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.skulpt.app.SkulptApplication
import com.skulpt.app.data.model.DayWithExercises
import com.skulpt.app.data.repository.WorkoutRepository
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as SkulptApplication).database
    private val repository = WorkoutRepository(
        db.workoutDayDao(), db.exerciseDao(), db.workoutSessionDao()
    )

    val allDaysWithExercises: LiveData<List<DayWithExercises>> =
        repository.allDaysWithExercises

    fun addDay(name: String) {
        viewModelScope.launch {
            val count = repository.getDayCount()
            repository.insertDay(
                com.skulpt.app.data.model.WorkoutDay(
                    name = name,
                    dayIndex = count
                )
            )
        }
    }

    fun deleteDay(day: com.skulpt.app.data.model.WorkoutDay) {
        viewModelScope.launch {
            repository.deleteDay(day)
        }
    }

    fun resetDayCompletion(dayId: Long) {
        viewModelScope.launch {
            repository.resetDayCompletion(dayId)
        }
    }

    fun moveDay(fromIndex: Int, toIndex: Int) {
        val current = allDaysWithExercises.value?.toMutableList() ?: return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        // Note: LiveData is backed by Room, so it will refresh when we save to DB.
        // For smoother UI we rely on the Activity skipping submitList during drag.
    }

    fun saveDayOrder(newList: List<DayWithExercises>) {
        viewModelScope.launch {
            newList.forEachIndexed { index, dayWithExercises ->
                val updated = dayWithExercises.day.copy(dayIndex = index)
                repository.updateDay(updated)
            }
        }
    }
}
