package com.skulpt.app.ui.custom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.skulpt.app.databinding.FragmentCustomDayBinding
import com.skulpt.app.ui.editor.AddExerciseDialog
import com.skulpt.app.ui.viewmodel.CustomDayViewModel

class CustomDayFragment : Fragment() {

    private var _binding: FragmentCustomDayBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CustomDayViewModel by viewModels()
    private lateinit var adapter: CustomExerciseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomDayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CustomExerciseAdapter(
            onCheckToggle = { id -> viewModel.toggleCompleted(id) },
            onRemove = { id ->
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("Remove this exercise?")
                    .setPositiveButton("Remove") { _, _ -> viewModel.removeExercise(id) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.recyclerExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CustomDayFragment.adapter
        }

        viewModel.exercises.observe(viewLifecycleOwner) { exercises ->
            adapter.submitList(exercises)
            val total = exercises.size
            val done = exercises.count { it.isCompleted }
            binding.progressBar.progress = if (total == 0) 0 else done * 100 / total
            binding.tvProgress.text = "$done / $total"

            binding.tvEmptyState.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAddExercise.setOnClickListener {
            AddExerciseDialog(null) { name, sets, reps, notes, timerSeconds ->
                viewModel.addExercise(name, sets, reps, notes, timerSeconds)
            }.show(childFragmentManager, "AddCustomExercise")
        }

        binding.btnFinish.setOnClickListener {
            if (viewModel.totalCount == 0) {
                Toast.makeText(requireContext(), "Add at least one exercise", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.saveSession()
        }

        viewModel.sessionSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Toast.makeText(requireContext(), "Custom workout saved!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
