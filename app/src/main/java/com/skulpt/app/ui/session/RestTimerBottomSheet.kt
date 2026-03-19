package com.skulpt.app.ui.session

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.skulpt.app.R
import com.skulpt.app.SkulptApplication
import com.skulpt.app.databinding.BottomSheetRestTimerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RestTimerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRestTimerBinding? = null
    private val binding get() = _binding!!

    private var timer: CountDownTimer? = null
    private var totalSeconds = 60
    private var isRunning = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRestTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val argsTimer = arguments?.getInt("START_SECONDS", -1) ?: -1
        
        // Load saved timer duration
        CoroutineScope(Dispatchers.IO).launch {
            val settings = SkulptApplication.instance.database.appSettingsDao().getSettings()
            totalSeconds = if (argsTimer > 0) argsTimer else (settings?.restTimerSeconds ?: 60)
            withContext(Dispatchers.Main) {
                updateTimerDisplay(totalSeconds.toLong())
                binding.progressTimer.max = totalSeconds
                binding.progressTimer.progress = totalSeconds
                binding.etCustomDuration.setText(totalSeconds.toString())
            }
        }

        // Setup custom duration input
        binding.etCustomDuration.doAfterTextChanged { text ->
            if (!isRunning) {
                val input = text?.toString()?.toIntOrNull() ?: 0
                if (input > 0) {
                    totalSeconds = input
                    updateTimerDisplay(totalSeconds.toLong())
                    binding.progressTimer.max = totalSeconds
                    binding.progressTimer.progress = totalSeconds
                    saveDuration(totalSeconds)
                }
            }
        }

        // Setup presets
        binding.chipGroupPresets.setOnCheckedStateChangeListener { group, checkedIds ->
            if (!isRunning && checkedIds.isNotEmpty()) {
                val chip = group.findViewById<com.google.android.material.chip.Chip>(checkedIds.first())
                val seconds = chip.tag.toString().toIntOrNull() ?: return@setOnCheckedStateChangeListener
                totalSeconds = seconds
                binding.etCustomDuration.setText(seconds.toString())
                updateTimerDisplay(totalSeconds.toLong())
                binding.progressTimer.max = totalSeconds
                binding.progressTimer.progress = totalSeconds
                saveDuration(totalSeconds)
            }
        }

        binding.btnStartPause.setOnClickListener {
            if (isRunning) pauseTimer() else startTimer()
        }

        binding.btnReset.setOnClickListener {
            resetTimer()
        }
    }

    private fun startTimer() {
        val millis = totalSeconds * 1000L
        val progress = binding.progressTimer.progress
        val startFrom = if (progress > 0) progress.toLong() * 1000L else millis

        isRunning = true
        binding.btnStartPause.text = "Pause"
        binding.progressTimer.max = totalSeconds

        timer = object : CountDownTimer(startFrom, 100) {
            override fun onTick(remaining: Long) {
                val secs = (remaining / 1000).toInt()
                updateTimerDisplay(remaining / 1000)
                binding.progressTimer.progress = secs
            }

            override fun onFinish() {
                isRunning = false
                binding.btnStartPause.text = "Start"
                updateTimerDisplay(0)
                binding.progressTimer.progress = 0
                vibrate()
                binding.tvTimerCountdown.text = "Done!"
            }
        }.start()
    }

    private fun pauseTimer() {
        timer?.cancel()
        isRunning = false
        binding.btnStartPause.text = "Resume"
    }

    private fun resetTimer() {
        timer?.cancel()
        isRunning = false
        binding.btnStartPause.text = "Start"
        updateTimerDisplay(totalSeconds.toLong())
        binding.progressTimer.progress = totalSeconds
    }

    private fun updateTimerDisplay(seconds: Long) {
        val mins = seconds / 60
        val secs = seconds % 60
        binding.tvTimerCountdown.text = String.format("%d:%02d", mins, secs)
    }

    private fun vibrate() {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 300, 200, 300), -1)
        }
    }

    override fun onDestroyView() {
        timer?.cancel()
        super.onDestroyView()
        _binding = null
    }

    private fun saveDuration(seconds: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = SkulptApplication.instance.database.appSettingsDao()
            val settings = dao.getSettings() ?: com.skulpt.app.data.model.AppSettings()
            dao.upsertSettings(settings.copy(restTimerSeconds = seconds))
        }
    }
}
