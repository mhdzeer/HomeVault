package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.BackupLog
import com.example.data.database.BackupServer
import com.example.data.database.MediaFile
import com.example.data.database.SystemConfig
import com.example.data.repository.BackupRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BackupRepository
    
    // Live UI parameters
    val config: StateFlow<SystemConfig>
    val pairedServers: StateFlow<List<BackupServer>>
    val allMedia: StateFlow<List<MediaFile>>
    val pendingMedia: StateFlow<List<MediaFile>>
    val logs: StateFlow<List<BackupLog>>

    // Device state states
    private val _isWifiConnected = MutableStateFlow(false)
    val isWifiConnected = _isWifiConnected.asStateFlow()

    private val _wifiSsid = MutableStateFlow("Disconnected")
    val wifiSsid = _wifiSsid.asStateFlow()

    private val _batteryLevel = MutableStateFlow(50)
    val batteryLevel = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging = _isCharging.asStateFlow()

    // Backup actions states
    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp = _isBackingUp.asStateFlow()

    private val _backupProgress = MutableStateFlow(0f)
    val backupProgress = _backupProgress.asStateFlow()

    private val _backupStatusMessage = MutableStateFlow("Idle")
    val backupStatusMessage = _backupStatusMessage.asStateFlow()

    private val _currentBackingUpFile = MutableStateFlow<MediaFile?>(null)
    val currentBackingUpFile = _currentBackingUpFile.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BackupRepository(database)

        config = repository.configFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SystemConfig()
        )

        pairedServers = repository.pairedServersFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allMedia = repository.allMediaFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        pendingMedia = repository.pendingMediaFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        logs = repository.logsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        refreshDeviceStatus()
        
        // Populate and scan local media on launch
        viewModelScope.launch {
            repository.discoverLocalMedia(getApplication())
        }
    }

    fun refreshDeviceStatus() {
        try {
            val ctx = getApplication<Application>()
            _isWifiConnected.value = repository.isConnectedToWifi(ctx)
            _wifiSsid.value = repository.getWifiSsid(ctx)
            _batteryLevel.value = repository.getBatteryLevel(ctx)
            _isCharging.value = repository.isDeviceCharging(ctx)
        } catch (e: Exception) {
            _isWifiConnected.value = false
            _wifiSsid.value = "Disconnected"
            _batteryLevel.value = 50
            _isCharging.value = false
        }
    }

    fun updateConfig(updated: SystemConfig) {
        viewModelScope.launch {
            repository.saveConfig(updated)
        }
    }

    fun scanMediaFiles(force: Boolean = false) {
        viewModelScope.launch {
            repository.discoverLocalMedia(getApplication(), forceReScan = force)
            refreshDeviceStatus()
        }
    }

    fun pairWithPc(serverName: String, ip: String, port: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (serverName.isBlank() || ip.isBlank() || port <= 0) {
                onError("Please fill in all pairing details with valid parameters.")
                return@launch
            }
            try {
                _backupStatusMessage.value = "Pairing with $serverName..."
                val success = repository.pairServer(serverName, ip, port)
                if (success) {
                    onSuccess()
                } else {
                    onError("Could not discover server at $ip. Ensure PC app is running.")
                }
            } catch (e: Exception) {
                onError(e.message ?: "An unknown pairing error occurred.")
            } finally {
                _backupStatusMessage.value = "Idle"
            }
        }
    }

    fun unpairServer(serverId: Int) {
        viewModelScope.launch {
            repository.unpairServer(serverId)
        }
    }

    fun runStorageCleanup(force: Boolean = false) {
        viewModelScope.launch {
            repository.handleCleanup(getApplication(), force = force)
        }
    }

    fun importFromCloud(service: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.importFromCloud(service)
            onComplete(count)
        }
    }

    fun clearHistoryAndReset() {
        viewModelScope.launch {
            repository.clearAllData()
            // Reset to defaults
            repository.saveConfig(SystemConfig())
            // Re-discover base media mock items
            repository.discoverLocalMedia(getApplication(), forceReScan = true)
        }
    }

    fun triggerBackup(onCompletion: (String?) -> Unit) {
        if (_isBackingUp.value) return
        
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupProgress.value = 0f
            _backupStatusMessage.value = "Preparing private backup..."
            refreshDeviceStatus()

            val errorMsg = repository.startManualBackup(
                context = getApplication(),
                onFileBackupStarted = { file ->
                    _currentBackingUpFile.value = file
                    _backupStatusMessage.value = "Transferring ${file.fileName}..."
                },
                onFileBackupProgress = { _, progress ->
                    _backupProgress.value = progress
                }
            )

            _isBackingUp.value = false
            _backupProgress.value = 0f
            _currentBackingUpFile.value = null
            _backupStatusMessage.value = "Idle"
            
            onCompletion(errorMsg)
        }
    }
}
