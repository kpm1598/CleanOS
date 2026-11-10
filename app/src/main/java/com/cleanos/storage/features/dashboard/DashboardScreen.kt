package com.cleanos.storage.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanos.storage.core.utils.Formatters
import com.cleanos.storage.ui.components.CategoryIconBadge
import com.cleanos.storage.ui.components.InfoCard
import com.cleanos.storage.ui.components.StatRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onStartScan: () -> Unit,
    onOpenSwipe: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenKeep: () -> Unit,
    vm: DashboardViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CleanOS", style = MaterialTheme.typography.headlineMedium)
        Text("Your storage, reviewed by you. Nothing is ever deleted without your confirmation.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        val stats = state.stats
        InfoCard(Modifier.fillMaxWidth()) {
            Text("Whole device storage", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Real figures from Android for the entire device - including apps and system files CleanOS cannot access.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            if (stats != null) {
                StatRow("Total storage", Formatters.size(stats.totalBytes))
                StatRow("Used", Formatters.size(stats.usedBytes))
                StatRow("Available", Formatters.size(stats.freeBytes))
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { stats.usedPercent / 100f }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text("${stats.usedPercent}% used", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else Text("Storage information unavailable on this device.")
        }
        InfoCard(Modifier.fillMaxWidth()) {
            Text("Files CleanOS can access", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            StatRow("Scanned files", state.scannedCount.toString())
            StatRow("Scanned size", Formatters.size(state.scannedSize))
            StatRow("Last scan", if (state.lastScanEpochMs > 0)
                SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(state.lastScanEpochMs)) else "Not scanned yet")
            if (state.breakdown.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                state.breakdown.forEach { total ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryIconBadge(total.category)
                            Text(total.category.label)
                        }
                        Text("${total.count} files · ${Formatters.size(total.sizeBytes)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
        Button(onClick = onStartScan, modifier = Modifier.fillMaxWidth()) { Text("Start Scan") }
        if (state.scannedCount > 0) {
            OutlinedButton(onClick = onOpenSwipe, modifier = Modifier.fillMaxWidth()) { Text("Review files (swipe)") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onOpenQueue, modifier = Modifier.weight(1f)) {
                Text("Delete queue (${state.deleteQueueCount} · ${Formatters.size(state.deleteQueueSize)})")
            }
            OutlinedButton(onClick = onOpenKeep, modifier = Modifier.weight(1f)) {
                Text("Keep list (${state.keepCount})")
            }
        }
    }
}
