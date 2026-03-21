package com.skulpt.app.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skulpt.app.R
import com.skulpt.app.data.model.WorkoutSession
import com.skulpt.app.databinding.FragmentAllActivityBinding
import com.skulpt.app.databinding.DialogEditSessionBinding
import com.skulpt.app.ui.viewmodel.StatsViewModel

class AllActivityFragment : Fragment() {

    private var _binding: FragmentAllActivityBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatsViewModel by viewModels()
    private lateinit var adapter: AllActivityAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        adapter = AllActivityAdapter(
            onEditClick = { session -> showEditDialog(session) },
            onDeleteClick = { session -> showDeleteConfirm(session) }
        )

        binding.recyclerAllActivity.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AllActivityFragment.adapter
        }

        viewModel.statsData.observe(viewLifecycleOwner) { data ->
            data ?: return@observe

        }

        viewModel.allSessions.observe(viewLifecycleOwner) { sessions: List<WorkoutSession>? ->
            adapter.submitList(sessions)
        }

        binding.fabAddActivity.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showDeleteConfirm(session: WorkoutSession) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Record?")
            .setMessage("Are you sure you want to delete this workout record for ${session.dayName}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSession(session)
                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(session: WorkoutSession) {
        val dialogBinding = DialogEditSessionBinding.inflate(layoutInflater)
        dialogBinding.etName.setText(session.dayName)
        dialogBinding.etSets.setText(session.completedSets.toString())
        dialogBinding.etExercises.setText(session.completedExercises.toString())
        dialogBinding.etDuration.setText((session.durationSeconds / 60).toString())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Session")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogBinding.etName.text.toString().ifBlank { session.dayName }
                val sets = dialogBinding.etSets.text.toString().toIntOrNull() ?: session.completedSets
                val exercises = dialogBinding.etExercises.text.toString().toIntOrNull() ?: session.completedExercises
                val duration = (dialogBinding.etDuration.text.toString().toLongOrNull() ?: (session.durationSeconds / 60)) * 60L

                val updated = session.copy(
                    dayName = name,
                    completedSets = sets,
                    completedExercises = exercises,
                    durationSeconds = duration
                )
                viewModel.updateSession(updated)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddDialog() {

        val dialogBinding = DialogEditSessionBinding.inflate(layoutInflater)
        dialogBinding.etName.setText("Manual Entry")
        dialogBinding.tilSets.hint = "Sets completed"
        dialogBinding.tilExercises.hint = "Exercises completed"
        dialogBinding.tilDuration.hint = "Duration (mins)"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Manual Entry")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val name = dialogBinding.etName.text.toString().ifBlank { "Manual Entry" }
                val sets = dialogBinding.etSets.text.toString().toIntOrNull() ?: 0
                val exercises = dialogBinding.etExercises.text.toString().toIntOrNull() ?: 0
                val duration = (dialogBinding.etDuration.text.toString().toLongOrNull() ?: 0L) * 60L

                val session = WorkoutSession(
                    dayId = -1L,
                    dayName = name,
                    totalExercises = exercises,
                    completedExercises = exercises,
                    totalSets = sets,
                    completedSets = sets,
                    durationSeconds = duration
                )
                viewModel.insertSession(session)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
