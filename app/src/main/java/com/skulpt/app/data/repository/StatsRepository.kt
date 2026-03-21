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
        val timeTodaySeconds: Long,
        val longestSessionTimeSeconds: Long,
        val timeThisWeekSeconds: Long,
        val timeThisMonthSeconds: Long,
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

        if (sessions.isEmpty()) {
            return StatsData(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, emptyMap(), emptyList(), emptyList(), emptyMap())
        }

        var totalExCompleted = 0
        var totalSetsCompleted = 0
        var totalRepsCompleted = 0
        var totalTimeSeconds = 0L

        val activeDaysMap = mutableMapOf<Long, Int>()
        val cal = Calendar.getInstance()
        val now = Calendar.getInstance()
        val nowMillis = now.timeInMillis
        val todayStart = normalizeToDay(nowMillis)

        val currentYear = now.get(Calendar.YEAR)
        val currentWeek = now.get(Calendar.WEEK_OF_YEAR)
        val currentMonth = now.get(Calendar.MONTH)

        var timeTodaySeconds = 0L
        var longestSessionTimeSeconds = 0L
        var timeThisWeekSeconds = 0L
        var timeThisMonthSeconds = 0L
        var workoutsThisWeek = 0
        var workoutsThisMonth = 0

        val distribution = mutableMapOf<String, Int>()

        sessions.forEach {
            totalExCompleted += it.completedExercises
            totalSetsCompleted += it.completedSets
            totalRepsCompleted += it.completedReps
            totalTimeSeconds += it.durationSeconds

            val dayStart = normalizeToDay(it.dateMillis)
            activeDaysMap[dayStart] = (activeDaysMap[dayStart] ?: 0) + 1

            cal.timeInMillis = it.dateMillis
            val dur = it.durationSeconds
            if (dur > longestSessionTimeSeconds) longestSessionTimeSeconds = dur

            if (dayStart == todayStart) timeTodaySeconds += dur

            if (cal.get(Calendar.YEAR) == currentYear) {
                if (cal.get(Calendar.WEEK_OF_YEAR) == currentWeek) {
                    workoutsThisWeek++
                    timeThisWeekSeconds += dur
                }
                if (cal.get(Calendar.MONTH) == currentMonth) {
                    workoutsThisMonth++
                    timeThisMonthSeconds += dur
                }
            }

            if (it.completedExercises > 0) {
                distribution[it.dayName] = (distribution[it.dayName] ?: 0) + it.completedExercises
            }
        }

        val streaks = calculateStreaks(sessions)
        val weeklyActivity = calculateWeeklyActivity(sessions)
        val consistencyPercent = calculateConsistency(sessions)

        val tenDaysAgo = nowMillis - TimeUnit.DAYS.toMillis(10)
        val filteredRecentSessions = sessions.filter { it.dateMillis >= tenDaysAgo }

        return StatsData(
            totalWorkouts = sessions.size,
            totalExCompleted = totalExCompleted,
            totalSetsCompleted = totalSetsCompleted,
            totalRepsCompleted = totalRepsCompleted,
            totalTimeSeconds = totalTimeSeconds,
            timeTodaySeconds = timeTodaySeconds,
            longestSessionTimeSeconds = longestSessionTimeSeconds,
            timeThisWeekSeconds = timeThisWeekSeconds,
            timeThisMonthSeconds = timeThisMonthSeconds,
            currentStreak = streaks.first,
            longestStreak = streaks.second,
            workoutsThisWeek = workoutsThisWeek,
            workoutsThisMonth = workoutsThisMonth,
            consistencyPercent = consistencyPercent,
            heatmapData = activeDaysMap,
            recentSessions = filteredRecentSessions,
            weeklyActivity = weeklyActivity,
            exerciseDistribution = distribution
        )
    }

    private fun calculateWeeklyActivity(sessions: List<WorkoutSession>): List<ActivityPoint> {
        val cal = Calendar.getInstance()
        val activity = mutableListOf<ActivityPoint>()
        val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        for (i in 6 downTo 0) {
            cal.apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dayStart = normalizeToDay(cal.timeInMillis)
            val dayEnd = dayStart + TimeUnit.DAYS.toMillis(1)

            val count = sessions.filter { it.dateMillis in dayStart until dayEnd }.sumOf { it.completedSets }
            val label = dayLabels[cal.get(Calendar.DAY_OF_WEEK) - 1]
            activity.add(ActivityPoint(label, count))
        }
        return activity
    }

    private fun calculateStreaks(sessions: List<WorkoutSession>): Pair<Int, Int> {
        if (sessions.isEmpty()) return 0 to 0

        val cal = Calendar.getInstance()
        val workoutDays = sessions
            .map {
                cal.timeInMillis = it.dateMillis
                normalizeToDay(cal.timeInMillis)
            }
            .distinct()
            .sortedByDescending { it }

        val today = normalizeToDay(System.currentTimeMillis())
        val yesterday = today - TimeUnit.DAYS.toMillis(1)

        var currentStreak = 0
        val firstDay = workoutDays.getOrNull(0)
        if (firstDay != null && (firstDay == today || firstDay == yesterday)) {
            currentStreak = 1
            var lastDay: Long = firstDay
            for (i in 1 until workoutDays.size) {
                if (workoutDays[i] == lastDay - TimeUnit.DAYS.toMillis(1)) {
                    currentStreak++
                    lastDay = workoutDays[i]
                } else {
                    break
                }
            }
        }

        var longestStreak = 0
        var runStreak = 1
        val sortedAsc = workoutDays.sorted()
        for (i in 1 until sortedAsc.size) {
            if (sortedAsc[i] == sortedAsc[i - 1] + TimeUnit.DAYS.toMillis(1)) {
                runStreak++
            } else {
                if (runStreak > longestStreak) longestStreak = runStreak
                runStreak = 1
            }
        }
        if (runStreak > longestStreak) longestStreak = runStreak
        if (sortedAsc.isNotEmpty() && longestStreak == 0) longestStreak = 1
        return currentStreak to longestStreak
    }

    private suspend fun calculateConsistency(sessions: List<WorkoutSession>): Int {
        val totalScheduledDays = workoutRepository.getDayCount()
        if (totalScheduledDays == 0) return 0

        val activeDaysSet = sessions.map { normalizeToDay(it.dateMillis) }.toSet()

        val daysIn30Days = 30
        val scheduledDaysPerMonth = totalScheduledDays * 4

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
