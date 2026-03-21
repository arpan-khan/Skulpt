package com.skulpt.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.skulpt.app.SkulptApplication
import com.skulpt.app.data.repository.StatsRepository
import com.skulpt.app.data.repository.WorkoutRepository
import com.skulpt.app.data.model.WorkoutSession
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as SkulptApplication).database
    private val workoutRepo = WorkoutRepository(
        db.workoutDayDao(), db.exerciseDao(), db.workoutSessionDao()
    )
    private val statsRepo = StatsRepository(workoutRepo)

    val totalSessions: LiveData<Int> = workoutRepo.totalSessionCount
    val fullyCompleted: LiveData<Int> = workoutRepo.fullyCompletedCount
    val allSessions: LiveData<List<WorkoutSession>> = workoutRepo.allSessions

    private val _statsData = MutableLiveData<StatsRepository.StatsData>()
    val statsData: LiveData<StatsRepository.StatsData> = _statsData

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _statsData.postValue(statsRepo.computeStats())
        }
    }

    fun insertSession(session: WorkoutSession) {
        viewModelScope.launch {
            workoutRepo.insertSession(session)
            loadStats()
        }
    }

    fun updateSession(session: WorkoutSession) {
        viewModelScope.launch {
            // We need an update method in repo/dao. Let's add it.
            // For now, insert(REPLACE) might work if ID is set.
            workoutRepo.insertSession(session)
            loadStats()
        }
    }

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch {
            workoutRepo.deleteSession(session)
            loadStats()
        }
    }
}
