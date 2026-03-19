package com.skulpt.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.skulpt.app.data.model.Exercise
import com.skulpt.app.data.model.WorkoutDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// DTOs for import/export JSON structure
data class ExportData(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("exportDate") val exportDate: String,
    @SerializedName("days") val days: List<ExportDay>
)

data class ExportDay(
    @SerializedName("dayIndex") val dayIndex: Int,
    @SerializedName("name") val name: String,
    @SerializedName("colorHex") val colorHex: String,
    @SerializedName("exercises") val exercises: List<ExportExercise>
)

data class ExportExercise(
    @SerializedName("name") val name: String,
    @SerializedName("sets") val sets: Int,
    @SerializedName("reps") val reps: Int,
    @SerializedName("orderIndex") val orderIndex: Int,
    @SerializedName("notes") val notes: String,
    @SerializedName("timerSeconds") val timerSeconds: Int,
    @SerializedName("hexcolor") val hexcolor: String = "#6750A4"
)

object ImportExportUtil {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Export the full weekly schedule (days 0–6) to a JSON file in Downloads.
     * Returns the Uri of the written file, or null on failure.
     */
    suspend fun exportFull(context: Context): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val db = com.skulpt.app.SkulptApplication.instance.database
                val exportDays = mutableListOf<ExportDay>()

                // Fetch all standard days in one suspend query (correct — no LiveData in IO)
                val allDaysWithEx = db.workoutDayDao().getAllDaysWithExercises()

                for (dayWithEx in allDaysWithEx) {
                    exportDays.add(
                        ExportDay(
                            dayIndex = dayWithEx.day.dayIndex,
                            name = dayWithEx.day.name,
                            colorHex = dayWithEx.day.colorHex,
                            exercises = dayWithEx.exercises
                                .sortedBy { it.orderIndex }
                                .map { ex ->
                                    ExportExercise(
                                        name = ex.name,
                                        sets = ex.sets,
                                        reps = ex.reps,
                                        orderIndex = ex.orderIndex,
                                        notes = ex.notes,
                                        timerSeconds = ex.timerSeconds,
                                        hexcolor = ex.hexcolor
                                    )
                                }
                        )
                    )
                }

                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val exportData = ExportData(version = 1, exportDate = dateStr, days = exportDays)
                val json = gson.toJson(exportData)
                writeJsonToDownloads(context, json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Import a schedule JSON from the given Uri, merging into the database.
     * Matches days by dayIndex (not by auto-generated ID). Returns true on success.
     */
    suspend fun importSchedule(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                } ?: return@withContext false
                importScheduleFromJson(json)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Import a schedule JSON from a raw string, merging into the database.
     */
    suspend fun importScheduleFromJson(json: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val exportData = gson.fromJson(json, ExportData::class.java)
                    ?: return@withContext false
                
                val db = com.skulpt.app.SkulptApplication.instance.database
                val dayDao = db.workoutDayDao()
                val exerciseDao = db.exerciseDao()

                for (exportDay in exportData.days) {
                    // Look up the day by its semantic dayIndex (0=Mon…6=Sun), not by Room ID
                    val existingDay = dayDao.getDayByIndex(exportDay.dayIndex)

                    val dayId = if (existingDay != null) {
                        // Update day name / color, then replace its exercises
                        dayDao.updateDay(existingDay.copy(name = exportDay.name, colorHex = exportDay.colorHex))
                        exerciseDao.deleteAllExercisesForDay(existingDay.id)
                        existingDay.id
                    } else {
                        // Day doesn't exist yet — insert it
                        dayDao.insertDay(WorkoutDay(dayIndex = exportDay.dayIndex, name = exportDay.name, colorHex = exportDay.colorHex))
                    }

                    // Insert exercises for this day
                    val exercises = exportDay.exercises.mapIndexed { idx, ex ->
                        Exercise(
                            dayId = dayId,
                            name = ex.name,
                            sets = ex.sets,
                            reps = ex.reps,
                            orderIndex = ex.orderIndex.takeIf { it >= 0 } ?: idx,
                            notes = ex.notes,
                            timerSeconds = ex.timerSeconds,
                            hexcolor = ex.hexcolor
                        )
                    }
                    exerciseDao.insertExercises(exercises)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun writeJsonToDownloads(context: Context, json: String): Uri? {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "Skulpt_backup_$dateStr.json"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
            )
            uri?.also {
                context.contentResolver.openOutputStream(it)?.use { os: OutputStream ->
                    os.write(json.toByteArray())
                }
            }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = java.io.File(dir, fileName)
            file.writeText(json)
            Uri.fromFile(file)
        }
    }
}
