package com.cleanos.storage.features.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanos.storage.core.scanner.FileCategory
import com.cleanos.storage.features.scanner.ScannerViewModel
import com.cleanos.storage.ui.components.InfoCard
import com.cleanos.storage.ui.components.categoryIcon
import androidx.compose.material3.Icon

@Composable
fun CategorySelectionScreen(onStartScan: () -> Unit, vm: ScannerViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("What should CleanOS scan?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text("Only the categories you select are scanned. Protected app and system folders are always skipped.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row {
            TextButton(onClick = { vm.selectAll() }) { Text("Select all") }
            TextButton(onClick = { vm.deselectAll() }) { Text("Deselect all") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(FileCategory.entries) { category ->
                InfoCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(categoryIcon(category), contentDescription = category.label, tint = MaterialTheme.colorScheme.primary)
                        Text(category.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Checkbox(checked = category in state.selected, onCheckedChange = { vm.toggle(category) })
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onStartScan, enabled = state.selected.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("Start scanning (${state.selected.size} categories)")
        }
    }
}
