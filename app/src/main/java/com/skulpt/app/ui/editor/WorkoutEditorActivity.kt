package com.skulpt.app.ui.editor

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.view.ViewGroup
import android.view.LayoutInflater
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

            try {
                contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {  }
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
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
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
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition

                val list = adapter.currentList.toMutableList()
                val movedItem = list.removeAt(from)
                list.add(to, movedItem)

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

        viewModel.dayColor.observe(this) { color ->
            try {
                binding.viewDayColorIndicator.setBackgroundColor(android.graphics.Color.parseColor(color))
            } catch (e: Exception) { }
        }

        binding.cvDayColor.setOnClickListener {
            showDayColorPicker()
        }

        binding.fabAddExercise.setOnClickListener {
            showAddExerciseDialog()
        }
    }

    private fun showAddExerciseDialog() {
        AddExerciseDialog(null) { name, sets, reps, notes, timerSeconds ->
            viewModel.addExercise(name, sets, reps, notes, timerSeconds)
        }.show(supportFragmentManager, "AddExercise")
    }

    private fun showEditDialog(exercise: com.skulpt.app.data.model.Exercise) {
        AddExerciseDialog(exercise) { name, sets, reps, notes, timerSeconds ->
            viewModel.updateExercise(
                exercise.copy(name = name, sets = sets, reps = reps, notes = notes, timerSeconds = timerSeconds)
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

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
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
            viewModel.defaultImageQuery.value,
            true,
            ""
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
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Quick Add Exercise")
            .setItems(commonExercises) { _, which ->
                viewModel.addExercise(commonExercises[which], 3, 10)
                Toast.makeText(this, "${commonExercises[which]} added", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showDayColorPicker() {
        val colors = listOf(
            "#6750A4", "#B00020", "#4CAF50", "#2196F3", "#FFEB3B", "#FF9800",
            "#6200EE", "#03DAC5", "#E91E63", "#9C27B0", "#00BCD4", "#8BC34A",
            "#CDDC39", "#FFC107", "#FF5722", "#795548", "#9E9E9E", "#607D8B"
        )
        val currentColor = viewModel.dayColor.value ?: "#6750A4"

        val dialogBinding = com.skulpt.app.databinding.DialogColorPickerBinding.inflate(layoutInflater)
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Pick Session Color")
            .setView(dialogBinding.root)
            .create()

        class ColorViewHolder(val itemBinding: com.skulpt.app.databinding.ItemColorPickerBinding) : RecyclerView.ViewHolder(itemBinding.root)

        dialogBinding.recyclerColors.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 4)
        dialogBinding.recyclerColors.adapter = object : RecyclerView.Adapter<ColorViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
                val itemBinding = com.skulpt.app.databinding.ItemColorPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return ColorViewHolder(itemBinding)
            }
            override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
                val hex = colors[position]
                holder.itemBinding.viewColor.setBackgroundColor(android.graphics.Color.parseColor(hex))
                holder.itemBinding.cardColor.strokeColor = if (hex.equals(currentColor, true)) {
                    com.google.android.material.color.MaterialColors.getColor(holder.itemBinding.root, com.google.android.material.R.attr.colorPrimary)
                } else {
                    android.graphics.Color.TRANSPARENT
                }
                holder.itemBinding.root.setOnClickListener {
                    viewModel.updateDayColor(hex)
                    dialog.dismiss()
                }
            }
            override fun getItemCount() = colors.size
        }

        dialogBinding.btnCustomColor.setOnClickListener {
            dialog.dismiss()
            showCustomColorDialog()
        }

        dialog.show()
    }

    private fun showCustomColorDialog() {
        val input = com.google.android.material.textfield.TextInputEditText(this).apply {
            hint = "#RRGGBB"
            setText(viewModel.dayColor.value)
        }
        val layout = com.google.android.material.textfield.TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox).apply {
            setPadding(48, 24, 48, 24)
            addView(input)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Custom Hex Color")
            .setView(layout)
            .setPositiveButton("Apply") { _, _ ->
                val hex = input.text.toString().trim()
                if (hex.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$"))) {
                    viewModel.updateDayColor(hex.uppercase())
                } else {
                    Toast.makeText(this, "Invalid hex code", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
