package com.skulpt.app.ui.stats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.skulpt.app.data.model.WorkoutSession
import com.skulpt.app.databinding.ItemRecordedSessionBinding
import java.text.SimpleDateFormat
import java.util.*

class AllActivityAdapter(
    private val onEditClick: (WorkoutSession) -> Unit,
    private val onDeleteClick: (WorkoutSession) -> Unit
) : ListAdapter<WorkoutSession, AllActivityAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        return SessionViewHolder(
            ItemRecordedSessionBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(private val binding: ItemRecordedSessionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(session: WorkoutSession) {
            binding.tvDayName.text = session.dayName
            
            val sdf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
            binding.tvDate.text = sdf.format(Date(session.dateMillis))

            val durationMinutes = session.durationSeconds / 60
            binding.tvStatsSummary.text = "${session.completedExercises} exercises • ${session.completedSets} sets • ${durationMinutes}m"

            binding.btnEdit.setOnClickListener { onEditClick(session) }
            binding.btnDelete.setOnClickListener { onDeleteClick(session) }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<WorkoutSession>() {
        override fun areItemsTheSame(oldItem: WorkoutSession, newItem: WorkoutSession) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: WorkoutSession, newItem: WorkoutSession) =
            oldItem == newItem
    }
}
