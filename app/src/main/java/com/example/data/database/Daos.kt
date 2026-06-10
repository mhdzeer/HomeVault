package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemConfigDao {
    @Query("SELECT * FROM system_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<SystemConfig?>

    @Query("SELECT * FROM system_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): SystemConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: SystemConfig)
}

@Dao
interface BackupServerDao {
    @Query("SELECT * FROM backup_servers WHERE isPaired = 1")
    fun getPairedServersFlow(): Flow<List<BackupServer>>

    @Query("SELECT * FROM backup_servers WHERE isPaired = 1 LIMIT 1")
    suspend fun getActiveServerSync(): BackupServer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: BackupServer)

    @Query("DELETE FROM backup_servers WHERE id = :id")
    suspend fun deleteServer(id: Int)

    @Query("DELETE FROM backup_servers")
    suspend fun clearAll()
}

@Dao
interface MediaFileDao {
    @Query("SELECT * FROM media_files ORDER BY dateAdded DESC")
    fun getAllMediaFlow(): Flow<List<MediaFile>>

    @Query("SELECT * FROM media_files WHERE backupStatus = 'PENDING' ORDER BY dateAdded DESC")
    fun getPendingMediaFlow(): Flow<List<MediaFile>>

    @Query("SELECT * FROM media_files WHERE backupStatus = 'PENDING'")
    suspend fun getPendingMediaSync(): List<MediaFile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaFiles(files: List<MediaFile>)

    @Update
    suspend fun updateMediaFile(file: MediaFile)

    @Delete
    suspend fun deleteLocalFiles(files: List<MediaFile>)

    @Query("DELETE FROM media_files")
    suspend fun clearAll()
}

@Dao
interface BackupLogDao {
    @Query("SELECT * FROM backup_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<BackupLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BackupLog)

    @Query("DELETE FROM backup_logs")
    suspend fun clearAll()
}
