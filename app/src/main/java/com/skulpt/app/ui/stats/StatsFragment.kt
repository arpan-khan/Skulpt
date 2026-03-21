package com.skulpt.app.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.skulpt.app.databinding.FragmentStatsBinding
import com.skulpt.app.ui.viewmodel.StatsViewModel
import androidx.navigation.fragment.findNavController
import com.skulpt.app.R
import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*
import android.widget.GridLayout
import com.skulpt.app.data.repository.StatsRepository
import android.view.Gravity
import androidx.core.view.setMargins

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()

        viewModel.statsData.observe(viewLifecycleOwner) { data ->
            data ?: return@observe
            binding.tvTotalWorkouts.text = data.totalWorkouts.toString()
            binding.tvTotalExercises.text = data.totalExCompleted.toString()
            binding.tvTotalSets.text = data.totalSetsCompleted.toString()
            binding.tvTotalReps.text = data.totalRepsCompleted.toString()
            
            val hours = data.totalTimeSeconds / 3600
            val mins = (data.totalTimeSeconds % 3600) / 60
            binding.tvTotalTime.text = "${hours}h ${mins}m"
            
            binding.tvConsistency.text = "${data.consistencyPercent}%"
            binding.tvCurrentStreak.text = data.currentStreak.toString()
            binding.tvLongestStreak.text = data.longestStreak.toString()
            binding.tvWorkoutsThisWeek.text = data.workoutsThisWeek.toString()
            binding.tvWorkoutsThisMonth.text = data.workoutsThisMonth.toString()

            renderHeatmap(data.heatmapData)

            val sessionLines = data.recentSessions.take(10).joinToString("\n") { session ->
                val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                    .format(Date(session.dateMillis))
                "$dateStr — ${session.dayName} (${session.completedExercises}/${session.totalExercises})"
            }
            binding.tvRecentSessions.text = sessionLines.ifEmpty { "No sessions yet.\nStart your first workout!" }
            
            fun formatTime(seconds: Long): String {
                val h = seconds / 3600
                val m = (seconds % 3600) / 60
                return if (h > 0) "${h}h ${m}m" else "${m}m"
            }
            
            binding.tvTimeToday.text = formatTime(data.timeTodaySeconds)
            binding.tvLongestSession.text = formatTime(data.longestSessionTimeSeconds)
            binding.tvTimeThisWeek.text = formatTime(data.timeThisWeekSeconds)
            binding.tvTimeThisMonth.text = formatTime(data.timeThisMonthSeconds)
            
            updateCharts(data)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadStats()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.btnViewAllActivity.setOnClickListener {
            findNavController().navigate(R.id.action_stats_to_allActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun setupCharts() {
        binding.pieChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            legend.textColor = if (isDarkMode()) Color.WHITE else Color.GRAY
            holeRadius = 65f
            setHoleColor(Color.TRANSPARENT)
            setDrawEntryLabels(true)
            setEntryLabelColor(if (isDarkMode()) Color.WHITE else Color.BLACK)
            setEntryLabelTextSize(12f)
        }
        binding.barChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.setDrawGridLines(false)
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = if (isDarkMode()) Color.WHITE else Color.GRAY
            axisLeft.setDrawGridLines(false)
            axisLeft.textColor = if (isDarkMode()) Color.WHITE else Color.GRAY
            axisRight.isEnabled = false
        }
    }

    private fun renderHeatmap(activeMap: Map<Long, Int>) {
        val scrollView = binding.gridHeatmap.parent as android.view.View
        scrollView.post {
            val width = scrollView.width
            if (width == 0) return@post

            // Clear and rebuild only when dimensions are known and we're ready to fill
            binding.gridHeatmap.removeAllViews()

            val context = requireContext()
            val density = resources.displayMetrics.density
            val cellSize = (12 * density).toInt()
            val margin = (2 * density).toInt()
            val cellTotalSize = cellSize + margin * 2

            val columns = width / cellTotalSize
            if (columns <= 0) return@post

            binding.gridHeatmap.columnCount = columns
            binding.gridHeatmap.rowCount = 7

            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val todayIndex = todayCal.get(Calendar.DAY_OF_WEEK) - 1
            val daysToOffset = 6 - todayIndex
            val totalCells = columns * 7
            
            val startCal = todayCal.clone() as Calendar
            startCal.add(Calendar.DAY_OF_YEAR, -(totalCells - 1 - daysToOffset))
            
            for (i in 0 until totalCells) {
                val currentCal = startCal.clone() as Calendar
                currentCal.add(Calendar.DAY_OF_YEAR, i)
                val currentMillis = currentCal.timeInMillis
                
                val count = activeMap[currentMillis] ?: 0
                val isFuture = currentMillis > todayCal.timeInMillis
                
                val view = View(context).apply {
                    val baseParams = android.view.ViewGroup.LayoutParams(cellSize, cellSize)
                    val params = GridLayout.LayoutParams(baseParams).apply {
                        setMargins(margin, margin, margin, margin)
                    }
                    layoutParams = params
                    
                    val color = when {
                        isFuture -> Color.TRANSPARENT
                        count == 0 -> if (isDarkMode()) Color.parseColor("#2D2D2D") else Color.parseColor("#EBEDF0")
                        count == 1 -> Color.parseColor("#C6E48B")
                        count == 2 -> Color.parseColor("#7BC96F")
                        count == 3 -> Color.parseColor("#239A3B")
                        else -> Color.parseColor("#196127")
                    }
                    setBackgroundColor(color)
                }
                binding.gridHeatmap.addView(view)
            }
        }
    }

    private fun updateCharts(data: StatsRepository.StatsData) {
        // Pie Chart - Exercise Distribution by Day
        val entries = data.exerciseDistribution.map { (name, count) ->
            PieEntry(count.toFloat(), name)
        }
        val colors = listOf(
            Color.parseColor("#9279D1"), // Lavender
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#FFC107")  // Amber
        )
        val pieDataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            setDrawValues(true)
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            sliceSpace = 3f
        }
        binding.pieChart.apply {
            this.data = PieData(pieDataSet)
            setEntryLabelColor(if (isDarkMode()) Color.WHITE else Color.DKGRAY)
            invalidate()
        }

        // Bar Chart
        val barEntries = data.weeklyActivity.mapIndexed { index, point ->
            BarEntry(index.toFloat(), point.count.toFloat())
        }
        val barDataSet = BarDataSet(barEntries, "Workouts").apply {
            color = Color.parseColor("#6750A4")
            setDrawValues(false)
        }
        binding.barChart.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(data.weeklyActivity.map { it.label })
            xAxis.labelCount = data.weeklyActivity.size
            barDataSet.valueTextColor = if (isDarkMode()) Color.WHITE else Color.BLACK
            this.data = BarData(barDataSet).apply {
                setValueTextColor(if (isDarkMode()) Color.WHITE else Color.BLACK)
            }
            invalidate()
        }
    }

    private fun isDarkMode(): Boolean {
        val nightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}
