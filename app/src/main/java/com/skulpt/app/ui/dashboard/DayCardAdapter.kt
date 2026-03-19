package com.skulpt.app.ui.dashboard

import android.animation.ObjectAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.skulpt.app.data.model.DayWithExercises
import com.skulpt.app.databinding.ItemDayCardBinding

class DayCardAdapter(
    private val onDayClick: (DayWithExercises) -> Unit,
    private val onDayLongClick: (Long) -> Unit,
    private val onDayDelete: (com.skulpt.app.data.model.WorkoutDay) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<DayCardAdapter.DayViewHolder>() {


    private var days: List<DayWithExercises> = emptyList()

    fun submitDays(newDays: List<DayWithExercises>) {
        val diffResult = DiffUtil.calculateDiff(DayDiffCallback(days, newDays))
        days = newDays.toList()
        diffResult.dispatchUpdatesTo(this)
    }

    fun moveItem(from: Int, to: Int) {
        val mutableList = days.toMutableList()
        val movedItem = mutableList.removeAt(from)
        mutableList.add(to, movedItem)
        days = mutableList
        notifyItemMoved(from, to)
    }

    fun getDays(): List<DayWithExercises> = days

    override fun getItemCount(): Int = days.size

    override fun getItemViewType(position: Int): Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        return DayViewHolder(
            ItemDayCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    inner class DayViewHolder(private val binding: ItemDayCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(dayWithExercises: DayWithExercises) {
            val day = dayWithExercises.day
            binding.tvDayName.text = day.name

            val total = dayWithExercises.totalCount
            val completed = dayWithExercises.completedCount

            binding.tvExerciseCount.text = if (total == 0) "Rest Day" else "$total exercises"
            binding.tvProgress.text = if (total > 0) "$completed / $total" else ""

            val progress = dayWithExercises.completionPercent
            binding.progressSpinner.progress = progress

            // Color accent based on day color and completion
            try {
                val color = Color.parseColor(day.colorHex)
                binding.cardView.strokeColor = if (progress == 100) color else Color.TRANSPARENT
                binding.tvDayName.setTextColor(color)
                binding.progressSpinner.setIndicatorColor(color)
                binding.progressSpinner.trackColor = ColorUtils.setAlphaComponent(color, 20)
            } catch (e: Exception) { /* ignore color parse errors */ }

            // Completion badge and progress text
            binding.ivCompletionBadge.visibility =
                if (progress == 100) android.view.View.VISIBLE else android.view.View.GONE
            
            binding.tvProgress.visibility = 
                if (progress == 100) android.view.View.GONE else android.view.View.VISIBLE

            binding.btnEditDay.setOnClickListener {
                onDayLongClick(day.id)
            }

            binding.btnDeleteDay.setOnClickListener {
                onDayDelete(day)
            }

            binding.root.setOnClickListener {
                animateTap(binding.root)
                onDayClick(dayWithExercises)
            }
            binding.root.setOnLongClickListener {
                onDayLongClick(day.id)
                true
            }

            binding.ivDragHandle.setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                }
                false
            }
        }
    }

    private fun animateTap(view: android.view.View) {
        ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.96f, 1f).setDuration(150).start()
        ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.96f, 1f).setDuration(150).start()
    }

    class DayDiffCallback(
        private val old: List<DayWithExercises>,
        private val new: List<DayWithExercises>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int) =
            old[oldPos].day.id == new[newPos].day.id

        override fun areContentsTheSame(oldPos: Int, newPos: Int) =
            old[oldPos] == new[newPos]
    }
}
