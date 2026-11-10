package com.cleanos.storage.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.cleanos.storage.data.models.ScannedFile

/** Opens a file with the system's viewer for its type. Read-only; never modifies the file. */
object FileOpener {
    fun open(context: Context, file: ScannedFile) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(file.uriString), file.mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open ${file.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "No app can open this file type", Toast.LENGTH_LONG).show()
        }
    }
}
