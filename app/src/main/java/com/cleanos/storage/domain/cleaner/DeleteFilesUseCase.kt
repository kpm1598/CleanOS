package com.cleanos.storage.domain.cleaner

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.cleanos.storage.data.models.FileSource
import com.cleanos.storage.data.models.ScannedFile
import java.io.File

data class DeleteOutcome(
    val deletedUris: List<String> = emptyList(),
    val failed: List<Pair<ScannedFile, String>> = emptyList(),
    /** When non-null, the system deletion dialog must be shown for these URIs (Android 11+ MediaStore flow). */
    val systemRequest: PendingIntent? = null,
    val systemRequestUris: List<String> = emptyList()
)

/** User-confirmed deletion only. Nothing here is ever called without the queue screen's
 *  confirmation dialog first. Tries, in order: a direct file delete (all-files access),
 *  ContentResolver.delete, SAF document delete; files that the OS protects are handed back
 *  as a MediaStore system deletion request instead of being force-deleted. */
class DeleteFilesUseCase(private val context: Context) {
    operator fun invoke(files: List<ScannedFile>): DeleteOutcome {
        val deleted = ArrayList<String>()
        val failed = ArrayList<Pair<ScannedFile, String>>()
        val needsSystem = ArrayList<ScannedFile>()
        for (file in files) {
            val uri = Uri.parse(file.uriString)
            var done = false
            // 1) Plain file delete when we have a real path and all-files access.
            val path = file.path
            if (!path.isNullOrBlank()) {
                val f = File(path)
                if (f.exists()) { done = runCatching { f.delete() }.getOrDefault(false) }
                else done = true // already gone - counts as removed, verified below
            }
            // 2) SAF document delete.
            if (!done && file.source == FileSource.SAF) {
                done = runCatching {
                    DocumentFile.fromSingleUri(context, uri)?.delete() == true ||
                        androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)?.delete() == true
                }.getOrDefault(false)
            }
            // 3) MediaStore delete.
            if (!done) {
                try {
                    done = context.contentResolver.delete(uri, null, null) > 0
                } catch (e: SecurityException) {
                    needsSystem += file; continue
                } catch (e: Exception) {
                    failed += file to (e.message ?: "Delete failed"); continue
                }
            }
            // Verify honestly: if the row/path still exists, do not claim success.
            if (done && !path.isNullOrBlank() && File(path).exists()) done = false
            if (done) deleted += file.uriString else if (file !in needsSystem) {
                // Some shares delete without a path check; verify via resolver query.
                if (!existsInResolver(uri)) deleted += file.uriString else failed += file to "File still exists after delete attempt"
            }
        }
        var pending: PendingIntent? = null
        if (needsSystem.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pending = MediaStore.createDeleteRequest(context.contentResolver, needsSystem.map { Uri.parse(it.uriString) })
        } else if (needsSystem.isNotEmpty()) {
            needsSystem.forEach { failed += it to "Android requires this file to be deleted from the Files app" }
        }
        return DeleteOutcome(deleted, failed, pending, needsSystem.map { it.uriString })
    }

    fun existsInResolver(uri: Uri): Boolean = try {
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use { it.moveToFirst() } ?: false
    } catch (e: Exception) { false }

    /** After a system deletion request returns, verify which of the URIs are actually gone. */
    fun verifyGone(files: List<ScannedFile>): Pair<List<String>, List<ScannedFile>> {
        val gone = ArrayList<String>(); val stillThere = ArrayList<ScannedFile>()
        for (file in files) {
            val pathGone = file.path.isNullOrBlank() || !File(file.path).exists()
            val resolverGone = !existsInResolver(Uri.parse(file.uriString))
            if (pathGone && resolverGone) gone += file.uriString else stillThere += file
        }
        return gone to stillThere
    }
}
