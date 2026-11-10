package com.cleanos.storage.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cleanos.storage.core.scanner.FileCategory
import com.cleanos.storage.ui.theme.CleanOsCard

fun categoryIcon(category: FileCategory): ImageVector = when (category) {
    FileCategory.PHOTOS -> Icons.Filled.Photo
    FileCategory.VIDEOS -> Icons.Filled.Movie
    FileCategory.PDFS -> Icons.Filled.PictureAsPdf
    FileCategory.DOCUMENTS -> Icons.Filled.Description
    FileCategory.AUDIO -> Icons.Filled.AudioFile
    FileCategory.APKS -> Icons.Filled.Android
    FileCategory.ARCHIVES -> Icons.Filled.FolderZip
    FileCategory.OTHER -> Icons.Filled.InsertDriveFile
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CleanOsCard)) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
fun CategoryIconBadge(category: FileCategory) {
    Icon(categoryIcon(category), contentDescription = category.label, tint = MaterialTheme.colorScheme.primary)
}
