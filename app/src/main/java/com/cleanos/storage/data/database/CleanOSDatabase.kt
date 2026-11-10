package com.cleanos.storage.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.cleanos.storage.core.scanner.FileCategory
import com.cleanos.storage.data.models.Decision
import com.cleanos.storage.data.models.FileSource
import com.cleanos.storage.data.models.ScannedFile

class Converters {
    @TypeConverter fun categoryToString(c: FileCategory): String = c.name
    @TypeConverter fun stringToCategory(s: String): FileCategory = runCatching { FileCategory.valueOf(s) }.getOrDefault(FileCategory.OTHER)
    @TypeConverter fun decisionToString(d: Decision): String = d.name
    @TypeConverter fun stringToDecision(s: String): Decision = runCatching { Decision.valueOf(s) }.getOrDefault(Decision.UNDECIDED)
    @TypeConverter fun sourceToString(s: FileSource): String = s.name
    @TypeConverter fun stringToSource(s: String): FileSource = runCatching { FileSource.valueOf(s) }.getOrDefault(FileSource.MEDIASTORE)
}

@Database(entities = [ScannedFile::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CleanOSDatabase : RoomDatabase() {
    abstract fun scannedFileDao(): ScannedFileDao
    companion object {
        @Volatile private var instance: CleanOSDatabase? = null
        fun get(context: Context): CleanOSDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, CleanOSDatabase::class.java, "cleanos.db")
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
