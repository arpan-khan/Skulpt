package com.skulpt.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object FileUtil {

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
