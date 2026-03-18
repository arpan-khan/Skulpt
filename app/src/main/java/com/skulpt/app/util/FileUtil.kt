package com.skulpt.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object FileUtil {

    /**
     * Copies the content of a Uri to the app's internal "images" directory.
     * Returns the absolute path of the new local file, or null on failure.
     */
    fun saveUriToInternalStorage(context: Context, uri: Uri): String? {
        val contentResolver = context.contentResolver
        val inputStream: InputStream? = try {
            contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (inputStream == null) return null

        return try {
            val imageDir = File(context.filesDir, "images")
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }

            // Generate a unique filename to avoid collisions
            val extension = getExtensionFromUri(context, uri) ?: "jpg"
            val fileName = "custom_${UUID.randomUUID()}.$extension"
            val outputFile = File(imageDir, fileName)

            FileOutputStream(outputFile).use { outputStream ->
                inputStream.use { input ->
                    input.copyTo(outputStream)
                }
            }

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getExtensionFromUri(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)?.let { mime ->
            android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        }
    }

    /**
     * Deletes a local file if it exists.
     */
    fun deleteLocalFile(path: String) {
        try {
            val file = File(path)
            if (file.exists() && file.absolutePath.contains(File.separator + "images" + File.separator)) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
