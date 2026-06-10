package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_config")
data class SystemConfig(
    @PrimaryKey val id: Int = 1,
    val trustedWifiSsid: String = "Home_Secure_Wi-Fi",
    val backupOnlyWhileCharging: Boolean = true,
    val backupOnlyOnWifi: Boolean = true,
    val minBatteryThreshold: Int = 20,
    val cleanupPolicy: String = "NEVER", // "IMMEDIATE", "7_DAYS", "30_DAYS", "NEVER"
    val isAutoBackupEnabled: Boolean = true,
    val isCloudSyncEnabled: Boolean = false,
    val cloudProvider: String = "Google Cloud Storage", // "Google Cloud Storage", "AWS S3", "Backblaze B2"
    val cloudAccessKey: String = "",
    val cloudSecretKey: String = ""
)

@Entity(tableName = "backup_servers")
data class BackupServer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serverName: String,
    val ipAddress: String,
    val port: Int,
    val isPaired: Boolean = false,
    val pairedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "media_files")
data class MediaFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val fileType: String, // "IMAGE" or "VIDEO"
    val dateAdded: Long,
    val sha256: String,
    val backupStatus: String, // "PENDING", "BACKING_UP", "COMPLETED"
    val backupTimestamp: Long? = null,
    val keywords: String? = null, // e.g. "vacation, nature", "trip, beach"
    val deviceName: String = "This Device" // e.g. "Google Pixel 8", "John's iPhone 15", "Google Photos Import"
)

@Entity(tableName = "backup_logs")
data class BackupLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val filesCount: Int,
    val totalBytes: Long,
    val status: String, // "SUCCESS", "FAILED"
    val message: String
)
