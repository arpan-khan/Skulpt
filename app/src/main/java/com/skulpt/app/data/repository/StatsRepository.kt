package com.skulpt.app.data.repository

import com.skulpt.app.data.model.WorkoutSession
import java.util.Calendar
import java.util.concurrent.TimeUnit

class StatsRepository(private val workoutRepository: WorkoutRepository) {

    data class ActivityPoint(val label: String, val count: Int)
    data class HeatmapPoint(val dateMillis: Long, val intensity: Int)

    data class StatsData(
        val totalWorkouts: Int,
        val totalExCompleted: Int,
        val totalSetsCompleted: Int,
        val totalRepsCompleted: Int,
        val totalTimeSeconds: Long,
        val currentStreak: Int,
        val longestStreak: Int,
        val workoutsThisWeek: Int,
        val workoutsThisMonth: Int,
        val consistencyPercent: Int,
        val heatmapData: Map<Long, Int>,
        val recentSessions: List<WorkoutSession>,
        val weeklyActivity: List<ActivityPoint>,
        val exerciseDistribution: Map<String, Int>
    )

    suspend fun computeStats(): StatsData {
        val sessions = workoutRepository.getAllSessionsOnce()
            .sortedByDescending { it.dateMillis }

        val totalWorkouts = sessions.size
        val totalExCompleted = sessions.sumOf { it.completedExercises }
        val totalSetsCompleted = sessions.sumOf { it.completedSets }
        val totalRepsCompleted = sessions.sumOf { it.completedReps }
        val totalTimeSeconds = sessions.sumOf { it.durationSeconds }

        val (currentStreak, longestStreak) = calculateStreaks(sessions)
        
        // Heatmap: Map of DayStartMillis -> Count
        val activeDaysMap = sessions.groupBy { normalizeToDay(it.dateMillis) }
            .mapValues { (_, sList) -> sList.size }
        
        val weeklyActivity = calculateWeeklyActivity(sessions)
        
        val now = Calendar.getInstance()
        val workoutsThisWeek = sessions.count { 
            val sessionCal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            sessionCal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) &&
            sessionCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        }
        val workoutsThisMonth = sessions.count {
            val sessionCal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            sessionCal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
            sessionCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        }

        // Consistency: % of scheduled days worked out in the last 30 days
        val consistencyPercent = calculateConsistency(sessions)

        val distribution = sessions.groupBy { it.dayName }
            .mapValues { (_, sList) -> sList.sumOf { it.completedExercises } }
            .filterValues { it > 0 }

        return StatsData(
            totalWorkouts = totalWorkouts,
            totalExCompleted = totalExCompleted,
            totalSetsCompleted = totalSetsCompleted,
            totalRepsCompleted = totalRepsCompleted,
            totalTimeSeconds = totalTimeSeconds,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            workoutsThisWeek = workoutsThisWeek,
            workoutsThisMonth = workoutsThisMonth,
            consistencyPercent = consistencyPercent,
            heatmapData = activeDaysMap,
            recentSessions = sessions.take(10),
            weeklyActivity = weeklyActivity,
            exerciseDistribution = distribution
        )
    }

    private fun calculateWeeklyActivity(sessions: List<WorkoutSession>): List<ActivityPoint> {
        val cal = Calendar.getInstance()
        val activity = mutableListOf<ActivityPoint>()
        val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        // Last 7 days
        for (i in 6 downTo 0) {
            val d = Calendar.getInstance()
            d.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = normalizeToDay(d.timeInMillis)
            val dayEnd = dayStart + TimeUnit.DAYS.toMillis(1)
            
            val count = sessions.count { it.dateMillis in dayStart until dayEnd }
            val label = dayLabels[d.get(Calendar.DAY_OF_WEEK) - 1]
            activity.add(ActivityPoint(label, count))
        }
        return activity
    }

    private fun calculateStreaks(sessions: List<WorkoutSession>): Pair<Int, Int> {
        if (sessions.isEmpty()) return 0 to 0

        // Get unique workout dates as Calendar objects
        val workoutDays = sessions
            .map { 
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.dateMillis
                normalizeToDay(cal)
                cal
            }
            .distinctBy { it.timeInMillis }
            .sortedByDescending { it.timeInMillis }

        val today = Calendar.getInstance()
        normalizeToDay(today)
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

        // Current streak: consecutive days ending today or yesterday
        var currentStreak = 0
        var expectedPrev = today.clone() as Calendar

        if (workoutDays.isNotEmpty() && (workoutDays[0].timeInMillis == today.timeInMillis || workoutDays[0].timeInMillis == yesterday.timeInMillis)) {
            expectedPrev = workoutDays[0].clone() as Calendar
            currentStreak = 1
            for (i in 1 until workoutDays.size) {
                expectedPrev.add(Calendar.DAY_OF_YEAR, -1)
                if (workoutDays[i].timeInMillis == expectedPrev.timeInMillis) {
                    currentStreak++
                } else break
            }
        }

        // Longest streak
        var longestStreak = 0
        var runStreak = 1
        val sortedAsc = workoutDays.reversed()
        for (i in 1 until sortedAsc.size) {
            val expectedNext = (sortedAsc[i - 1].clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            if (sortedAsc[i].timeInMillis == expectedNext.timeInMillis) {
                runStreak++
                if (runStreak > longestStreak) longestStreak = runStreak
            } else {
                runStreak = 1
            }
        }
        if (sortedAsc.isNotEmpty() && longestStreak == 0) longestStreak = 1
        return currentStreak to longestStreak
    }

    // Removed calculateHeatmap since UI renders it from raw mapped data.

    private suspend fun calculateConsistency(sessions: List<WorkoutSession>): Int {
        val totalScheduledDays = workoutRepository.getDayCount()
        if (totalScheduledDays == 0) return 0
        
        // Active days in the list of sessions (unique days)
        val activeDaysSet = sessions.map { normalizeToDay(it.dateMillis) }.toSet()
        
        // For simplicity, consistency is (%) = (Sessions in last 30 days) / (Scheduled Days * 4.3) 
        // Or better: (Days worked out in last 30 days) / (Total Scheduled Days in 30 days)
        // Let's assume the user has N scheduled days per week.
        val daysIn30Days = 30
        val scheduledDaysPerMonth = totalScheduledDays * 4 // approx
        
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)
        
        val sessionsInLast30 = activeDaysSet.count { it >= thirtyDaysAgo }
        
        return if (scheduledDaysPerMonth == 0) 0 
               else (sessionsInLast30 * 100 / scheduledDaysPerMonth).coerceIn(0, 100)
    }

    private fun normalizeToDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        normalizeToDay(cal)
        return cal.timeInMillis
    }

    private fun normalizeToDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }
}
