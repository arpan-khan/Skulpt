package com.skulpt.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skulpt.app.R
import com.skulpt.app.databinding.FragmentDashboardBinding
import com.skulpt.app.ui.editor.WorkoutEditorActivity
import com.skulpt.app.ui.viewmodel.DashboardViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: DayCardAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var isDragging = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DayCardAdapter(
            onDayClick = { dayWithEx ->
                val bundle = bundleOf("dayId" to dayWithEx.day.id)
                findNavController().navigate(R.id.action_dashboard_to_workoutSession, bundle)
            },
            onDayLongClick = { dayId ->
                navigateToEditor(dayId)
            },
            onDayDelete = { day ->
                showDeleteConfirmDialog(day)
            },
            onStartDrag = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            }
        )

        itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    isDragging = true
                }
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition

                adapter.moveItem(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                isDragging = false

                val currentDays = (binding.recyclerDays.adapter as? DayCardAdapter)?.getDays() ?: return
                viewModel.saveDayOrder(currentDays)
            }
        })

        binding.recyclerDays.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DashboardFragment.adapter
            setHasFixedSize(false)
            itemTouchHelper.attachToRecyclerView(this)
        }

        viewModel.allDaysWithExercises.observe(viewLifecycleOwner) { days ->
            if (!isDragging) {
                adapter.submitDays(days)
            }
        }

        binding.fabAddDay.setOnClickListener {
            showAddDayDialog()
        }

        binding.fabResetProgressDashboard.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset Daily Progress?")
                .setMessage("This will set all workouts to 0 completion for today. Your exercise data and stats remain safe. Proceed?")
                .setPositiveButton("Reset") { _, _ ->
                    viewModel.resetAllWorkoutProgress()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showAddDayDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "e.g. Leg Day, Push Day"
        val padding = (16 * resources.displayMetrics.density).toInt()
        val container = android.widget.FrameLayout(requireContext())
        container.addView(input)
        input.setPadding(padding, padding, padding, padding)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Workout Day")
            .setMessage("Enter a name for your new workout day")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.addDay(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmDialog(day: com.skulpt.app.data.model.WorkoutDay) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete ${day.name}?")
            .setMessage("Are you sure? This will hide the exercises for this day (they won't be deleted from the database).")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDay(day)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToEditor(dayId: Long) {
        val intent = Intent(requireContext(), WorkoutEditorActivity::class.java)
        intent.putExtra(WorkoutEditorActivity.EXTRA_DAY_ID, dayId)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
