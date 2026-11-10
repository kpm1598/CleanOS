package com.cleanos.storage.features.swipecleaner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanos.storage.core.utils.FileOpener
import com.cleanos.storage.core.utils.Formatters
import com.cleanos.storage.ui.components.InfoCard

@Composable
fun KeepListScreen(vm: SwipeViewModel = viewModel()) {
    val keep by vm.keepList.collectAsState()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Keep list", style = MaterialTheme.typography.headlineMedium)
        Text("Kept files are never deleted by CleanOS and do not appear again in this review session.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (keep.isEmpty()) Text("Nothing kept yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(keep, key = { it.uriString }) { file ->
                InfoCard(Modifier.fillMaxWidth()) {
                    Text(file.name, style = MaterialTheme.typography.titleMedium)
                    Text("${file.category.label} · ${Formatters.size(file.sizeBytes)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row {
                        TextButton(onClick = { FileOpener.open(context, file) }) { Text("Preview") }
                        TextButton(onClick = { vm.moveKeepToUndecided(file) }) { Text("Move back to review") }
                    }
                }
            }
        }
    }
}
