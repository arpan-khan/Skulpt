package com.skulpt.app.ui.custom

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.skulpt.app.data.model.Exercise
import com.skulpt.app.databinding.ItemCustomExerciseBinding

class CustomExerciseAdapter(
    private val onCheckToggle: (Long) -> Unit,
    private val onRemove: (Long) -> Unit
) : ListAdapter<Exercise, CustomExerciseAdapter.ViewHolder>(ExerciseDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemCustomExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCustomExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: Exercise) {
            binding.tvName.text = exercise.name
            binding.tvSetsReps.text = "${exercise.sets} × ${exercise.reps}"

            if (exercise.isCompleted) {
                binding.tvName.paintFlags =
                    binding.tvName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.root.alpha = 0.55f
            } else {
                binding.tvName.paintFlags =
                    binding.tvName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.root.alpha = 1f
            }

            binding.checkBox.isChecked = exercise.isCompleted
            binding.checkBox.setOnClickListener { onCheckToggle(exercise.id) }
            binding.btnRemove.setOnClickListener { onRemove(exercise.id) }
        }
    }

    class ExerciseDiff : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(a: Exercise, b: Exercise) = a.id == b.id
        override fun areContentsTheSame(a: Exercise, b: Exercise) = a == b
    }
}
