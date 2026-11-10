package com.cleanos.storage.features.swipecleaner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleanos.storage.CleanOSApp
import com.cleanos.storage.data.models.Decision
import com.cleanos.storage.data.models.ScannedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Swipe review session.
 *  Decisions are written to Room the moment a swipe happens, so leaving the screen or the
 *  process being killed loses nothing. The undo stack is in-memory for the current session,
 *  but every undo also reverts the persisted decision.
 *  KEEP files are filtered out of the undecided stream, so a kept file never reappears in
 *  the same session (resetSession() is the only way back, and it deletes nothing). */
class SwipeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as CleanOSApp).repository
    val queue: StateFlow<List<ScannedFile>> = repo.observeUndecided()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val keepList: StateFlow<List<ScannedFile>> = repo.observeKeepList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val deleteQueue: StateFlow<List<ScannedFile>> = repo.observeDeleteQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val undoStack = ArrayDeque<String>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _paused = MutableStateFlow(repo.prefs.swipePaused)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()
    private val _reviewedCount = MutableStateFlow(0)
    val reviewedCount: StateFlow<Int> = _reviewedCount.asStateFlow()

    fun swipeLeft(file: ScannedFile) = decide(file, Decision.DELETE)
    fun swipeRight(file: ScannedFile) = decide(file, Decision.KEEP)
    private fun decide(file: ScannedFile, decision: Decision) {
        if (_paused.value) return
        undoStack.addLast(file.uriString)
        _canUndo.value = true
        _reviewedCount.value += 1
        viewModelScope.launch { repo.setDecision(file, decision) }
    }
    fun undo() {
        val uri = undoStack.removeLastOrNull() ?: return
        _canUndo.value = undoStack.isNotEmpty()
        _reviewedCount.value = (_reviewedCount.value - 1).coerceAtLeast(0)
        viewModelScope.launch {
            repo.getAll().firstOrNull { it.uriString == uri }?.let { repo.setDecision(it, Decision.UNDECIDED) }
        }
    }
    fun setPaused(value: Boolean) { _paused.value = value; repo.prefs.swipePaused = value }
    fun resetSession() {
        undoStack.clear(); _canUndo.value = false; _reviewedCount.value = 0
        viewModelScope.launch { repo.resetSession() }
    }
    fun moveKeepToUndecided(file: ScannedFile) {
        viewModelScope.launch { repo.setDecision(file, Decision.UNDECIDED) }
    }
}
