package com.cleanos.storage.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cleanos.storage.core.scanner.FileCategory

enum class Decision { UNDECIDED, KEEP, DELETE }
enum class FileSource { MEDIASTORE, SAF }

/** One scanned file. Metadata only - file contents are never loaded into RAM or copied anywhere. */
@Entity(tableName = "scanned_files")
data class ScannedFile(
    @PrimaryKey val uriString: String,
    val name: String,
    val path: String?,
    val mimeType: String?,
    val sizeBytes: Long,
    val modifiedEpochSec: Long,
    val category: FileCategory,
    val source: FileSource,
    val decision: Decision = Decision.UNDECIDED,
    val queueSelected: Boolean = true
)
