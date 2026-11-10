package com.cleanos.storage.features.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleanos.storage.CleanOSApp
import com.cleanos.storage.core.scanner.FileCategory
import com.cleanos.storage.core.storage.StorageStats
import com.cleanos.storage.core.storage.StorageStatsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoryTotal(val category: FileCategory, val count: Int, val sizeBytes: Long)
data class DashboardState(
    val stats: StorageStats? = null,
    val breakdown: List<CategoryTotal> = emptyList(),
    val scannedCount: Int = 0,
    val scannedSize: Long = 0,
    val lastScanEpochMs: Long = 0,
    val keepCount: Int = 0,
    val deleteQueueCount: Int = 0,
    val deleteQueueSize: Long = 0
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as CleanOSApp).repository
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    init { refresh() }
    fun refresh() {
        val stats = StorageStatsProvider.externalStats() ?: StorageStatsProvider.internalStats()
        _state.value = _state.value.copy(stats = stats, lastScanEpochMs = repo.prefs.lastScanEpochMs)
        viewModelScope.launch {
            repo.observeAll().collect { files ->
                val breakdown = files.groupBy { it.category }.map { (cat, list) ->
                    CategoryTotal(cat, list.size, list.sumOf { it.sizeBytes })
                }.sortedByDescending { it.sizeBytes }
                _state.value = _state.value.copy(
                    stats = StorageStatsProvider.externalStats() ?: StorageStatsProvider.internalStats(),
                    breakdown = breakdown,
                    scannedCount = files.size,
                    scannedSize = files.sumOf { it.sizeBytes },
                    lastScanEpochMs = repo.prefs.lastScanEpochMs,
                    keepCount = files.count { it.decision.name == "KEEP" },
                    deleteQueueCount = files.count { it.decision.name == "DELETE" },
                    deleteQueueSize = files.filter { it.decision.name == "DELETE" }.sumOf { it.sizeBytes }
                )
            }
        }
    }
}
