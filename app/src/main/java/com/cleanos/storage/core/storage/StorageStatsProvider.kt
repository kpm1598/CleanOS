package com.cleanos.storage.core.storage

import android.os.Environment
import android.os.StatFs
import java.io.File

data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val usedPercent: Int
)

/** Real whole-device storage figures via StatFs. These describe the whole device,
 *  NOT only the files CleanOS can access - the dashboard labels them as such. */
object StorageStatsProvider {
    fun internalStats(): StorageStats = statsFor(Environment.getDataDirectory())
    fun externalStats(): StorageStats? = try {
        Environment.getExternalStorageDirectory()?.let { statsFor(it) }
    } catch (e: Exception) { null }
    private fun statsFor(dir: File): StorageStats {
        val stat = StatFs(dir.path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = (total - free).coerceAtLeast(0)
        return StorageStats(total, used, free, if (total > 0) ((used.toDouble() / total) * 100).toInt() else 0)
    }
}
