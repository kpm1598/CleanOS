package com.cleanos.storage.data.repositories

import android.content.Context
import com.cleanos.storage.core.scanner.FileCategory
import com.cleanos.storage.core.scanner.ScanProgress
import com.cleanos.storage.core.scanner.StorageScanner
import com.cleanos.storage.data.database.CleanOSDatabase
import com.cleanos.storage.data.models.Decision
import com.cleanos.storage.data.models.ScannedFile
import kotlinx.coroutines.flow.Flow

class FileRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = CleanOSDatabase.get(appContext).scannedFileDao()
    val prefs = AppPrefs(appContext)

    fun observeAll(): Flow<List<ScannedFile>> = dao.observeAll()
    fun observeUndecided(): Flow<List<ScannedFile>> = dao.observeUndecided()
    fun observeDeleteQueue(): Flow<List<ScannedFile>> = dao.observeDeleteQueue()
    fun observeKeepList(): Flow<List<ScannedFile>> = dao.observeKeepList()
    suspend fun getAll(): List<ScannedFile> = dao.getAll()

    /** Runs a fresh scan. Previous results are replaced, but KEEP/DELETE decisions for files
     *  that still exist are preserved and re-applied to the new rows. */
    suspend fun scan(categories: Set<FileCategory>, onProgress: suspend (ScanProgress) -> Unit) {
        val previous = dao.getAll().associateBy { it.uriString }
        val scanner = StorageScanner(appContext)
        val fresh = ArrayList<ScannedFile>()
        scanner.onBatch = { batch ->
            val merged = batch.map { f ->
                val old = previous[f.uriString]
                if (old != null) f.copy(decision = old.decision, queueSelected = old.queueSelected) else f
            }
            fresh += merged
            dao.insertAll(merged)
        }
        // Clear first so vanished files from the previous scan do not linger.
        val keptUris = previous.filterValues { it.decision != Decision.UNDECIDED }.keys
        dao.clearAll()
        scanner.scan(categories, prefs.safTreeUri).collect { progress ->
            onProgress(progress)
            if (progress.done) {
                // Files that had decisions but were not re-found are not silently dropped from the queue:
                // they stay out of the DB, because a file that no longer exists cannot be deleted or kept.
                prefs.lastScanEpochMs = System.currentTimeMillis()
            }
        }
        @Suppress("UNUSED_EXPRESSION") keptUris
    }

    suspend fun setDecision(file: ScannedFile, decision: Decision) = dao.setDecision(file.uriString, decision)
    suspend fun setQueueSelected(uri: String, selected: Boolean) = dao.setQueueSelected(uri, selected)
    suspend fun setAllQueueSelected(selected: Boolean) = dao.setAllQueueSelected(selected)
    suspend fun removeFromDatabase(uris: List<String>) = dao.deleteByUris(uris)
    suspend fun resetSession() {
        // Resetting a review session returns every file to UNDECIDED (files themselves are untouched).
        dao.getAll().forEach { dao.setDecision(it.uriString, Decision.UNDECIDED) }
    }
}
