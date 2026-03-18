package com.skulpt.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.skulpt.app.data.db.SkulptDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DatabaseBackupUtil {

    private const val TAG = "DatabaseBackupUtil"

    suspend fun exportDatabase(context: Context, database: SkulptDatabase, targetUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Force a WAL checkpoint to ensure all data is in the main database file
                database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
                
                val dbPath = database.openHelper.readableDatabase.path
                val dbFile = File(dbPath)
                
                if (!dbFile.exists()) {
                    Log.e(TAG, "Source database file does not exist")
                    return@withContext false
                }

                context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                    FileInputStream(dbFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d(TAG, "Database successfully exported to $targetUri")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export database", e)
                false
            }
        }
    }

    suspend fun importDatabase(context: Context, database: SkulptDatabase, sourceUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val dbPath = database.openHelper.readableDatabase.path
                val dbFile = File(dbPath)
                val walFile = File("$dbPath-wal")
                val shmFile = File("$dbPath-shm")

                // Close database to release locks
                database.close()

                // Delete WAL and SHM files to prevent corruption with the new DB file
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()

                // Copy from source URI to the local DB file
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    FileOutputStream(dbFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                Log.d(TAG, "Database successfully imported from $sourceUri")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import database", e)
                false
            }
        }
    }
}
