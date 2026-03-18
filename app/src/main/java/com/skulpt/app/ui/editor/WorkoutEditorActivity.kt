package com.skulpt.app.ui.editor

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skulpt.app.R
import com.skulpt.app.databinding.ActivityWorkoutEditorBinding
import com.skulpt.app.ui.viewmodel.WorkoutEditorViewModel

class WorkoutEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutEditorBinding
    private val viewModel: WorkoutEditorViewModel by viewModels()
    private lateinit var adapter: EditorExerciseAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var isDragging = false

    companion object {
        const val EXTRA_DAY_ID = "extra_day_id"
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Grant persistent permission
            try {
                contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { /* not all URIs support this */ }
            pendingImageExerciseId?.let { exId ->
                viewModel.updateExerciseImage(exId, it.toString())
            }
        }
        pendingImageExerciseId = null
    }

    private var pendingImageExerciseId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val dayId = intent.getLongExtra(EXTRA_DAY_ID, -1L)
        if (dayId == -1L) {
            finish()
            return
        }

        viewModel.loadDay(dayId)

        adapter = EditorExerciseAdapter(
            onEditClick = { exercise -> showEditDialog(exercise) },
            onDeleteClick = { exercise ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Exercise")
                    .setMessage("Remove \"${exercise.name}\"?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteExercise(exercise) }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onDuplicateClick = { exercise -> viewModel.duplicateExercise(exercise) },
            onImageClick = { exercise ->
                showImageActions(exercise)
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
                
                // Locally swap items in the adapter's list for immediate visual feedback
                val list = adapter.currentList.toMutableList()
                val movedItem = list.removeAt(from)
                list.add(to, movedItem)
                // We use notifyItemMoved for the animation, but we also update the ViewModel's state
                viewModel.moveExerciseStateOnly(from, to)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                isDragging = false
                viewModel.saveOrder()
            }
        })

        binding.recyclerExercises.apply {
            layoutManager = LinearLayoutManager(this@WorkoutEditorActivity)
            adapter = this@WorkoutEditorActivity.adapter
            itemTouchHelper.attachToRecyclerView(this)
        }

        viewModel.exercises.observe(this) { exercises ->
            if (!isDragging) {
                adapter.submitList(exercises)
            }
        }

        binding.etDayName.doAfterTextChanged { text ->
            viewModel.updateDayName(text?.toString() ?: "")
        }

        viewModel.dayName.observe(this) { name ->
            binding.toolbar.title = name
            if (binding.etDayName.text.toString() != name) {
                binding.etDayName.setText(name)
            }
        }

        binding.fabAddExercise.setOnClickListener {
            showAddExerciseDialog()
        }
    }

    private fun showAddExerciseDialog() {
        AddExerciseDialog(null) { name, sets, reps, notes ->
            viewModel.addExercise(name, sets, reps, notes)
        }.show(supportFragmentManager, "AddExercise")
    }

    private fun showEditDialog(exercise: com.skulpt.app.data.model.Exercise) {
        AddExerciseDialog(exercise) { name, sets, reps, notes ->
            viewModel.updateExercise(
                exercise.copy(name = name, sets = sets, reps = reps, notes = notes)
            )
        }.show(supportFragmentManager, "EditExercise")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.menu_quick_add -> {
                showQuickAddDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showImageActions(exercise: com.skulpt.app.data.model.Exercise) {
        val options = mutableListOf("View Full Screen", "Pick from Gallery", "Search from Internet")
        if (!exercise.imageUri.isNullOrEmpty()) {
            options.add("Delete Custom Image")
        }

        AlertDialog.Builder(this)
            .setTitle(exercise.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "View Full Screen" -> {
                        val imageUri = if (!exercise.imageUri.isNullOrEmpty()) {
                            exercise.imageUri
                        } else {
                            com.skulpt.app.util.PlaceholderUtil.getDynamicImageUrl(
                                exercise.name,
                                viewModel.defaultImageQuery.value ?: ""
                            )
                        }
                        val intent = Intent(this, com.skulpt.app.ui.image.FullScreenImageActivity::class.java)
                        intent.putExtra(com.skulpt.app.ui.image.FullScreenImageActivity.EXTRA_IMAGE_URI, imageUri)
                        startActivity(intent)
                    }
                    "Pick from Gallery" -> {
                        pendingImageExerciseId = exercise.id
                        imagePickerLauncher.launch("image/*")
                    }
                    "Search from Internet" -> {
                        showInternetSearchDialog(exercise)
                    }
                    "Delete Custom Image" -> {
                        viewModel.updateExerciseImage(exercise.id, null)
                    }
                }
            }
            .show()
    }

    private fun showInternetSearchDialog(exercise: com.skulpt.app.data.model.Exercise) {
        val dialog = com.skulpt.app.ui.session.WebViewSearchDialogFragment.newInstance(
            exercise.name,
            true, // acceleration
            ""   // user agent
        )
        
        supportFragmentManager.setFragmentResultListener(
            com.skulpt.app.ui.session.WebViewSearchDialogFragment.REQUEST_KEY,
            this
        ) { _, bundle ->
            val imageUrl = bundle.getString(com.skulpt.app.ui.session.WebViewSearchDialogFragment.RESULT_IMAGE_URL)
            if (!imageUrl.isNullOrEmpty()) {
                viewModel.updateExerciseImage(exercise.id, imageUrl)
                Toast.makeText(this, "Image selected!", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show(supportFragmentManager, com.skulpt.app.ui.session.WebViewSearchDialogFragment.TAG)
    }

    private fun showQuickAddDialog() {
        val commonExercises = arrayOf(
            "Push-Ups", "Pull-Ups", "Squats", "Lunges", "Plank",
            "Deadlift", "Bench Press", "Shoulder Press", "Bicep Curls",
            "Tricep Dips", "Jumping Jacks", "Burpees", "Mountain Climbers",
            "Crunches", "Leg Raises", "Hip Thrusts", "Rows"
        )
        AlertDialog.Builder(this)
            .setTitle("Quick Add Exercise")
            .setItems(commonExercises) { _, which ->
                viewModel.addExercise(commonExercises[which], 3, 10)
                Toast.makeText(this, "${commonExercises[which]} added", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
