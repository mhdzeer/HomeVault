package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import com.example.data.database.BackupLog
import com.example.data.database.BackupServer
import com.example.data.database.MediaFile
import com.example.data.database.SystemConfig
import com.example.data.repository.BackupRepository
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupApp(viewModel: BackupViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val config by viewModel.config.collectAsStateWithLifecycle()
    val pairedServers by viewModel.pairedServers.collectAsStateWithLifecycle()
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val pendingMedia by viewModel.pendingMedia.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    val isWifiConnected by viewModel.isWifiConnected.collectAsStateWithLifecycle()
    val wifiSsid by viewModel.wifiSsid.collectAsStateWithLifecycle()
    val batteryLevel by viewModel.batteryLevel.collectAsStateWithLifecycle()
    val isCharging by viewModel.isCharging.collectAsStateWithLifecycle()

    val isBackingUp by viewModel.isBackingUp.collectAsStateWithLifecycle()
    val backupProgress by viewModel.backupProgress.collectAsStateWithLifecycle()
    val backupStatusMessage by viewModel.backupStatusMessage.collectAsStateWithLifecycle()
    val currentBackingUpFile by viewModel.currentBackingUpFile.collectAsStateWithLifecycle()

    var showPairDialog by remember { mutableStateOf(false) }

    // Periodic state refresh
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshDeviceStatus()
            delay(4000)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if (selectedTab == 0) Icons.Filled.CloudSync else Icons.Outlined.CloudSync, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(if (selectedTab == 1) Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary, contentDescription = "Gallery") },
                    label = { Text("Gallery") },
                    modifier = Modifier.testTag("nav_gallery")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(if (selectedTab == 2) Icons.AutoMirrored.Filled.ListAlt else Icons.AutoMirrored.Outlined.ListAlt, contentDescription = "Logs") },
                    label = { Text("Logs") },
                    modifier = Modifier.testTag("nav_logs")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(if (selectedTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardView(
                    viewModel = viewModel,
                    config = config,
                    pairedServers = pairedServers,
                    allMedia = allMedia,
                    pendingMedia = pendingMedia,
                    isWifiConnected = isWifiConnected,
                    wifiSsid = wifiSsid,
                    batteryLevel = batteryLevel,
                    isCharging = isCharging,
                    isBackingUp = isBackingUp,
                    backupProgress = backupProgress,
                    statusMsg = backupStatusMessage,
                    currentFile = currentBackingUpFile,
                    onPairClick = { showPairDialog = true }
                )
                1 -> GalleryView(
                    viewModel = viewModel,
                    allMedia = allMedia,
                    pairedServers = pairedServers,
                    isWifiConnected = isWifiConnected,
                    onScanClick = { viewModel.scanMediaFiles(force = true) }
                )
                2 -> LogsView(logs = logs, onClearClick = { viewModel.clearHistoryAndReset() })
                3 -> SettingsView(viewModel = viewModel, config = config)
            }

            if (showPairDialog) {
                PairComputerDialog(
                    onDismiss = { showPairDialog = false },
                    onPairSuccess = { name, ip, port ->
                        viewModel.pairWithPc(name, ip, port,
                            onSuccess = {
                                showPairDialog = false
                                Toast.makeText(context, "$name paired successfully!", Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD COMPOSABLE
// ==========================================
@Composable
fun DashboardView(
    viewModel: BackupViewModel,
    config: SystemConfig,
    pairedServers: List<BackupServer>,
    allMedia: List<MediaFile>,
    pendingMedia: List<MediaFile>,
    isWifiConnected: Boolean,
    wifiSsid: String,
    batteryLevel: Int,
    isCharging: Boolean,
    isBackingUp: Boolean,
    backupProgress: Float,
    statusMsg: String,
    currentFile: MediaFile?,
    onPairClick: () -> Unit
) {
    val context = LocalContext.current
    val completedCount = allMedia.count { it.backupStatus == "COMPLETED" || it.backupStatus == "DELETED" }
    val totalSize = allMedia.sumOf { it.fileSize }
    
    // Calculated based on database backupStatus flags
    val backedUpSize = allMedia.filter { it.backupStatus == "COMPLETED" || it.backupStatus == "DELETED" }.sumOf { it.fileSize }
    val cleanableCount = allMedia.count { it.backupStatus == "COMPLETED" }
    val cleanableSize = allMedia.filter { it.backupStatus == "COMPLETED" }.sumOf { it.fileSize }

    // Pre-requisite validation alerts
    val isWifiPrereqOk = !config.backupOnlyOnWifi || isWifiConnected
    val isPowerPrereqOk = !config.backupOnlyWhileCharging || isCharging
    val isBatteryPrereqOk = batteryLevel >= config.minBatteryThreshold
    val hasPairedDevice = pairedServers.isNotEmpty()

    val canBackupAuto = isWifiPrereqOk && isPowerPrereqOk && isBatteryPrereqOk && hasPairedDevice

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Brand
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HomeWork,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "HomeVault",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                }
                
                // Initials avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JD",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Prominent Backup Progress/State Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Top line with badge and percentage
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isBackingUp) "Backing up" else if (canBackupAuto) "Sync ready" else "Suspended",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Percentage or State text
                        Text(
                            text = if (isBackingUp) "${(backupProgress * 100).toInt()}%" else if (pendingMedia.isNotEmpty()) "${pendingMedia.size} left" else "Idle",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title Header text
                    Text(
                        text = if (isBackingUp) {
                            "Sending ${pendingMedia.size} items to ${pairedServers.firstOrNull()?.serverName ?: "PC"}"
                        } else if (pendingMedia.isNotEmpty()) {
                            "${pendingMedia.size} items pending backup"
                        } else {
                            "All photos secured offline"
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // SSid caption info
                    Text(
                        text = if (isWifiConnected) "Connected to: $wifiSsid" else "WiFi Disconnected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom elements: Progress bar or Back Up action button
                    if (isBackingUp) {
                        // Linear Progress Indicator matching HTML progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(backupProgress)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Transfer active logs label
                        Text(
                            text = currentFile?.fileName ?: "Compressing file streams...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        // Elegant action trigger button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    viewModel.triggerBackup { err ->
                                        if (err != null) {
                                            val readableErr = when (err) {
                                                "NO_PAIRED_SERVER" -> "Ensure you pair with your PC before backup can run!"
                                                "NOT_ON_WIFI" -> "Requires connection to a trusted Wi-Fi network (configured in Settings)."
                                                "NOT_CHARGING" -> "Backup is configured to only execute while charging."
                                                "LOW_BATTERY" -> "Battery too low to backup safely. Hook up to power!"
                                                "NO_NEW_MEDIA" -> "No files found pending backup. You're up-to-date!"
                                                else -> "Backup failed: $err"
                                            }
                                            Toast.makeText(context, readableErr, Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Backup complete!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isBackingUp && pendingMedia.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("backup_now_button")
                            ) {
                                Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Back Up")
                            }
                        }
                    }
                }
            }
        }

        // Outlined Stats Cards Row (Safe Storage / Redundant space tracking)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Safe Storage Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(128.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Light blue checkmark indicator circular background
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFC2E7FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color(0xFF001D35),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Labels and values
                        Column {
                            Text(
                                text = "SAFE STORAGE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = BackupRepository.formatBytes(backedUpSize),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Redundant / Cleanable Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(128.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Pale yellow indicator background
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFDF2D0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = Color(0xFF1D1B16),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Labels and values
                        Column {
                            Text(
                                text = "REDUNDANT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = BackupRepository.formatBytes(cleanableSize),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (cleanableSize > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Clean space card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Free up device space",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (cleanableCount > 0) "Delete $cleanableCount backed-up files" else "All copies safely cleaned up",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.runStorageCleanup(force = true)
                            Toast.makeText(context, "$cleanableCount files cleaned up successfully!", Toast.LENGTH_SHORT).show()
                        },
                        enabled = cleanableCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("clean_button")
                    ) {
                        Text("Clean", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // System Controls Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SYSTEM CONTROLS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Wifi constraint display indicator
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isWifiPrereqOk) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.dp,
                                if (isWifiPrereqOk) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isWifiPrereqOk) MaterialTheme.colorScheme.primary
                                        else Color(0xFFC4C7C5)
                                    )
                            )
                            Text(
                                "WiFi Only",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isWifiPrereqOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Charging constraint indicator
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isPowerPrereqOk) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.dp,
                                if (isPowerPrereqOk) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPowerPrereqOk) MaterialTheme.colorScheme.primary
                                        else Color(0xFFC4C7C5)
                                    )
                            )
                            Text(
                                "Charging",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isPowerPrereqOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Paired Computer Info Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Paired Backup Computer",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (pairedServers.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Computer,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Desktop Host Paired",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onPairClick,
                                modifier = Modifier.testTag("pair_computer_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(50)
                            ) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pair via QR Code")
                            }
                        }
                    } else {
                        val firstPc = pairedServers.first()
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DesktopMac,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = firstPc.serverName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "IP: ${firstPc.ipAddress}:${firstPc.port}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.unpairServer(firstPc.id) },
                                    modifier = Modifier.testTag("unpair_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Unpair server",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // SAME WI-FI CONNECTION STATUS INDICATOR
                            val indicatorColor = if (isWifiConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            val indicatorBg = if (isWifiConnected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("connection_status_indicator"),
                                color = indicatorBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, indicatorColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isWifiConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                                        contentDescription = null,
                                        tint = indicatorColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isWifiConnected) "Same Wi-Fi Network Connected" else "Same Wi-Fi Network Disconnected",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = indicatorColor
                                        )
                                        Text(
                                            text = if (isWifiConnected) {
                                                "Active connection to ${firstPc.serverName} on SSID: $wifiSsid"
                                            } else {
                                                "Connect to the same Wi-Fi network as ${firstPc.serverName} to start automated backups."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isWifiConnected) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(indicatorColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. MY PHOTOS GALLERY VIEW
// ==========================================
@Composable
fun GalleryView(
    viewModel: BackupViewModel,
    allMedia: List<MediaFile>,
    pairedServers: List<BackupServer>,
    isWifiConnected: Boolean,
    onScanClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDevice by remember { mutableStateOf("All Devices") }
    var sortBy by remember { mutableStateOf(0) } // 0 = Date Newest, 1 = Date Oldest, 2 = Name A-Z, 3 = Name Z-A
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeviceMenu by remember { mutableStateOf(false) }

    var filterMode by remember { mutableStateOf(0) } // 0 = All local, 1 = Pending, 2 = Synced, 3 = PC Wifi View

    var selectedMediaForLightbox by remember { mutableStateOf<MediaFile?>(null) }

    val deviceList = remember(allMedia) {
        listOf("All Devices") + allMedia.map { it.deviceName }.distinct()
    }

    val filteredAndSortedList = remember(allMedia, filterMode, searchQuery, selectedDevice, sortBy) {
        val step1 = when (filterMode) {
            1 -> allMedia.filter { it.backupStatus == "PENDING" }
            2 -> allMedia.filter { it.backupStatus == "COMPLETED" }
            3 -> allMedia.filter { it.backupStatus == "COMPLETED" || it.backupStatus == "DELETED" } // On PC hub server
            else -> allMedia.filter { it.backupStatus != "DELETED" }
        }

        val step2 = if (searchQuery.isBlank()) {
            step1
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd MMMM dd yyyy", Locale.getDefault())
            step1.filter { file ->
                val dateStr = sdf.format(Date(file.dateAdded))
                file.fileName.contains(searchQuery, ignoreCase = true) ||
                        (file.keywords?.contains(searchQuery, ignoreCase = true) == true) ||
                        dateStr.contains(searchQuery, ignoreCase = true)
            }
        }

        val step3 = if (selectedDevice == "All Devices") {
            step2
        } else {
            step2.filter { it.deviceName == selectedDevice }
        }

        when (sortBy) {
            1 -> step3.sortedBy { it.dateAdded }
            2 -> step3.sortedBy { it.fileName }
            3 -> step3.sortedByDescending { it.fileName }
            else -> step3.sortedByDescending { it.dateAdded }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (filterMode == 3) "PC Server Vault" else "Local Media",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredAndSortedList.size} Photos & Videos",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(
                onClick = onScanClick,
                modifier = Modifier.testTag("scan_photos_button")
            ) {
                Icon(Icons.Filled.Sync, contentDescription = "Rescan MediaStore")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search & Filters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search date, keywords...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1.dp.value)
                    .height(56.dp)
                    .testTag("gallery_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Device Filter Trigger
            Box {
                FilterChip(
                    selected = selectedDevice != "All Devices",
                    onClick = { showDeviceMenu = true },
                    label = { Text(selectedDevice.take(12) + if (selectedDevice.length > 12) ".." else "") },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                    modifier = Modifier.testTag("device_filter_chip")
                )
                DropdownMenu(
                    expanded = showDeviceMenu,
                    onDismissRequest = { showDeviceMenu = false }
                ) {
                    deviceList.forEach { dev ->
                        DropdownMenuItem(
                            text = { Text(dev) },
                            onClick = {
                                selectedDevice = dev
                                showDeviceMenu = false
                            }
                        )
                    }
                }
            }

            // Sort Trigger
            Box {
                FilterChip(
                    selected = sortBy != 0,
                    onClick = { showSortMenu = true },
                    label = { 
                        Text(
                            when(sortBy) {
                                1 -> "Oldest"
                                2 -> "Name A-Z"
                                3 -> "Name Z-A"
                                else -> "Newest"
                            }
                        ) 
                    },
                    trailingIcon = { Icon(Icons.Filled.Sort, null) },
                    modifier = Modifier.testTag("sort_filter_chip")
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    listOf(
                        0 to "Date: Newest",
                        1 to "Date: Oldest",
                        2 to "Filename: A-Z",
                        3 to "Filename: Z-A"
                    ).forEach { (idx, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                sortBy = idx
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Toggle filters tab row
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            val segments = listOf("All", "Pending", "Synced", "PC Vault")
            segments.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = filterMode == index,
                    onClick = { filterMode = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 4)
                ) {
                    Text(label, fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filterMode == 3 && (!isWifiConnected || pairedServers.isEmpty())) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WifiOff,
                            contentDescription = "Wi-Fi Connection Required",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Same Wi-Fi Connection Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (pairedServers.isEmpty()) {
                                "First register and pair your home computer with HomeVault Photos using the Pair Dialog on the Dashboard tab."
                            } else {
                                "Ensure your phone is connected to the same Wi-Fi network as your paired home computer (${pairedServers.first().serverName}) to access and stream backed up vault files."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Display live banner if filterMode == 3 (Server Stream Active!)
            if (filterMode == 3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F5E9))
                        .border(1.dp, Color(0xFF81C784), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                "Live Stream: Connected to PC Vault",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                "Connected with server ${pairedServers.first().serverName} at ${pairedServers.first().ipAddress}:${pairedServers.first().port}",
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            if (filteredAndSortedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching items found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredAndSortedList) { media ->
                        MediaThumbnailItem(media = media, onClick = { selectedMediaForLightbox = media })
                    }
                }
            }
        }
    }

    selectedMediaForLightbox?.let { media ->
        LightboxDialog(
            media = media,
            isWifiConnected = isWifiConnected,
            serverName = pairedServers.firstOrNull()?.serverName,
            onDismiss = { selectedMediaForLightbox = null }
        )
    }
}

@Composable
fun MediaThumbnailItem(media: MediaFile, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (media.fileType == "VIDEO") {
                            listOf(Color(0xFF2C3E50), Color(0xFF3498DB))
                        } else {
                            listOf(Color(0xFF8E44AD), Color(0xFFF39C12))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    imageVector = if (media.fileType == "VIDEO") Icons.Filled.PlayCircle else Icons.Filled.Image,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = media.fileName.takeLast(10),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Overlay status badge
        Box(
            modifier = Modifier
                .padding(6.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(
                    when (media.backupStatus) {
                        "COMPLETED" -> Color(0xFF2ECC71) // Success Green
                        "BACKING_UP" -> Color(0xFF3498DB) // Info Blue
                        "DELETED" -> Color(0xFF7F8C8D) // Space Freed Gray
                        else -> Color(0xFFF1C40F) // Pending Yellow
                    }
                )
                .padding(4.dp)
        ) {
            Icon(
                imageVector = when (media.backupStatus) {
                    "COMPLETED" -> Icons.Filled.Check
                    "BACKING_UP" -> Icons.Filled.HourglassBottom
                    "DELETED" -> Icons.Filled.DeleteSweep
                    else -> Icons.Filled.CloudUpload
                },
                contentDescription = media.backupStatus,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
fun LightboxDialog(
    media: MediaFile,
    isWifiConnected: Boolean,
    serverName: String?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large styled mock preview box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                if (media.fileType == "VIDEO") listOf(Color(0xFF2C3E50), Color(0xFF3498DB))
                                else listOf(Color(0xFF8E44AD), Color(0xFFF39C12))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (media.fileType == "VIDEO") Icons.Filled.PlayCircle else Icons.Filled.Image,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(64.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = media.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Details table
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DetailRow("Type", media.fileType)
                    DetailRow("Size", BackupRepository.formatBytes(media.fileSize))
                    DetailRow("Source Device", media.deviceName)
                    DetailRow("Tags / keywords", media.keywords ?: "family, photos, backup")
                    
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    DetailRow("Added local", sdf.format(Date(media.dateAdded)))
                    
                    if (media.backupTimestamp != null) {
                        DetailRow("Backed up to PC", sdf.format(Date(media.backupTimestamp)))
                        DetailRow("Storage PC Path", "D:\\HomeVault\\Photos\\${media.fileName}")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // If on Wifi and backup Completed, show remote streaming tag
                if (isWifiConnected && serverName != null && media.backupStatus != "PENDING") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Wifi, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Streaming live from $serverName over Wi-Fi",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ==========================================
// 3. SECURE BACKUP LOGS VIEW
// ==========================================
@Composable
fun LogsView(
    logs: List<BackupLog>,
    onClearClick: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Audit Logs",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Secure local backup telemetry",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (logs.isNotEmpty()) {
                IconButton(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear backup audit log history", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.ListAlt,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "History is clear. Back up some files to populate logs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(logs) { log ->
                    LogItemCard(log = log)
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Reset Application State?") },
            text = { Text("Are you sure? This will unpair your device, clear your home backup configs, and flush all transfer history databases.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearClick()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LogItemCard(log: BackupLog) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()) }
    val timeLabel = formatter.format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (log.status) {
                            "SUCCESS" -> Color(0xFFD4EFDF)
                            "PENDING" -> Color(0xFFFEF9E7)
                            else -> Color(0xFFFADBD8)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (log.status) {
                        "SUCCESS" -> Icons.Filled.Check
                        "PENDING" -> Icons.Filled.HourglassTop
                        else -> Icons.Filled.Close
                    },
                    contentDescription = null,
                    tint = when (log.status) {
                        "SUCCESS" -> Color(0xFF27AE60)
                        "PENDING" -> Color(0xFFF39C12)
                        else -> Color(0xFFC0392B)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (log.status == "SUCCESS") "Backup Verified" else "Transfer Notice",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
                if (log.totalBytes > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Size: ${BackupRepository.formatBytes(log.totalBytes)}   |   ${log.filesCount} file(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. SETTINGS VIEW COMPOSABLE
// ==========================================
@Composable
fun SettingsView(
    viewModel: BackupViewModel,
    config: SystemConfig
) {
    val context = LocalContext.current
    var showCleanupDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Backup Rules",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Customize connection, charging, and deletion parameters",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 1: Automatic Home Detection
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Automatic Home Detection", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                var localSsid by remember(config.trustedWifiSsid) { mutableStateOf(config.trustedWifiSsid) }
                OutlinedTextField(
                    value = localSsid,
                    onValueChange = {
                        localSsid = it
                        viewModel.updateConfig(config.copy(trustedWifiSsid = it))
                    },
                    label = { Text("Trusted Home Wi-Fi SSID Name") },
                    leadingIcon = { Icon(Icons.Filled.Wifi, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("trusted_wifi_input"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "When your phone links with this specific network ID, background backups execute instantly on WorkManager schedules.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: Battery & Network controls
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Battery & Network Controls", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // Toggle 1: Only Charging
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Only While Charging", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Conserves power by only starting uploads while plugged in.", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = config.backupOnlyWhileCharging,
                        onCheckedChange = { viewModel.updateConfig(config.copy(backupOnlyWhileCharging = it)) },
                        modifier = Modifier.testTag("charging_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Toggle 2: Only Wi-Fi
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Only on Wi-Fi State", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Disables operations if connected to cellular data.", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = config.backupOnlyOnWifi,
                        onCheckedChange = { viewModel.updateConfig(config.copy(backupOnlyOnWifi = it)) },
                        modifier = Modifier.testTag("wifi_only_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Battery slider selector
                Text(
                    text = "Minimum Battery Threshold: ${config.minBatteryThreshold}%",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Halts and pauses background processing if below this percentage.", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = config.minBatteryThreshold.toFloat(),
                    onValueChange = { viewModel.updateConfig(config.copy(minBatteryThreshold = it.toInt())) },
                    valueRange = 10f..80f,
                    steps = 7,
                    modifier = Modifier.testTag("battery_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: Storage Cleanup
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Storage Cleanup", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                // Dropdown trigger button showing policy
                val readablePolicy = when (config.cleanupPolicy) {
                    "IMMEDIATE" -> "Delete immediately"
                    "7_DAYS" -> "Delete after 7 days"
                    "30_DAYS" -> "Delete after 30 days"
                    else -> "Never delete (Default)"
                }

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCleanupDialog = true }
                        .testTag("cleanup_policy_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Local Asset Retention Policy", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(readablePolicy, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Edit Policy")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.runStorageCleanup()
                        Toast.makeText(context, "Scanning and pruning backed up photos according to policy.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End).testTag("trigger_prune_button")
                ) {
                    Icon(Icons.Filled.CleanHands, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clean Storage Now")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 4: Cloud Integrations & Disaster Recovery
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("cloud_integration_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cloud Integration & Services", 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.primary, 
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Import from external vaults or enable second-copy Disaster Recovery replicates.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Import Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var isImportingGoogle by remember { mutableStateOf(false) }
                    var isImportingIcloud by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            isImportingGoogle = true
                            viewModel.importFromCloud("Google Photos") { count ->
                                isImportingGoogle = false
                                Toast.makeText(context, "Successfully imported $count photos from Google Photos!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("google_import_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        enabled = !isImportingGoogle && !isImportingIcloud,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isImportingGoogle) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("GPhotos Import", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            isImportingIcloud = true
                            viewModel.importFromCloud("iCloud") { count ->
                                isImportingIcloud = false
                                Toast.makeText(context, "Successfully imported $count photos from iCloud!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("icloud_import_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        enabled = !isImportingGoogle && !isImportingIcloud,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isImportingIcloud) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("iCloud Import", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Disaster Recovery Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Disaster Recovery Replication", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Simultaneously replication of PC backups to a secure off-site cloud.", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = config.isCloudSyncEnabled,
                        onCheckedChange = { viewModel.updateConfig(config.copy(isCloudSyncEnabled = it)) },
                        modifier = Modifier.testTag("dr_sync_switch")
                    )
                }

                if (config.isCloudSyncEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Select DR Cloud Provider:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    
                    var showProviderDropdown by remember { mutableStateOf(false) }
                    val providers = listOf("Google Cloud Storage", "AWS S3 Sync", "Backblaze B2 Glacier")
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { showProviderDropdown = true },
                            modifier = Modifier.fillMaxWidth().testTag("dr_provider_selector")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(config.cloudProvider, fontWeight = FontWeight.Bold)
                                Icon(Icons.Filled.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(
                            expanded = showProviderDropdown,
                            onDismissRequest = { showProviderDropdown = false }
                        ) {
                            providers.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p) },
                                    onClick = {
                                        viewModel.updateConfig(config.copy(cloudProvider = p))
                                        showProviderDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    var localAccessKey by remember(config.cloudAccessKey) { mutableStateOf(config.cloudAccessKey) }
                    OutlinedTextField(
                        value = localAccessKey,
                        onValueChange = {
                            localAccessKey = it
                            viewModel.updateConfig(config.copy(cloudAccessKey = it))
                        },
                        label = { Text("Cloud API Access Key ID") },
                        leadingIcon = { Icon(Icons.Filled.Key, null) },
                        modifier = Modifier.fillMaxWidth().testTag("cloud_access_key"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    var localSecretKey by remember(config.cloudSecretKey) { mutableStateOf(config.cloudSecretKey) }
                    OutlinedTextField(
                        value = localSecretKey,
                        onValueChange = {
                            localSecretKey = it
                            viewModel.updateConfig(config.copy(cloudSecretKey = it))
                        },
                        label = { Text("Cloud API Secret Access Key") },
                        leadingIcon = { Icon(Icons.Filled.Password, null) },
                        modifier = Modifier.fillMaxWidth().testTag("cloud_secret_key"),
                        singleLine = true
                    )
                }
            }
        }
    }

    if (showCleanupDialog) {
        val policies = listOf(
            "IMMEDIATE" to "Delete immediately",
            "7_DAYS" to "Delete after 7 days",
            "30_DAYS" to "Delete after 30 days",
            "NEVER" to "Never delete"
        )

        AlertDialog(
            onDismissRequest = { showCleanupDialog = false },
            title = { Text("Local Asset Retention Policy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select when to safely remove local photos and videos from device storage once they are confirmed on your home pc:", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    policies.forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateConfig(config.copy(cleanupPolicy = code))
                                    showCleanupDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = config.cleanupPolicy == code,
                                onClick = {
                                    viewModel.updateConfig(config.copy(cleanupPolicy = code))
                                    showCleanupDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCleanupDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// ==========================================
// 5. PC PAIRING DIALOG WITH SIMULATED QR CAMERA
// ==========================================
@Composable
fun PairComputerDialog(
    onDismiss: () -> Unit,
    onPairSuccess: (name: String, ip: String, port: Int) -> Unit
) {
    var step by remember { mutableStateOf(0) } // 0 = Scanner, 1 = Backup Manual Form Setup
    var pcName by remember { mutableStateOf("My-Desktop-PC") }
    var pcIp by remember { mutableStateOf("192.168.1.135") }
    var pcPort by remember { mutableStateOf("4848") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (step == 0) {
                    // QR Scanner simulation
                    Text(
                        text = "Scan PC Pairing Code",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Point camera at the QR code shown inside your HomeVault PC desktop server app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    // Scanner View Simulation Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background Grid Pattern Simulation
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Subtle futuristic HUD corners
                            val strokeWidth = 5f
                            val lineLen = 40f
                            // Top left
                            drawLine(Color.Green, Offset(16.dp.toPx(), 16.dp.toPx()), Offset((16.dp.toPx() + lineLen), 16.dp.toPx()), strokeWidth)
                            drawLine(Color.Green, Offset(16.dp.toPx(), 16.dp.toPx()), Offset(16.dp.toPx(), (16.dp.toPx() + lineLen)), strokeWidth)
                            // Top right
                            drawLine(Color.Green, Offset(size.width - 16.dp.toPx(), 16.dp.toPx()), Offset((size.width - 16.dp.toPx() - lineLen), 16.dp.toPx()), strokeWidth)
                            drawLine(Color.Green, Offset(size.width - 16.dp.toPx(), 16.dp.toPx()), Offset(size.width - 16.dp.toPx(), (16.dp.toPx() + lineLen)), strokeWidth)
                            // Bottom left
                            drawLine(Color.Green, Offset(16.dp.toPx(), size.height - 16.dp.toPx()), Offset((16.dp.toPx() + lineLen), size.height - 16.dp.toPx()), strokeWidth)
                            drawLine(Color.Green, Offset(16.dp.toPx(), size.height - 16.dp.toPx()), Offset(16.dp.toPx(), (size.height - 16.dp.toPx() - lineLen)), strokeWidth)
                            // Bottom right
                            drawLine(Color.Green, Offset(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()), Offset((size.width - 16.dp.toPx() - lineLen), size.height - 16.dp.toPx()), strokeWidth)
                            drawLine(Color.Green, Offset(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()), Offset(size.width - 16.dp.toPx(), (size.height - 16.dp.toPx() - lineLen)), strokeWidth)
                        }

                        // Scanning Laser animating up and down
                        val infiniteTransition = rememberInfiniteTransition(label = "Laser")
                        val laserOffset by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 0.8f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "LaserMotion"
                        )

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .offset(y = maxHeight * laserOffset)
                                    .background(Color.Green)
                            )
                        }

                        Icon(
                            imageVector = Icons.Filled.QrCode,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(90.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            // Mock scan success: jump to manual review form confirmation
                            step = 1
                        },
                        modifier = Modifier.fillMaxWidth().testTag("pair_scan_button")
                    ) {
                        Text("Simulate Quick Scan Success")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = { step = 1 }, modifier = Modifier.testTag("pair_manual_button")) {
                        Text("Add Server Details Manually")
                    }
                } else {
                    // Manual pairing form
                    Text(
                        text = "Confirm Desktop Configuration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pcName,
                        onValueChange = { pcName = it },
                        label = { Text("Computer Name / Alias") },
                        leadingIcon = { Icon(Icons.Filled.Label, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pcIp,
                        onValueChange = { pcIp = it },
                        label = { Text("Local Network IP Address") },
                        leadingIcon = { Icon(Icons.Filled.Dns, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pcPort,
                        onValueChange = { pcPort = it },
                        label = { Text("Sync Service Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Filled.Toll, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { step = 0 },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back to Scanner")
                        }
                        Button(
                            onClick = {
                                val portInt = pcPort.toIntOrNull() ?: 4848
                                onPairSuccess(pcName, pcIp, portInt)
                            },
                            modifier = Modifier.weight(1f).testTag("pair_save_button")
                        ) {
                            Text("Complete Pairing")
                        }
                    }
                }
            }
        }
    }
}
