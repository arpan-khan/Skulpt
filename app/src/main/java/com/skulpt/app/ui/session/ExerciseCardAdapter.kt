package com.skulpt.app.ui.session

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.skulpt.app.R
import com.skulpt.app.data.model.Exercise
import com.skulpt.app.databinding.ItemExerciseCardBinding
import com.skulpt.app.util.PlaceholderUtil

class ExerciseCardAdapter(
    private val onCheckToggle: (Exercise) -> Unit,
    private val onImageClick: (Exercise) -> Unit,
    private val onTimerClick: (Exercise) -> Unit
) : RecyclerView.Adapter<ExerciseCardAdapter.ExerciseViewHolder>() {

    private var exercises: List<Exercise> = emptyList()
    private var baseQuery: String = "workout,exercise"
    private var showImages: Boolean = true

    fun setShowImages(show: Boolean) {
        if (showImages != show) {
            showImages = show
            notifyDataSetChanged()
        }
    }

    fun setBaseQuery(query: String) {
        if (baseQuery != query) {
            baseQuery = query
            notifyDataSetChanged() // Re-bind images with new query
        }
    }

    fun submitExercises(newList: List<Exercise>) {
        val diff = DiffUtil.calculateDiff(ExerciseDiffCallback(exercises, newList))
        exercises = newList
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = exercises.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        return ExerciseViewHolder(
            ItemExerciseCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(exercises[position], position)
    }

    inner class ExerciseViewHolder(private val binding: ItemExerciseCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: Exercise, position: Int) {
            binding.tvExerciseName.text = exercise.name
            binding.tvSetsReps.text = "${exercise.sets} sets × ${exercise.reps} reps"
            binding.tvNotes.text = exercise.notes
            binding.tvNotes.visibility = if (exercise.notes.isNotEmpty()) View.VISIBLE else View.GONE

            // Completion state
            if (exercise.isCompleted) {
                binding.tvExerciseName.paintFlags =
                    binding.tvExerciseName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.root.alpha = 0.55f
                binding.checkBox.isChecked = true
            } else {
                binding.tvExerciseName.paintFlags =
                    binding.tvExerciseName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.root.alpha = 1f
                binding.checkBox.isChecked = false
            }

            // Accent Color (Text)
            try {
                val colorInt = android.graphics.Color.parseColor(exercise.hexcolor)
                binding.tvExerciseName.setTextColor(colorInt)
            } catch (e: Exception) {
                // Default color if parsing fails
                binding.tvExerciseName.setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(binding.tvExerciseName, com.google.android.material.R.attr.colorOnSurface)
                )
            }

            // Image
            val imageUri = exercise.imageUri
            val displayImage = if (!imageUri.isNullOrEmpty()) imageUri else PlaceholderUtil.getDynamicImageUrl(exercise.name, baseQuery)

            if (showImages) {
                binding.ivExercise.visibility = View.VISIBLE
                Glide.with(binding.ivExercise.context)
                    .load(displayImage)
                    .placeholder(PlaceholderUtil.getPlaceholderDrawable(binding.root.context, exercise.name))
                    .error(PlaceholderUtil.getPlaceholderDrawable(binding.root.context, exercise.name))
                    .centerCrop()
                    .into(binding.ivExercise)
            } else {
                binding.ivExercise.visibility = View.GONE
            }

            binding.ivExercise.setOnClickListener { onImageClick(exercise) }

            // Timer indicator
            binding.tvTimer.visibility =
                if (exercise.timerSeconds > 0) View.VISIBLE else View.GONE
            if (exercise.timerSeconds > 0) {
                binding.tvTimer.text = "${exercise.timerSeconds}s rest"
            }
            binding.tvTimer.setOnClickListener {
                if (exercise.timerSeconds > 0) {
                    onTimerClick(exercise)
                }
            }

            // Staggered entrance animation
            binding.root.translationY = 60f
            binding.root.alpha = 0f
            binding.root.animate()
                .translationY(0f)
                .alpha(if (exercise.isCompleted) 0.55f else 1f)
                .setStartDelay((position * 50L).coerceAtMost(300L))
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .start()

            binding.checkBox.setOnClickListener {
                onCheckToggle(exercise)
                if (!exercise.isCompleted) {
                    // Check animation
                    val scaleX = ObjectAnimator.ofFloat(binding.root, "scaleX", 1f, 1.03f, 1f)
                    val scaleY = ObjectAnimator.ofFloat(binding.root, "scaleY", 1f, 1.03f, 1f)
                    AnimatorSet().apply {
                        playTogether(scaleX, scaleY)
                        duration = 200
                        start()
                    }
                }
            }
        }
    }

    class ExerciseDiffCallback(
        private val old: List<Exercise>,
        private val new: List<Exercise>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int) =
            old[oldPos].id == new[newPos].id
        override fun areContentsTheSame(oldPos: Int, newPos: Int) =
            old[oldPos] == new[newPos]
    }
}
