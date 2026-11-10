package com.cleanos.storage

import com.cleanos.storage.core.scanner.FileCategory
import com.cleanos.storage.core.utils.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test

class FileCategoryTest {
    @Test fun photosByMimeAndExtension() {
        assertEquals(FileCategory.PHOTOS, FileCategory.classify("image/jpeg", "a.jpg"))
        assertEquals(FileCategory.PHOTOS, FileCategory.classify(null, "b.HEIC"))
    }
    @Test fun videosDocumentsAudio() {
        assertEquals(FileCategory.VIDEOS, FileCategory.classify(null, "clip.mkv"))
        assertEquals(FileCategory.DOCUMENTS, FileCategory.classify(null, "notes.docx"))
        assertEquals(FileCategory.AUDIO, FileCategory.classify("audio/mpeg", "song.bin"))
    }
    @Test fun pdfApkArchiveOther() {
        assertEquals(FileCategory.PDFS, FileCategory.classify("application/pdf", "x.bin"))
        assertEquals(FileCategory.APKS, FileCategory.classify(null, "app.apk"))
        assertEquals(FileCategory.ARCHIVES, FileCategory.classify(null, "backup.7z"))
        assertEquals(FileCategory.OTHER, FileCategory.classify(null, "data.xyz"))
    }
    @Test fun formatterSizes() {
        assertEquals("512 B", Formatters.size(512))
        assertEquals("1.0 KB", Formatters.size(1024))
        assertEquals("Unknown", Formatters.size(-1))
    }
}
