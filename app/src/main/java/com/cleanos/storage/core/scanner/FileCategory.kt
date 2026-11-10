package com.cleanos.storage.core.scanner

/** File categories shown on the category-selection screen.
 *  Classification uses MIME type first, then file extension (per Milestone 1 spec E). */
enum class FileCategory(val label: String, val mimePrefixes: List<String>, val mimeTypes: Set<String>, val extensions: Set<String>) {
    PHOTOS("Photos", listOf("image/"), emptySet(), setOf("jpg","jpeg","png","webp","heic","heif","gif","bmp","avif")),
    VIDEOS("Videos", listOf("video/"), emptySet(), setOf("mp4","mkv","mov","avi","webm","3gp")),
    PDFS("PDFs", emptyList(), setOf("application/pdf"), setOf("pdf")),
    DOCUMENTS("Documents", emptyList(), setOf(
        "application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain","application/rtf","text/rtf","application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint","application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/csv","application/vnd.oasis.opendocument.text","application/vnd.oasis.opendocument.spreadsheet"),
        setOf("doc","docx","txt","rtf","xls","xlsx","ppt","pptx","csv","odt","ods")),
    AUDIO("Audio", listOf("audio/"), emptySet(), setOf("mp3","wav","aac","m4a","flac","ogg","opus")),
    APKS("APK files", emptyList(), setOf("application/vnd.android.package-archive"), setOf("apk","xapk","apks")),
    ARCHIVES("Archives", emptyList(), setOf(
        "application/zip","application/x-zip-compressed","application/x-rar-compressed","application/vnd.rar",
        "application/x-7z-compressed","application/x-tar","application/gzip"),
        setOf("zip","rar","7z","tar","gz","tgz")),
    OTHER("Other files", emptyList(), emptySet(), emptySet());

    fun matches(mimeType: String?, fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val mime = mimeType?.lowercase()
        if (mime != null && (mimePrefixes.any { mime.startsWith(it) } || mime in mimeTypes)) return true
        return ext in extensions
    }

    companion object {
        /** Returns the single best category for a file, or OTHER when nothing matches. */
        fun classify(mimeType: String?, fileName: String): FileCategory =
            entries.firstOrNull { it != OTHER && it.matches(mimeType, fileName) } ?: OTHER
    }
}
