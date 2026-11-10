package com.cleanos.storage.features.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleanos.storage.CleanOSApp
import com.cleanos.storage.core.scanner.FileCategory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScannerState(
    val selected: Set<FileCategory> = FileCategory.entries.toSet(),
    val scanning: Boolean = false,
    val done: Boolean = false,
    val found: Int = 0,
    val currentCategory: FileCategory? = null,
    val error: String? = null
)

class ScannerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as CleanOSApp).repository
    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state.asStateFlow()
    private var scanJob: Job? = null

    fun toggle(category: FileCategory) {
        val current = _state.value.selected
        _state.value = _state.value.copy(selected = if (category in current) current - category else current + category)
    }
    fun selectAll() { _state.value = _state.value.copy(selected = FileCategory.entries.toSet()) }
    fun deselectAll() { _state.value = _state.value.copy(selected = emptySet()) }

    fun startScan() {
        val categories = _state.value.selected
        if (categories.isEmpty() || _state.value.scanning) return
        _state.value = _state.value.copy(scanning = true, done = false, found = 0, error = null)
        scanJob = viewModelScope.launch {
            try {
                repo.scan(categories) { progress ->
                    _state.value = _state.value.copy(
                        found = maxOf(_state.value.found, progress.foundSoFar),
                        currentCategory = progress.currentCategory ?: _state.value.currentCategory,
                        error = progress.error ?: _state.value.error,
                        scanning = !progress.done,
                        done = progress.done
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _state.value = _state.value.copy(scanning = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(scanning = false, error = e.message ?: "Scan failed")
            }
        }
    }
    fun cancelScan() { scanJob?.cancel(); _state.value = _state.value.copy(scanning = false) }
}
