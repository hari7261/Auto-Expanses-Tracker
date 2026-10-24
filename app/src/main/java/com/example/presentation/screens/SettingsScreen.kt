package com.example.presentation.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasNotificationServicePermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val enabledListeners = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners"
                )
                hasNotificationServicePermission = enabledListeners != null && enabledListeners.contains(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isDetectionEnabled by viewModel.isNotificationDetectionEnabled.collectAsState()
    val activeTheme by viewModel.themePreference.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricLockEnabled.collectAsState()
    val allowedApps by viewModel.allowedApps.collectAsState()

    val endpointState by viewModel.backupServerEndpoint.collectAsState()
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()

    var tempUrl by remember(endpointState) { mutableStateOf(endpointState) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showAddCustomAppDialog by remember { mutableStateOf(false) }
    var customPackageName by remember { mutableStateOf("") }
    var customAppName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // Section: Appearance Theming
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Appearance Theme",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = activeTheme == "Light",
                            onClick = { viewModel.changeThemePreference("Light") },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) {
                            Text("Light")
                        }
                        SegmentedButton(
                            selected = activeTheme == "Dark",
                            onClick = { viewModel.changeThemePreference("Dark") },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) {
                            Text("Dark")
                        }
                        SegmentedButton(
                            selected = activeTheme == "System",
                            onClick = { viewModel.changeThemePreference("System") },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) {
                            Text("System")
                        }
                    }
                }
            }

            // Section: Privacy Controls & Detections
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Core Security & Parsing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Global Parsing Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Interception active", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Auto-detect debit receipts from allowlisted payment apps local on device.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            
                            if (isDetectionEnabled) {
                                Spacer(modifier = Modifier.height(4.dp))
                                if (!hasNotificationServicePermission) {
                                    TextButton(
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                                context.startActivity(intent)
                                            }
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            "⚠️ System Access Required (Tap to open Settings)",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        "🟢 Active (System permission granted)",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Switch(
                            checked = isDetectionEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (hasNotificationServicePermission) {
                                        viewModel.toggleNotificationDetection(true)
                                    } else {
                                        showPermissionDialog = true
                                    }
                                } else {
                                    viewModel.toggleNotificationDetection(false)
                                }
                            },
                            modifier = Modifier.testTag("global_notification_switch")
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Biometric lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Optional Biometric Lock", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Ask for fingerprint or pattern verification when starting application.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { viewModel.toggleBiometricLock(it) },
                            modifier = Modifier.testTag("biometric_lock_switch")
                        )
                    }
                }
            }

            // Section: Managing Allowed Apps
            if (isDetectionEnabled) {
                val builtIns = remember {
                    setOf(
                        "com.phonepe.app", "com.google.android.apps.nbu.paisa.user", "net.one97.paytm",
                        "in.org.npci.upiapp", "com.dreamplug.androidcash", "com.mobikwik_new",
                        "com.freecharge.android", "in.amazon.mShop.android.shopping", "com.snapwork.hdfc",
                        "com.icicibank.imobile", "com.sbi.yonolite", "com.axis.mobile", "com.msf.kbank.mobile",
                        "com.google.android.apps.messaging", "com.samsung.android.messaging",
                        "com.android.mms", "com.oneplus.mms"
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Permitted Payment & Banking Apps",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    customPackageName = ""
                                    customAppName = ""
                                    showAddCustomAppDialog = true
                                },
                                modifier = Modifier.testTag("add_custom_app_btn"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Custom", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            "Only notifications from these checked apps will ever be parsed. All other items are immediately ignored.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (allowedApps.isEmpty()) {
                            Text("Loading package allowlists...", fontSize = 12.sp)
                        } else {
                            allowedApps.forEach { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            app.displayName,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            app.packageName,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!builtIns.contains(app.packageName)) {
                                            IconButton(
                                                onClick = { viewModel.removeAppFromAllowlist(app) },
                                                modifier = Modifier.testTag("delete_app_${app.packageName.replace(".", "_")}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove App",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = app.isEnabled,
                                            onCheckedChange = { isChecked ->
                                                viewModel.toggleAppPermission(app.packageName, isChecked)
                                            },
                                            modifier = Modifier.testTag("app_switch_${app.packageName.replace(".", "_")}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: Cloud Backup & Sync Ledger
            Card(
                modifier = Modifier.fillMaxWidth().testTag("sync_section_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Cloud Backup & Sync Ledger",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        "Synchronize your private expense database, lists, and budgets securely with a remote ledger endpoint.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    // Target Endpoint url input field with save option
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("Backup Server Sync URL") },
                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                        trailingIcon = {
                            if (tempUrl != endpointState) {
                                IconButton(onClick = { viewModel.changeBackupServerEndpoint(tempUrl) }) {
                                    Icon(Icons.Default.Save, contentDescription = "Save settings", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sync_endpoint_input")
                    )

                    // Auto sync toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Backup", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Instantly stream transactions to sync endpoint on any new additions or edits.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Switch(
                            checked = isAutoSyncEnabled,
                            onCheckedChange = { viewModel.toggleAutoSync(it) },
                            modifier = Modifier.testTag("auto_sync_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Sync state indicators
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when (syncState) {
                                    "Success" -> Icons.Default.CheckCircle
                                    "Error" -> Icons.Default.Error
                                    "Syncing..." -> Icons.Default.Sync
                                    else -> Icons.Default.Cloud
                                },
                                contentDescription = null,
                                tint = when (syncState) {
                                    "Success" -> Color(0xFF4CAF50)
                                    "Error" -> Color(0xFFF44336)
                                    "Syncing..." -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            )
                            Column {
                                Text("Sync Engine State", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = when (syncState) {
                                        "Success" -> "Operational (Sync complete)"
                                        "Error" -> "Sync error! Verify URL / Network"
                                        "Syncing..." -> "Synchronizing ledger payload..."
                                        else -> "Ready to synchronize"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        if (syncState == "Syncing...") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Backup and restore manually
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerSyncNow() },
                            enabled = syncState == "Idle" && tempUrl.isNotBlank(),
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup Now", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.triggerRestoreNow() },
                            enabled = syncState == "Idle" && tempUrl.isNotBlank(),
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Back", fontSize = 13.sp)
                        }
                    }

                    // Activity history list nested block
                    Text(
                        "Device Sync Activity Log",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    )

                    if (syncLogs.isEmpty()) {
                        Text(
                            "No synchronization history on this device yet.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            val sdf = remember { java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()) }
                            syncLogs.forEach { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.operation,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = log.details,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = sdf.format(java.util.Date(log.timestamp)),
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    
                                    val (badgeColor, badgeText) = when (log.status) {
                                        "SUCCESS" -> Color(0xFF4CAF50) to "Success"
                                        "FAILURE" -> Color(0xFFF44336) to "Failed"
                                        else -> Color(0xFFFFC107) to "Syncing"
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                )
                            }
                        }
                        
                        TextButton(
                            onClick = { viewModel.clearSyncHistory() },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear sync logs", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Section: Portability & Operations
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Data Administration",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Export CSV
                    Button(
                        onClick = { viewModel.exportToCsv(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("export_csv_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Expenses as CSV file")
                    }

                    // Clear Data
                    Button(
                        onClick = { showClearDataConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_data_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Wipe Database")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wipe local data")
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Modal Clear Data Confirm Dialog
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Dangerous Wipe Operation") },
            text = { Text("This will permanently delete all manually logged expenses, custom categories budgets, and cleared suggestion histories completely from your device. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_wipe_data_btn")
                ) {
                    Text("Permanently Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("wipe_confirm_dialog")
        )
    }

    // Modal Permission Request Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("System Permission Required") },
            text = { Text("To automatically read and import payment transaction receipts from your banking apps, Android requires you to manually grant 'Notification Access' to Private Expenses in your system settings.\n\nWould you like to open settings now?") },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        viewModel.toggleNotificationDetection(true)
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.testTag("grant_notification_permission_btn")
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("notification_permission_dialog")
        )
    }

    // Modal Add Custom App Dialog
    if (showAddCustomAppDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomAppDialog = false },
            title = { Text("Add Custom Payment App", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Input details for any installed app so its notifications can be parsed for receipts.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customAppName,
                        onValueChange = { customAppName = it },
                        label = { Text("Display App Name") },
                        placeholder = { Text("e.g. Jupiter, WhatsApp") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_app_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customPackageName,
                        onValueChange = { customPackageName = it },
                        label = { Text("Package Name") },
                        placeholder = { Text("e.g. com.jupiter.app") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_package_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customAppName.isNotBlank() && customPackageName.isNotBlank()) {
                            viewModel.addCustomApp(customPackageName.trim(), customAppName.trim())
                            showAddCustomAppDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_custom_app_btn")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomAppDialog = false }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("add_custom_app_dialog")
        )
    }
}
