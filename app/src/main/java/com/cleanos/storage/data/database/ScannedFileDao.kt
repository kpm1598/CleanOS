package com.cleanos.storage.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cleanos.storage.data.models.Decision
import com.cleanos.storage.data.models.ScannedFile
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedFileDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(files: List<ScannedFile>)

    @Query("SELECT * FROM scanned_files ORDER BY sizeBytes DESC")
    fun observeAll(): Flow<List<ScannedFile>>

    @Query("SELECT * FROM scanned_files WHERE decision = 'UNDECIDED' ORDER BY sizeBytes DESC")
    fun observeUndecided(): Flow<List<ScannedFile>>

    @Query("SELECT * FROM scanned_files WHERE decision = 'DELETE'")
    fun observeDeleteQueue(): Flow<List<ScannedFile>>

    @Query("SELECT * FROM scanned_files WHERE decision = 'KEEP' ORDER BY name")
    fun observeKeepList(): Flow<List<ScannedFile>>

    @Query("SELECT * FROM scanned_files")
    suspend fun getAll(): List<ScannedFile>

    @Query("UPDATE scanned_files SET decision = :decision WHERE uriString = :uri")
    suspend fun setDecision(uri: String, decision: Decision)

    @Query("UPDATE scanned_files SET queueSelected = :selected WHERE uriString = :uri")
    suspend fun setQueueSelected(uri: String, selected: Boolean)

    @Query("UPDATE scanned_files SET queueSelected = :selected WHERE decision = 'DELETE'")
    suspend fun setAllQueueSelected(selected: Boolean)

    @Query("DELETE FROM scanned_files WHERE uriString IN (:uris)")
    suspend fun deleteByUris(uris: List<String>)

    @Query("DELETE FROM scanned_files WHERE uriString = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM scanned_files")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM scanned_files")
    suspend fun count(): Int
}
