package com.cleanos.storage.features.deletequeue

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanos.storage.core.utils.FileOpener
import com.cleanos.storage.core.utils.Formatters
import com.cleanos.storage.ui.components.InfoCard
import com.cleanos.storage.ui.theme.CleanOsDelete

@Composable
fun DeleteQueueScreen(onBackToSwipe: () -> Unit, vm: DeleteQueueViewModel = viewModel()) {
    val queue by vm.queue.collectAsState()
    val deleting by vm.deleting.collectAsState()
    val report by vm.report.collectAsState()
    val pending by vm.pendingOutcome.collectAsState()
    val context = LocalContext.current
    var showConfirm by remember { mutableStateOf(false) }
    val selected = queue.filter { it.queueSelected }
    val selectedSize = selected.sumOf { it.sizeBytes }

    val senderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()) { result ->
        vm.onSystemRequestFinished(result.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(pending) {
        pending?.systemRequest?.let { senderLauncher.launch(IntentSenderRequest.Builder(it.intentSender).build()) }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Delete queue", style = MaterialTheme.typography.headlineMedium)
        Text("${queue.size} files marked · ${selected.size} selected · ${Formatters.size(selectedSize)} to be freed",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Nothing here has been deleted yet. Files are removed only after you confirm on the next step.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Row {
            TextButton(onClick = { vm.selectAll(true) }) { Text("Select all") }
            TextButton(onClick = { vm.selectAll(false) }) { Text("Deselect all") }
            TextButton(onClick = { vm.clearQueue() }, enabled = queue.isNotEmpty()) { Text("Clear queue") }
        }
        if (queue.isEmpty()) {
            Text("The delete queue is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(queue, key = { it.uriString }) { file ->
                InfoCard(Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = file.queueSelected, onCheckedChange = { vm.toggleSelected(file, it) })
                        Column(Modifier.weight(1f)) {
                            Text(file.name, style = MaterialTheme.typography.titleMedium)
                            Text("${file.category.label} · ${Formatters.size(file.sizeBytes)} · ${Formatters.date(file.modifiedEpochSec)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Row {
                        TextButton(onClick = { FileOpener.open(context, file) }) { Text("Preview") }
                        TextButton(onClick = { vm.removeFromQueue(file) }) { Text("Remove from queue") }
                    }
                }
            }
        }
        OutlinedButton(onClick = onBackToSwipe, modifier = Modifier.fillMaxWidth()) { Text("Return to swipe review") }
        Button(onClick = { showConfirm = true }, enabled = selected.isNotEmpty() && !deleting,
            colors = ButtonDefaults.buttonColors(containerColor = CleanOsDelete), modifier = Modifier.fillMaxWidth()) {
            Text(if (deleting) "Deleting..." else "Confirm deletion (${selected.size} files)")
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete ${selected.size} files?") },
            text = { Text("This will permanently delete ${selected.size} files (${Formatters.size(selectedSize)}). This cannot be undone. Kept files are not affected.") },
            confirmButton = {
                Button(onClick = { showConfirm = false; vm.deleteSelected() },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanOsDelete)) { Text("Delete permanently") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } }
        )
    }
    report?.let { r ->
        AlertDialog(
            onDismissRequest = { vm.dismissReport() },
            title = { Text("Deletion finished") },
            text = {
                Column {
                    Text("Deleted: ${r.deletedCount} files (${Formatters.size(r.deletedSize)} freed). Storage and scan results have been refreshed.")
                    if (r.failedMessages.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Could not delete:", color = MaterialTheme.colorScheme.error)
                        r.failedMessages.take(10).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            },
            confirmButton = { Button(onClick = { vm.dismissReport() }) { Text("OK") } }
        )
    }
}
