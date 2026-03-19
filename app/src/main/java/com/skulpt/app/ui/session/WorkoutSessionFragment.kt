package com.skulpt.app.ui.session

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import android.webkit.*
import android.widget.ProgressBar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.skulpt.app.data.model.Exercise
import com.skulpt.app.databinding.FragmentWorkoutSessionBinding
import com.skulpt.app.ui.image.FullScreenImageActivity
import com.skulpt.app.ui.viewmodel.WorkoutSessionViewModel

class WorkoutSessionFragment : Fragment() {

    private var _binding: FragmentWorkoutSessionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorkoutSessionViewModel by viewModels()
    private lateinit var adapter: ExerciseCardAdapter
    private var pendingExerciseId: Long? = null
    private var heroRotationJob: Job? = null
    private var currentHeroIndex = 0
    private var dayId: Long = -1L
    private var autoScrollEnabled = true

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val id = pendingExerciseId ?: return@registerForActivityResult
            // Save a local copy to internal storage for permanent access
            val localPath = com.skulpt.app.util.FileUtil.saveUriToInternalStorage(requireContext(), uri)
            if (localPath != null) {
                viewModel.updateExerciseImage(id, localPath)
            } else {
                // Fallback to direct URI if copying fails (try-catch within saveUri handles most errors)
                viewModel.updateExerciseImage(id, uri.toString())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dayId = arguments?.getLong("dayId", -1L) ?: -1L
        if (dayId == -1L) {
            findNavController().popBackStack()
            return
        }

        viewModel.initialize(dayId)

        adapter = ExerciseCardAdapter(
            onCheckToggle = { exercise ->
                viewModel.toggleExercise(exercise)
                // Auto scroll logic
                if (autoScrollEnabled && !exercise.isCompleted) {
                    val currentList = viewModel.dayWithExercises.value?.exercises?.sortedBy { it.orderIndex }
                    if (currentList != null) {
                        val index = currentList.indexOfFirst { it.id == exercise.id }
                        if (index != -1 && index + 1 < currentList.size) {
                            binding.recyclerExercises.postDelayed({
                                binding.recyclerExercises.smoothScrollToPosition(index + 1)
                            }, 300)
                        }
                    }
                }
            },
            onImageClick = { exercise ->
                showImageActions(exercise)
            },
            onTimerClick = { exercise ->
                val bottomSheet = RestTimerBottomSheet().apply {
                    arguments = Bundle().apply { putInt("START_SECONDS", exercise.timerSeconds) }
                }
                bottomSheet.show(childFragmentManager, "RestTimer")
            }
        )

        binding.recyclerExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WorkoutSessionFragment.adapter
            setHasFixedSize(false)
        }

        viewModel.dayWithExercises.observe(viewLifecycleOwner) { dayWithEx ->
            dayWithEx ?: return@observe
            binding.tvDayTitle.text = dayWithEx.day.name
            binding.toolbar.title = "" // Day name is now in tvDayTitle

            val total = dayWithEx.totalCount
            val completed = dayWithEx.completedCount
            val percent = dayWithEx.completionPercent

            binding.progressBar.progress = percent
            binding.tvProgress.text = "$completed / $total exercises completed"

            // Bind hero image based on first exercise or general query
            val firstExercise = dayWithEx.exercises.sortedBy { it.orderIndex }.firstOrNull()
            val heroQuery = firstExercise?.name ?: dayWithEx.day.name
            val heroUrl = com.skulpt.app.util.PlaceholderUtil.getDynamicImageUrl(heroQuery)
            
            com.bumptech.glide.Glide.with(this)
                .load(heroUrl)
                .centerCrop()
                .placeholder(com.skulpt.app.R.drawable.bg_image_rounded)
                .into(binding.ivHero)

            adapter.submitExercises(dayWithEx.exercises.sortedBy { it.orderIndex })

            if (total > 0 && completed == total) {
                binding.cardAllDone.visibility = View.VISIBLE
            } else {
                binding.cardAllDone.visibility = View.GONE
            }

            startHeroRotation(dayWithEx.exercises)
        }

        viewModel.settings.observe(viewLifecycleOwner) { settings ->
            settings ?: return@observe
            adapter.setBaseQuery(settings.defaultImageQuery)
            adapter.setShowImages(settings.showExerciseImages)
            autoScrollEnabled = settings.autoScrollExercises
        }

        viewModel.sessionSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Toast.makeText(requireContext(), "Workout saved!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            showFinishDialog()
        }

        binding.toolbar.inflateMenu(com.skulpt.app.R.menu.menu_session)
        binding.toolbar.setOnMenuItemClickListener { item ->
            handleMenuItem(item)
        }

        binding.fabTimer.setOnClickListener {
            RestTimerBottomSheet().show(childFragmentManager, "RestTimer")
        }

        binding.btnEditWorkoutHeader.setOnClickListener {
            val intent = Intent(requireContext(), com.skulpt.app.ui.editor.WorkoutEditorActivity::class.java)
            intent.putExtra(com.skulpt.app.ui.editor.WorkoutEditorActivity.EXTRA_DAY_ID, dayId)
            startActivity(intent)
        }

        binding.btnStartStopWorkout.setOnClickListener {
            if (viewModel.isSessionActive.value == true) {
                // Just stop the timer, don't finish
                viewModel.stopSession()
            } else {
                showStartOptions()
            }
        }

        binding.btnFinishWorkout.setOnClickListener {
            showFinishDialog()
        }

        viewModel.isSessionActive.observe(viewLifecycleOwner) { active ->
            if (active) {
                binding.btnStartStopWorkout.text = "Pause"
                binding.btnStartStopWorkout.setIconResource(com.skulpt.app.R.drawable.ic_stop)
                binding.btnFinishWorkout.visibility = View.VISIBLE
            } else {
                binding.btnStartStopWorkout.text = "Start"
                binding.btnStartStopWorkout.setIconResource(com.skulpt.app.R.drawable.ic_play)
                // If elapsed time > 0, we can still show finish
                val hasProgress = (viewModel.elapsedTimeSeconds.value ?: 0) > 0
                binding.btnFinishWorkout.visibility = if (hasProgress) View.VISIBLE else View.GONE
            }
        }

        viewModel.elapsedTimeSeconds.observe(viewLifecycleOwner) { seconds ->
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            binding.tvSessionTimer.text = String.format("%02d:%02d:%02d", h, m, s)
        }
    }

    private fun showStartOptions() {
        val options = arrayOf("Reset & Start", "Resume from where I left off")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Start Workout")
            .setItems(options) { _, which ->
                viewModel.startSession(shouldReset = (which == 0))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleMenuItem(item: MenuItem): Boolean {
        return when (item.itemId) {
            com.skulpt.app.R.id.menu_reset_workout -> {
                viewModel.resetSession()
                true
            }
            else -> false
        }
    }

    private fun showImageActions(exercise: Exercise) {
        val options = mutableListOf("View Full Screen", "Pick from Gallery", "Search from Internet")
        if (!exercise.imageUri.isNullOrEmpty()) {
            options.add("Delete Custom Image")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(exercise.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "View Full Screen" -> {
                        val imageUri = if (!exercise.imageUri.isNullOrEmpty()) {
                            exercise.imageUri
                        } else {
                            com.skulpt.app.util.PlaceholderUtil.getDynamicImageUrl(
                                exercise.name, 
                                viewModel.settings.value?.defaultImageQuery ?: ""
                            )
                        }
                        val intent = Intent(requireContext(), FullScreenImageActivity::class.java)
                        intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_URI, imageUri)
                        startActivity(intent)
                    }
                    "Pick from Gallery" -> {
                        pendingExerciseId = exercise.id
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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

    private fun showFinishDialog() {
        val dayWithEx = viewModel.dayWithExercises.value ?: run {
            findNavController().popBackStack()
            return
        }
        
        // Pause timer while dialog is shown
        viewModel.stopSession()

        AlertDialog.Builder(requireContext())
            .setTitle("Finish Workout?")
            .setMessage("Save progress for ${dayWithEx.day.name}?")
            .setPositiveButton("Save") { _, _ ->
                viewModel.saveSession(dayWithEx)
            }
            .setNegativeButton("Discard") { _, _ ->
                findNavController().popBackStack()
            }
            .show()
    }

    private fun showInternetSearchDialog(exercise: Exercise) {
        val settings = viewModel.settings.value ?: com.skulpt.app.data.model.AppSettings()
        val dialog = WebViewSearchDialogFragment.newInstance(
            exercise.name,
            settings.webViewHardwareAcceleration,
            settings.customUserAgent
        )
        
        childFragmentManager.setFragmentResultListener(
            WebViewSearchDialogFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val imageUrl = bundle.getString(WebViewSearchDialogFragment.RESULT_IMAGE_URL)
            if (!imageUrl.isNullOrEmpty()) {
                viewModel.updateExerciseImage(exercise.id, imageUrl)
                Toast.makeText(requireContext(), "Image selected!", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show(childFragmentManager, WebViewSearchDialogFragment.TAG)
    }

    // Removed showSearchResultsDialog as it's merged into showInternetSearchDialog

    private fun startHeroRotation(exercises: List<Exercise>) {
        heroRotationJob?.cancel()
        if (exercises.isEmpty()) return

        val imageUrls = exercises.map { ex ->
            if (!ex.imageUri.isNullOrEmpty()) ex.imageUri
            else com.skulpt.app.util.PlaceholderUtil.getDynamicImageUrl(ex.name, viewModel.settings.value?.defaultImageQuery ?: "")
        }.filterNotNull().distinct()

        if (imageUrls.size <= 1) return

        heroRotationJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                delay(4000) // Rotate every 4 seconds
                currentHeroIndex = (currentHeroIndex + 1) % imageUrls.size
                val nextUrl = imageUrls[currentHeroIndex]
                
                if (_binding != null) {
                    com.bumptech.glide.Glide.with(this@WorkoutSessionFragment)
                        .load(nextUrl)
                        .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(binding.ivHero)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        heroRotationJob?.cancel()
        _binding = null
    }
}
