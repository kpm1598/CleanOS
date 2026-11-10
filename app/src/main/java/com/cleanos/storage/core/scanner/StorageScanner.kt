package com.cleanos.storage.core.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.cleanos.storage.data.models.FileSource
import com.cleanos.storage.data.models.ScannedFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class ScanProgress(
    val currentCategory: FileCategory? = null,
    val foundSoFar: Int = 0,
    val done: Boolean = false,
    val error: String? = null
)

/** Real scanner.
 *  Primary source: MediaStore Files collection (covers images/video/audio/documents on shared storage).
 *  Secondary source: the user-picked SAF folder, traversed recursively.
 *  Results are de-duplicated by content URI and then by absolute path, so a file seen by both
 *  sources appears once. Protected app-private and system directories are never touched:
 *  MediaStore simply does not return them, and SAF only exposes the folder the user picked.
 *  Only metadata is read - never file contents. */
class StorageScanner(private val context: Context) {

    fun scan(categories: Set<FileCategory>, safTreeUri: String?): Flow<ScanProgress> = flow {
        val seenUris = HashSet<String>()
        val seenPaths = HashSet<String>()
        val batch = ArrayList<ScannedFile>(200)
        var found = 0

        suspend fun collect(file: ScannedFile) {
            val key = file.uriString
            val pathKey = file.path?.lowercase()
            if (key in seenUris) return
            if (pathKey != null && pathKey in seenPaths) return
            seenUris += key; if (pathKey != null) seenPaths += pathKey
            batch += file; found++
            if (batch.size >= 200) {
                onBatch(ArrayList(batch)); batch.clear()
                emit(ScanProgress(foundSoFar = found))
            }
        }

        for (category in categories) {
            currentCoroutineContext().ensureActive()
            emit(ScanProgress(currentCategory = category, foundSoFar = found))
            try { scanMediaStoreCategory(category, ::collect) } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                emit(ScanProgress(currentCategory = category, foundSoFar = found, error = "Could not scan ${category.label}: ${e.message}"))
            }
        }
        if (!safTreeUri.isNullOrBlank()) {
            emit(ScanProgress(currentCategory = null, foundSoFar = found))
            try { scanSafTree(Uri.parse(safTreeUri), categories, ::collect) } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
        if (batch.isNotEmpty()) { onBatch(ArrayList(batch)); batch.clear() }
        emit(ScanProgress(foundSoFar = found, done = true))
    }

    /** Set by the caller (repository) to persist each batch as it arrives. */
    var onBatch: suspend (List<ScannedFile>) -> Unit = {}

    private suspend fun scanMediaStoreCategory(category: FileCategory, collect: suspend (ScannedFile) -> Unit) {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA
        )
        // MIME-prefix selection keeps the query fast; extension-only types (e.g. .mkv, .7z)
        // are additionally matched in code after the query.
        val (selection, args) = mimeSelection(category)
        context.contentResolver.query(collection, projection, selection, args,
            MediaStore.Files.FileColumns.SIZE + " DESC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val modCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val name = cursor.getString(nameCol) ?: continue
                val mime = cursor.getString(mimeCol)
                val actual = FileCategory.classify(mime, name)
                if (actual != category) continue
                val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                // Never index CleanOS's own data or Android's protected app directories.
                if (path != null && (path.contains("/Android/data/") || path.contains("/Android/obb/"))) continue
                val uri = ContentUris.withAppendedId(collection, cursor.getLong(idCol))
                collect(ScannedFile(
                    uriString = uri.toString(), name = name, path = path, mimeType = mime,
                    sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0),
                    modifiedEpochSec = cursor.getLong(modCol),
                    category = actual, source = FileSource.MEDIASTORE))
            }
        }
    }

    private fun mimeSelection(category: FileCategory): Pair<String?, Array<String>?> {
        val clauses = ArrayList<String>(); val args = ArrayList<String>()
        category.mimePrefixes.forEach { clauses += MediaStore.Files.FileColumns.MIME_TYPE + " LIKE ?"; args += it + "%" }
        category.mimeTypes.forEach { clauses += MediaStore.Files.FileColumns.MIME_TYPE + " = ?"; args += it }
        category.extensions.forEach { clauses += MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?"; args += "%.$it" }
        if (category == FileCategory.OTHER) return null to null
        if (clauses.isEmpty()) return null to null
        return clauses.joinToString(" OR ", "(", ")") to args.toTypedArray()
    }

    private suspend fun scanSafTree(treeUri: Uri, categories: Set<FileCategory>, collect: suspend (ScannedFile) -> Unit) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return
        val stack = ArrayDeque<DocumentFile>(); stack.add(root)
        var visited = 0
        while (stack.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val node = stack.removeLast()
            if (visited++ > 20000) break // safety cap for very large trees
            if (node.isDirectory) { node.listFiles().forEach { stack.add(it) }; continue }
            val name = node.name ?: continue
            val category = FileCategory.classify(node.type, name)
            if (category !in categories) continue
            collect(ScannedFile(
                uriString = node.uri.toString(), name = name, path = null, mimeType = node.type,
                sizeBytes = node.length().coerceAtLeast(0),
                modifiedEpochSec = node.lastModified() / 1000,
                category = category, source = FileSource.SAF))
        }
    }
}
