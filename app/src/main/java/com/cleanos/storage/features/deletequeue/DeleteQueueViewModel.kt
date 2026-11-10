package com.cleanos.storage.features.deletequeue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleanos.storage.CleanOSApp
import com.cleanos.storage.data.models.Decision
import com.cleanos.storage.data.models.ScannedFile
import com.cleanos.storage.domain.cleaner.DeleteOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DeleteReport(
    val deletedCount: Int = 0,
    val deletedSize: Long = 0,
    val failedMessages: List<String> = emptyList()
)

class DeleteQueueViewModel(app: Application) : AndroidViewModel(app) {
    private val appRef = app as CleanOSApp
    private val repo = appRef.repository
    val queue: StateFlow<List<ScannedFile>> = repo.observeDeleteQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()
    private val _report = MutableStateFlow<DeleteReport?>(null)
    val report: StateFlow<DeleteReport?> = _report.asStateFlow()
    /** Set when Android wants the user to approve deletion in a system dialog. */
    private val _pendingOutcome = MutableStateFlow<DeleteOutcome?>(null)
    val pendingOutcome: StateFlow<DeleteOutcome?> = _pendingOutcome.asStateFlow()
    private var pendingFiles: List<ScannedFile> = emptyList()
    private var targetSizes: Map<String, Long> = emptyMap()
    private var directOutcome: DeleteOutcome? = null

    fun toggleSelected(file: ScannedFile, selected: Boolean) {
        viewModelScope.launch { repo.setQueueSelected(file.uriString, selected) }
    }
    fun selectAll(selected: Boolean) { viewModelScope.launch { repo.setAllQueueSelected(selected) } }
    fun removeFromQueue(file: ScannedFile) {
        viewModelScope.launch { repo.setDecision(file, Decision.UNDECIDED) }
    }
    fun clearQueue() {
        viewModelScope.launch { queue.value.forEach { repo.setDecision(it, Decision.UNDECIDED) } }
    }

    /** Called only after the confirmation dialog is confirmed. */
    fun deleteSelected() {
        val targets = queue.value.filter { it.queueSelected }
        if (targets.isEmpty() || _deleting.value) return
        targetSizes = targets.associate { it.uriString to it.sizeBytes }
        _deleting.value = true
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) { appRef.deleteFiles(targets) }
            directOutcome = outcome
            if (outcome.deletedUris.isNotEmpty()) repo.removeFromDatabase(outcome.deletedUris)
            if (outcome.systemRequest != null) {
                pendingFiles = targets.filter { it.uriString in outcome.systemRequestUris }
                _pendingOutcome.value = outcome
            } else {
                finishReport(outcome, emptyList(), emptyList())
            }
        }
    }

    /** Called after the system deletion dialog returns (approved or cancelled). */
    fun onSystemRequestFinished(approved: Boolean) {
        val files = pendingFiles
        _pendingOutcome.value = null
        viewModelScope.launch {
            if (!approved) {
                finishReport(directOutcome ?: DeleteOutcome(), emptyList(),
                    files.map { it.name + ": cancelled in the system dialog - file was NOT deleted" })
            } else {
                val (gone, stillThere) = withContext(Dispatchers.IO) { appRef.deleteFiles.verifyGone(files) }
                if (gone.isNotEmpty()) repo.removeFromDatabase(gone)
                finishReport(directOutcome ?: DeleteOutcome(), gone,
                    stillThere.map { it.name + ": still present after the system request" })
            }
            pendingFiles = emptyList()
        }
    }

    private fun finishReport(direct: DeleteOutcome, extraGone: List<String>, extraFailures: List<String>) {
        val deletedUris = direct.deletedUris + extraGone
        _report.value = DeleteReport(
            deletedCount = deletedUris.size,
            deletedSize = deletedUris.sumOf { targetSizes[it] ?: 0L },
            failedMessages = direct.failed.map { it.first.name + ": " + it.second } + extraFailures
        )
        _deleting.value = false
    }
    fun dismissReport() { _report.value = null }
}
