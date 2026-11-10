package com.cleanos.storage.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    fun size(bytes: Long): String {
        if (bytes < 0) return "Unknown"
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unit = -1
        while (value >= 1024 && unit < units.lastIndex) { value /= 1024; unit++ }
        return String.format(Locale.US, "%.1f %s", value, units[maxOf(unit, 0)])
    }
    fun date(epochSeconds: Long): String {
        if (epochSeconds <= 0) return "Unknown date"
        return SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(epochSeconds * 1000))
    }
    fun percent(used: Long, total: Long): Int =
        if (total <= 0) 0 else ((used.toDouble() / total.toDouble()) * 100).toInt().coerceIn(0, 100)
}
