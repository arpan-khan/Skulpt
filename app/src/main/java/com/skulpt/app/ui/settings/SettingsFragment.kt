package com.skulpt.app.ui.settings

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.skulpt.app.SkulptApplication
import com.skulpt.app.data.model.AppSettings
import com.skulpt.app.databinding.FragmentSettingsBinding
import com.skulpt.app.notifications.NotificationHelper
import com.skulpt.app.util.DatabaseBackupUtil
import com.skulpt.app.util.ImportExportUtil
import com.skulpt.app.ui.viewmodel.SettingsViewModel
import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    private var currentSettings: AppSettings = AppSettings()

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        CoroutineScope(Dispatchers.IO).launch {
            val success = ImportExportUtil.importSchedule(requireContext(), uri)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(requireContext(), "Import successful! ✅", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Import failed. Check file format.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        CoroutineScope(Dispatchers.IO).launch {
            val db = SkulptApplication.instance.database
            val success = DatabaseBackupUtil.exportDatabase(requireContext(), db, uri)
            withContext(Dispatchers.Main) {
                if (success) Toast.makeText(requireContext(), "Backup Exported! ✅", Toast.LENGTH_LONG).show()
                else Toast.makeText(requireContext(), "Failed to export backup.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        CoroutineScope(Dispatchers.IO).launch {
            val db = SkulptApplication.instance.database
            val success = DatabaseBackupUtil.importDatabase(requireContext(), db, uri)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(requireContext(), "Backup Restored! App will restart.", Toast.LENGTH_LONG).show()
                    val intent = requireContext().packageManager.getLaunchIntentForPackage(requireContext().packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent?.let { startActivity(it) }
                    Runtime.getRuntime().exit(0)
                } else {
                    Toast.makeText(requireContext(), "Failed to restore backup.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.settings.observe(viewLifecycleOwner) { settings ->
            settings ?: return@observe
            currentSettings = settings
            bindSettings(settings)
        }

        setupListeners()
    }

    private fun bindSettings(s: AppSettings) {
        // Theme
        binding.radioGroupTheme.setOnCheckedChangeListener(null)
        binding.radioGroupTheme.check(
            when (s.themeMode) {
                1 -> com.skulpt.app.R.id.radio_light
                2 -> com.skulpt.app.R.id.radio_dark
                else -> com.skulpt.app.R.id.radio_system
            }
        )
        setupThemeListener()

        // Rest timer
        binding.sliderRestTimer.value = s.restTimerSeconds.toFloat().coerceIn(15f, 300f)
        binding.tvRestTimerValue.text = "${s.restTimerSeconds}s"

        // Toggles
        binding.switchAutoScroll.isChecked = s.autoScrollExercises
        binding.switchShowImages.isChecked = s.showExerciseImages
        binding.switchReminders.isChecked = s.remindersEnabled

        // Reminder time
        updateReminderTimeUI(s.reminderHour, s.reminderMinute)
        binding.layoutReminderTime.visibility =
            if (s.remindersEnabled) View.VISIBLE else View.GONE

        // Media
        binding.etDefaultQuery.setText(s.defaultImageQuery)

        // Advanced
        binding.switchHwAccel.isChecked = s.webViewHardwareAcceleration
        binding.etUserAgent.setText(s.customUserAgent)
    }

    private fun setupThemeListener() {
        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                com.skulpt.app.R.id.radio_light -> 1
                com.skulpt.app.R.id.radio_dark -> 2
                else -> 0
            }
            if (mode != currentSettings.themeMode) {
                currentSettings = currentSettings.copy(themeMode = mode)
                viewModel.saveSettings(currentSettings)

                when (mode) {
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }
    }

    private fun setupListeners() {
        setupThemeListener()

        binding.sliderRestTimer.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val secs = value.toInt()
                binding.tvRestTimerValue.text = "${secs}s"
                currentSettings = currentSettings.copy(restTimerSeconds = secs)
                viewModel.saveSettings(currentSettings)
            }
        }

        binding.switchAutoScroll.setOnCheckedChangeListener { _, checked ->
            currentSettings = currentSettings.copy(autoScrollExercises = checked)
            viewModel.saveSettings(currentSettings)
        }

        binding.switchShowImages.setOnCheckedChangeListener { _, checked ->
            currentSettings = currentSettings.copy(showExerciseImages = checked)
            viewModel.saveSettings(currentSettings)
        }

        binding.switchReminders.setOnCheckedChangeListener { _, checked ->
            currentSettings = currentSettings.copy(remindersEnabled = checked)
            viewModel.saveSettings(currentSettings)
            binding.layoutReminderTime.visibility = if (checked) View.VISIBLE else View.GONE
            if (checked) {
                NotificationHelper.scheduleReminder(requireContext(), currentSettings)
            } else {
                NotificationHelper.cancelReminder(requireContext())
            }
        }

        binding.etDefaultQuery.doAfterTextChanged { text ->
            val query = text?.toString() ?: ""
            if (query != currentSettings.defaultImageQuery) {
                currentSettings = currentSettings.copy(defaultImageQuery = query)
                viewModel.saveSettings(currentSettings)
            }
        }

        binding.switchHwAccel.setOnCheckedChangeListener { _, checked ->
            currentSettings = currentSettings.copy(webViewHardwareAcceleration = checked)
            viewModel.saveSettings(currentSettings)
        }

        binding.etUserAgent.doAfterTextChanged { text ->
            val ua = text?.toString() ?: ""
            if (ua != currentSettings.customUserAgent) {
                currentSettings = currentSettings.copy(customUserAgent = ua)
                viewModel.saveSettings(currentSettings)
            }
        }

        binding.btnSetReminderTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    currentSettings = currentSettings.copy(
                        reminderHour = hour, reminderMinute = minute
                    )
                    viewModel.saveSettings(currentSettings)
                    updateReminderTimeUI(hour, minute)
                    if (currentSettings.remindersEnabled) {
                        NotificationHelper.scheduleReminder(requireContext(), currentSettings)
                    }
                },
                currentSettings.reminderHour,
                currentSettings.reminderMinute,
                false
            ).show()
        }

        // Backup & Restore
        binding.btnExportBackup.setOnClickListener {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            exportBackupLauncher.launch("SkulptBackup_$dateStr.bak")
        }

        binding.btnImportBackup.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Restore Backup")
                .setMessage("Warning: This will overwrite ALL current workouts, stats, and settings. The app will restart. Proceed?")
                .setPositiveButton("Restore") { _, _ ->
                    importBackupLauncher.launch(arrayOf("*/*"))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Data management
        binding.btnExport.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val uri = ImportExportUtil.exportFull(requireContext())
                withContext(Dispatchers.Main) {
                    if (uri != null) {
                        Toast.makeText(
                            requireContext(),
                            "Exported to Downloads ✅",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Export failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        binding.btnImport.setOnClickListener {
            importLauncher.launch("application/json")
        }

        binding.btnImportJson.setOnClickListener {
            showImportJsonDialog()
        }

        binding.btnResetData.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Reset All Data")
                .setMessage("This will delete all workout sessions and cannot be undone. Your schedule will remain.")
                .setPositiveButton("Reset Sessions") { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        SkulptApplication.instance.database.workoutSessionDao().deleteAllSessions()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Session history cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnResetWebview.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Reset WebView Cache")
                .setMessage("This will clear all Cookies, Cache, and Browser data. This often fixes the 'blank screen' issue.")
                .setPositiveButton("Reset") { _, _ ->
                    resetWebViewData()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.tvAboutApp.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("About Skulpt")
                .setMessage("Skulpt is an offline-first fitness tracking application.\n\nDeveloped by Arpan.\n\nBuilt natively using Kotlin and Material 3 design and with the help of AI.")
                .setPositiveButton("GitHub") { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/arpan-khan/Skulpt"))
                    startActivity(intent)
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    private fun resetWebViewData() {
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()
        
        // Clearing WebView data requires a context
        val webView = android.webkit.WebView(requireContext())
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
        webView.clearSslPreferences()
        
        Toast.makeText(requireContext(), "WebView cleared! Re-try the search now.", Toast.LENGTH_LONG).show()
    }

    private fun updateReminderTimeUI(hour: Int, minute: Int) {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = if (hour % 12 == 0) 12 else hour % 12
        binding.tvReminderTime.text = String.format("%d:%02d %s", h, minute, amPm)
    }

    private fun showImportJsonDialog() {
        val input = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            hint = "Paste JSON here..."
            minLines = 5
            gravity = android.view.Gravity.TOP
        }
        val container = com.google.android.material.textfield.TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox).apply {
            setPadding(48, 24, 48, 24)
            addView(input)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Import Workout JSON")
            .setView(container)
            .setPositiveButton("Import") { _, _ ->
                val json = input.text?.toString() ?: ""
                if (json.isNotBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val success = ImportExportUtil.importScheduleFromJson(json)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                Toast.makeText(requireContext(), "Import successful! ✅", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(requireContext(), "Import failed. Check JSON format.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
