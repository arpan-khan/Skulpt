package com.skulpt.app.ui.editor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.skulpt.app.data.model.Exercise
import com.skulpt.app.databinding.ItemEditorExerciseBinding
import com.skulpt.app.util.PlaceholderUtil

class EditorExerciseAdapter(
    private val onEditClick: (Exercise) -> Unit,
    private val onDeleteClick: (Exercise) -> Unit,
    private val onDuplicateClick: (Exercise) -> Unit,
    private val onImageClick: (Exercise) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : ListAdapter<Exercise, EditorExerciseAdapter.EditorViewHolder>(ExerciseDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditorViewHolder {
        return EditorViewHolder(
            ItemEditorExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: EditorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EditorViewHolder(private val binding: ItemEditorExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: Exercise) {
            binding.tvExerciseName.text = exercise.name
            binding.tvSetsReps.text = "${exercise.sets} sets × ${exercise.reps} reps"
            binding.tvNotes.text = exercise.notes.ifEmpty { "" }

            val imageUri = exercise.imageUri
            val displayImage = if (!imageUri.isNullOrEmpty()) imageUri else PlaceholderUtil.getDynamicImageUrl(exercise.name)

            Glide.with(binding.ivExercise.context)
                .load(displayImage)
                .placeholder(PlaceholderUtil.getPlaceholderDrawable(binding.root.context, exercise.name))
                .error(PlaceholderUtil.getPlaceholderDrawable(binding.root.context, exercise.name))
                .centerCrop()
                .into(binding.ivExercise)

            binding.ivExercise.setOnClickListener { onImageClick(exercise) }
            binding.btnEdit.setOnClickListener { onEditClick(exercise) }
            binding.btnDelete.setOnClickListener { onDeleteClick(exercise) }
            binding.btnDuplicate.setOnClickListener { onDuplicateClick(exercise) }

            // Drag handle visual
            binding.ivDragHandle.setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                }
                false
            }
        }
    }

    class ExerciseDiff : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise) =
            oldItem == newItem
    }
}
