package com.skulpt.app.ui.editor

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.skulpt.app.data.model.Exercise
import com.skulpt.app.databinding.DialogAddExerciseBinding

class AddExerciseDialog(
    private val existingExercise: Exercise?,
    private val onConfirm: (name: String, sets: Int, reps: Int, notes: String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAddExerciseBinding.inflate(LayoutInflater.from(requireContext()))

        existingExercise?.let { ex ->
            binding.etName.setText(ex.name)
            binding.etSets.setText(ex.sets.toString())
            binding.etReps.setText(ex.reps.toString())
            binding.etNotes.setText(ex.notes)
        }

        val title = if (existingExercise != null) "Edit Exercise" else "Add Exercise"

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton("Save", null) // set in onStart to access button
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btn.setOnClickListener {
                val name = binding.etName.text.toString().trim()
                val sets = binding.etSets.text.toString().toIntOrNull() ?: 3
                val reps = binding.etReps.text.toString().toIntOrNull() ?: 10
                val notes = binding.etNotes.text.toString().trim()

                if (name.isEmpty()) {
                    binding.tilName.error = "Name is required"
                    return@setOnClickListener
                }
                if (sets < 1) {
                    binding.tilSets.error = "Min 1 set"
                    return@setOnClickListener
                }
                if (reps < 1) {
                    binding.tilReps.error = "Min 1 rep"
                    return@setOnClickListener
                }

                onConfirm(name, sets, reps, notes)
                dialog.dismiss()
            }
        }

        return dialog
    }
}
