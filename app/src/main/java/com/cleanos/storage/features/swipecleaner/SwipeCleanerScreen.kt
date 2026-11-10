package com.cleanos.storage.features.swipecleaner

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cleanos.storage.core.scanner.FileCategory
import com.cleanos.storage.core.utils.FileOpener
import com.cleanos.storage.core.utils.Formatters
import com.cleanos.storage.data.models.ScannedFile
import com.cleanos.storage.ui.components.CategoryIconBadge
import com.cleanos.storage.ui.theme.CleanOsCard
import com.cleanos.storage.ui.theme.CleanOsDelete
import com.cleanos.storage.ui.theme.CleanOsKeep
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val SWIPE_THRESHOLD = 320f

@Composable
fun SwipeCleanerScreen(onOpenQueue: () -> Unit, onDone: () -> Unit, vm: SwipeViewModel = viewModel()) {
    val queue by vm.queue.collectAsState()
    val deleteQueue by vm.deleteQueue.collectAsState()
    val canUndo by vm.canUndo.collectAsState()
    val paused by vm.paused.collectAsState()
    val reviewed by vm.reviewedCount.collectAsState()
    val context = LocalContext.current
    val current = queue.firstOrNull()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Review files", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = { vm.setPaused(!paused) }) { Text(if (paused) "Resume" else "Pause") }
        }
        Text("Swipe left to queue for deletion, right to keep. Swiping never deletes anything by itself.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Reviewed: $reviewed · Remaining: ${queue.size} · In delete queue: ${deleteQueue.size} (${Formatters.size(deleteQueue.sumOf { it.sizeBytes })})",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        if (paused) {
            Text("Session paused. Your decisions so far are saved.", color = CleanOsKeep)
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (current == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("All files reviewed", style = MaterialTheme.typography.titleLarge)
                    Text("Nothing left in this session. Check the delete queue before anything is removed.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                SwipeCard(file = current, enabled = !paused,
                    onSwipeLeft = { vm.swipeLeft(current) },
                    onSwipeRight = { vm.swipeRight(current) },
                    onTap = { FileOpener.open(context, current) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { if (current != null) vm.swipeLeft(current) }, enabled = current != null && !paused,
                colors = ButtonDefaults.buttonColors(containerColor = CleanOsDelete), modifier = Modifier.weight(1f)) { Text("Delete") }
            Button(onClick = { if (current != null) vm.swipeRight(current) }, enabled = current != null && !paused,
                colors = ButtonDefaults.buttonColors(containerColor = CleanOsKeep), modifier = Modifier.weight(1f)) { Text("Keep") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { vm.undo() }, enabled = canUndo, modifier = Modifier.weight(1f)) { Text("Undo") }
            OutlinedButton(onClick = onOpenQueue, modifier = Modifier.weight(1f)) { Text("Delete queue (${deleteQueue.size})") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { vm.resetSession() }) { Text("Reset session") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDone) { Text("Done for now") }
        }
    }
}

@Composable
private fun SwipeCard(file: ScannedFile, enabled: Boolean, onSwipeLeft: () -> Unit, onSwipeRight: () -> Unit, onTap: () -> Unit) {
    val offsetX = remember(file.uriString) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val fraction = (offsetX.value / SWIPE_THRESHOLD).coerceIn(-1f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth()
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer { rotationZ = fraction * 6f }
            .pointerInput(file.uriString, enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragEnd = {
                        when {
                            offsetX.value <= -SWIPE_THRESHOLD -> scope.launch {
                                offsetX.animateTo(-1400f, tween(180)); onSwipeLeft() }
                            offsetX.value >= SWIPE_THRESHOLD -> scope.launch {
                                offsetX.animateTo(1400f, tween(180)); onSwipeRight() }
                            else -> scope.launch { offsetX.animateTo(0f, tween(200)) }
                        }
                    },
                    onDragCancel = { scope.launch { offsetX.animateTo(0f, tween(200)) } },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                    }
                )
            }
            .clickable { onTap() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CleanOsCard)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                if (file.category == FileCategory.PHOTOS || file.category == FileCategory.VIDEOS) {
                    AsyncImage(model = file.uriString, contentDescription = file.name,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CategoryIconBadge(file.category)
                    Text(file.category.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                when {
                    fraction <= -0.25f -> Text("DELETE", color = CleanOsDelete,
                        style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
                    fraction >= 0.25f -> Text("KEEP", color = CleanOsKeep,
                        style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp))
                }
            }
            Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Type: ${file.mimeType ?: file.name.substringAfterLast('.', "").uppercase()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Size: ${Formatters.size(file.sizeBytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Modified: ${Formatters.date(file.modifiedEpochSec)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Location: ${file.path ?: file.uriString}", color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Tap the card to preview this file.", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
