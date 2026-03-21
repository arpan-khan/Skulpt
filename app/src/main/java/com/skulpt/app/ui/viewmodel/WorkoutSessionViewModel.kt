package com.skulpt.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.skulpt.app.SkulptApplication
import com.skulpt.app.data.model.DayWithExercises
import com.skulpt.app.data.model.Exercise
import com.skulpt.app.data.model.WorkoutSession
import com.skulpt.app.data.repository.WorkoutRepository
import kotlinx.coroutines.launch

class WorkoutSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as SkulptApplication).database
    private val repository = WorkoutRepository(
        db.workoutDayDao(), db.exerciseDao(), db.workoutSessionDao()
    )
    private val settingsRepository = com.skulpt.app.data.repository.SettingsRepository(db.appSettingsDao())

    private var dayId: Long = -1L
    private var sessionStartMillis: Long = 0L

    private lateinit var _dayWithExercises: LiveData<DayWithExercises>
    val dayWithExercises: LiveData<DayWithExercises> get() = _dayWithExercises
    val settings: LiveData<com.skulpt.app.data.model.AppSettings?> = settingsRepository.settingsLive

    private val _sessionSaved = MutableLiveData(false)
    val sessionSaved: LiveData<Boolean> = _sessionSaved

    private val _isSessionActive = MutableLiveData(false)
    val isSessionActive: LiveData<Boolean> = _isSessionActive

    private val _elapsedTimeSeconds = MutableLiveData(0L)
    val elapsedTimeSeconds: LiveData<Long> = _elapsedTimeSeconds

    private var timerJob: kotlinx.coroutines.Job? = null

    fun initialize(dayId: Long) {
        if (this.dayId == dayId) return
        this.dayId = dayId
        _dayWithExercises = repository.getDayWithExercisesLive(dayId)
    }

    fun startSession(shouldReset: Boolean) {
        if (_isSessionActive.value == true) return
        
        if (shouldReset) {
            _elapsedTimeSeconds.value = 0L
            resetSession()
        }

        _isSessionActive.value = true
        sessionStartMillis = System.currentTimeMillis() - (_elapsedTimeSeconds.value ?: 0L) * 1000L
        
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                _elapsedTimeSeconds.postValue((System.currentTimeMillis() - sessionStartMillis) / 1000)
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun stopSession() {
        _isSessionActive.value = false
        timerJob?.cancel()
    }

    fun toggleExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.setExerciseCompleted(exercise.id, !exercise.isCompleted)
        }
    }

    fun incrementExerciseSet(exercise: Exercise) {
        viewModelScope.launch {
            val nextSets = if (exercise.completedSets < exercise.sets) exercise.completedSets + 1 else 0
            repository.updateExerciseCompletedSets(exercise.id, nextSets)
        }
    }

    fun updateExerciseImage(exerciseId: Long, imageUri: String?) {
        viewModelScope.launch {
            repository.updateExerciseImage(exerciseId, imageUri)
        }
    }

    fun saveSession(day: DayWithExercises) {
        viewModelScope.launch {
            val durationSeconds = _elapsedTimeSeconds.value ?: 0L
            
            val totalSets = day.exercises.sumOf { it.sets }
            // Calculate NEWLY completed sets since last track
            val newCompletedSets = day.exercises.sumOf { (it.completedSets - it.lastTrackedSets).coerceAtLeast(0) }
            
            val totalReps = day.exercises.sumOf { it.sets * it.reps }
            val newCompletedReps = day.exercises.sumOf { 
                val newSets = (it.completedSets - it.lastTrackedSets).coerceAtLeast(0)
                newSets * it.reps
            }
            val newCompletedExercises = day.exercises.count { it.completedSets > it.lastTrackedSets }

            if (newCompletedSets <= 0 && day.completedCount <= 0) {
                // Nothing new to track, just return or handle as needed
            }

            val session = WorkoutSession(
                dayId = day.day.id,
                dayName = day.day.name,
                totalExercises = day.totalCount,
                completedExercises = newCompletedExercises,
                totalSets = totalSets,
                completedSets = newCompletedSets,
                totalReps = totalReps,
                completedReps = newCompletedReps,
                durationSeconds = durationSeconds
            )
            repository.insertSession(session)

            // Update lastTrackedSets for all exercises
            day.exercises.forEach {
                repository.updateExerciseLastTrackedSets(it.id, it.completedSets)
            }

            _sessionSaved.postValue(true)
        }
    }

    fun resetSession() {
        viewModelScope.launch {
            repository.resetDayCompletion(dayId)
        }
    }
}
