package com.renaissance.teacher.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

object CsvShareHelper {

    fun saveAndShareCsv(context: Context, responseBody: ResponseBody, filename: String) {
        try {
            val exportDir = File(context.cacheDir, "csv_exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val file = File(exportDir, filename)
            val inputStream = responseBody.byteStream()
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, filename)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share / Save Attendance CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Toast.makeText(context, "Export ready: $filename", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
