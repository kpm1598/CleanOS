package com.cleanos.storage.features.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.cleanos.storage.CleanOSApp
import com.cleanos.storage.core.permissions.PermissionManager
import com.cleanos.storage.ui.components.InfoCard

/** First-launch permission onboarding.
 *  Explains, then offers: (1) All-files access via Android Settings (best coverage, optional),
 *  (2) standard media permissions, (3) a SAF folder the user picks themselves.
 *  The user can continue with whatever access they granted - the app degrades gracefully. */
@Composable
fun PermissionScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CleanOSApp
    var refreshTick by remember { mutableIntStateOf(0) }
    // Re-check grants every time the user returns from Settings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshTick++ }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refreshTick++ }
    val safLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            app.repository.prefs.safTreeUri = uri.toString()
        }
        refreshTick++
    }
    val allFiles = PermissionManager.hasAllFilesAccess()
    val media = PermissionManager.hasAnyMediaPermission(context)
    val saf = app.repository.prefs.safTreeUri != null
    @Suppress("UNUSED_EXPRESSION") refreshTick
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("Welcome to CleanOS", style = MaterialTheme.typography.headlineMedium)
        Text("CleanOS works entirely on this phone. No account, no ads, no internet, nothing leaves your device.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        InfoCard(Modifier.fillMaxWidth()) {
            Text("Why storage access?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("To find your photos, videos, documents and downloads, CleanOS needs permission to look at shared storage. " +
                "It only reads file names, sizes and dates. It never uploads anything, and it never deletes a file unless you swipe it into the delete queue and confirm the deletion yourself.")
        }
        InfoCard(Modifier.fillMaxWidth()) {
            Text(if (allFiles) "✓ All-files access granted" else "All-files access (recommended)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Lets CleanOS scan downloads, documents, APKs and archives across shared storage. Android opens a Settings page - turn on \"Allow access to manage all files\" for CleanOS, then come back.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            if (!allFiles) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = { context.startActivity(PermissionManager.allFilesAccessIntent(context)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Settings for all-files access")
                }
            }
        }
        InfoCard(Modifier.fillMaxWidth()) {
            Text(if (media) "✓ Media permissions granted" else "Media permissions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Standard Android permission for photos, videos and audio only. Enough for a media-only clean if you prefer not to grant all-files access.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            if (!media) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { mediaLauncher.launch(PermissionManager.runtimeMediaPermissions()) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Grant media permissions")
                }
            }
        }
        InfoCard(Modifier.fillMaxWidth()) {
            Text(if (saf) "✓ Folder selected" else "Pick a folder (optional)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Use Android's folder picker to let CleanOS scan one specific folder, for example Downloads.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { safLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (saf) "Change selected folder" else "Pick a folder")
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = { app.repository.prefs.onboardingDone = true; onDone() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (PermissionManager.hasUsableAccess(context) || saf) "Continue to dashboard" else "Continue without access (dashboard only)")
        }
        if (!PermissionManager.hasUsableAccess(context) && !saf) {
            Text("Without any access, CleanOS can show device storage totals but cannot scan files. You can grant access later from this screen flow by clearing app data, or from Android Settings.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
