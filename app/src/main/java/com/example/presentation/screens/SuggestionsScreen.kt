package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.Expense
import com.example.data.local.entities.NotificationSuggestion
import com.example.presentation.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(
    viewModel: ExpenseViewModel,
    onEditAndSaveClick: (NotificationSuggestion) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val greenColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)

    val suggestions by viewModel.pendingSuggestions.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    var showSimulatorDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var isCheckingStatus by remember { mutableStateOf(false) }
    var showPermissionErrorDialog by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasSmsPermission by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val smsPermissionsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val receiveGranted = permissions[android.Manifest.permission.RECEIVE_SMS] as? Boolean ?: false
        val readGranted = permissions[android.Manifest.permission.READ_SMS] as? Boolean ?: false
        hasSmsPermission = receiveGranted || readGranted
        if (receiveGranted || readGranted) {
            android.widget.Toast.makeText(context, "Direct Bank SMS Monitoring Active!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val enabledListeners = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners"
                )
                hasNotificationPermission = enabledListeners != null && enabledListeners.contains(context.packageName)

                hasSmsPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECEIVE_SMS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val triggerStatusSync = {
        if (!hasNotificationPermission) {
            showPermissionErrorDialog = true
        } else {
            isCheckingStatus = true
            viewModel.syncNotificationsFromStatusBar(context) { success, count ->
                isCheckingStatus = false
                if (!success) {
                    android.widget.Toast.makeText(context, "Scanning failed. Ensure payment SMS apps are enabled in settings allowlist.", android.widget.Toast.LENGTH_LONG).show()
                } else if (count > 0) {
                    android.widget.Toast.makeText(context, "Successfully detected $count new payment alert(s)!", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Scan complete. No new payment alerts found in notification shade.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Alert Suggestions", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { showSimulatorDialog = true },
                        modifier = Modifier.testTag("simulate_alert_icon_btn")
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = "Simulate Alerts Center",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (suggestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!hasNotificationPermission) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .testTag("notification_permission_warning_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Notification Auto-Detection Disabled",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Grant 'Notification Access' in settings to let Private Expenses automatically extract payment info.",
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
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
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (!hasSmsPermission) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .testTag("sms_permission_warning_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = "SMS",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Read Direct Bank SMS Texts",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Automatically capture transactions directly from incoming bank text messages (recommended).",
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        smsPermissionsLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.RECEIVE_SMS,
                                                android.Manifest.permission.READ_SMS
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Permit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Inbox Empty",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        "All Clear!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "No pending suggestions to review. New alerts from enabled banking apps will appear here as you receive notification transactions.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { triggerStatusSync() },
                        modifier = Modifier.testTag("scan_notifications_empty_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isCheckingStatus) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning Notification Shade...")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Phone Notifications")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showSimulatorDialog = true },
                        modifier = Modifier.testTag("simulate_from_empty_btn")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = "Simulate")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate Demo Alerts")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!hasNotificationPermission) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notification_permission_list_warning_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Notification Auto-Detection Disabled",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Grant 'Notification Access' in settings to let Private Expenses automatically extract payment info.",
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
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
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (!hasSmsPermission) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sms_permission_list_warning_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = "SMS",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Read Direct Bank SMS Texts",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Automatically capture transactions directly from incoming bank text messages (recommended).",
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        smsPermissionsLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.RECEIVE_SMS,
                                                android.Manifest.permission.READ_SMS
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Permit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    "Scan Notification Shade",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Directly fetch any payment or transaction SMS receipts visible in your status bar right now.",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            
                            Button(
                                onClick = { triggerStatusSync() },
                                modifier = Modifier.testTag("scan_notifications_header_btn"),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                if (isCheckingStatus) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Scan", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "These expenses were automatically detected by the offline rule engine. Confirm and save or discard. None are saved unless approved by you.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(suggestions, key = { it.id }) { suggestion ->
                    // Duplicate warning checks: is there an expense with identical amount and same day?
                    val hasPotentialDuplicate = expenses.any { exp ->
                        exp.amount == suggestion.amount &&
                        Math.abs(exp.date - suggestion.timestamp) < 24 * 3600 * 1000L // same day
                    }

                    SuggestionCardItem(
                        suggestion = suggestion,
                        hasDuplicateWarning = hasPotentialDuplicate,
                        currencyFormatter = currencyFormatter,
                        onApprove = { selectedCategory ->
                            val expense = Expense(
                                amount = suggestion.amount,
                                category = selectedCategory,
                                merchant = suggestion.merchant,
                                paymentMethod = suggestion.paymentMethod,
                                date = suggestion.timestamp,
                                notes = "Approved from payment alert: ${suggestion.sourceApp}",
                                isCredit = suggestion.isCredit,
                                accountLast4 = suggestion.accountLast4
                            )
                            viewModel.approveSuggestion(suggestion.id, expense)
                        },
                        onEdit = {
                            onEditAndSaveClick(suggestion)
                        },
                        onIgnore = {
                            viewModel.ignoreSuggestion(suggestion.id)
                        }
                    )
                }
            }
        }
    }

    if (showSimulatorDialog) {
        AlertDialog(
            onDismissRequest = { showSimulatorDialog = false },
            title = {
                Text(
                    "Notification Parser Simulator",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Select a realistic transaction notice pattern below to simulate an interception over device notification streams:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val templates = listOf(
                        Triple("Google Pay", "Paid ₹180 to Chai Point ending in 5543.", "UPI Debit"),
                        Triple("PhonePe", "Debited Rs 465 at Swiggy from A/c ending 2190.", "UPI Debit"),
                        Triple("HDFC Bank", "Your HDFC Card xx9012 is charged for INR 2,450.00 at Amazon India.", "Card Spend"),
                        Triple("SBI YONO Lite", "Dear Customer, your ac ending 8812 was credited Rs.25,000 for Salary.", "Salary Credit"),
                        Triple("Paytm", "Rs. 150.00 received from Uber India Refund.", "Refund Credit"),
                        Triple("Axis Mobile", "Amt Spent Rs.890 at Grocer Mart on Axis Bank card xx5412.", "Card Spend")
                    )

                    templates.forEach { (app, text, type) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.simulateNotification(app, text)
                                    showSimulatorDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        app,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        type,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (type.contains("Credit")) greenColor else MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"$text\"",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSimulatorDialog = false }) {
                    Text("Close")
                }
            },
            modifier = Modifier.testTag("simulator_options_dialog")
        )
    }

    if (showPermissionErrorDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionErrorDialog = false },
            title = { Text("Notification Access Required", fontWeight = FontWeight.Bold) },
            text = { Text("To scan your status bar and automatically detect transaction notifications from your UPI and SMS apps, please manually enable 'Notification Access' for Private Expenses in your system settings.") },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionErrorDialog = false
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
                    modifier = Modifier.testTag("open_settings_from_suggestions_btn")
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionErrorDialog = false }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("suggestions_permission_error_dialog")
        )
    }
}

@Composable
fun SuggestionCardItem(
    suggestion: NotificationSuggestion,
    hasDuplicateWarning: Boolean,
    currencyFormatter: NumberFormat,
    onApprove: (String) -> Unit,
    onEdit: () -> Unit,
    onIgnore: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val greenColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val categoriesList = listOf("Food", "Shopping", "Bills", "Travel", "Groceries", "Other")
    var selectedCategoryForQuickSave by remember { mutableStateOf("Food") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("suggestion_card_${suggestion.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = if (hasDuplicateWarning) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Duplicate Warning Callout
            if (hasDuplicateWarning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.PriorityHigh,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Potential Duplicate! An expense with this amount is already logged today.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Top Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Sms,
                            contentDescription = "Alert Recipient",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            suggestion.sourceApp,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            sdf.format(Date(suggestion.timestamp)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                // Confidence badge
                val confidencePct = (suggestion.confidence * 100).toInt()
                Badge(
                    containerColor = when {
                        suggestion.confidence >= 0.8f -> Color(0xFFE8F5E9)
                        suggestion.confidence >= 0.6f -> Color(0xFFFFF9C4)
                        else -> Color(0xFFFFEBEE)
                    },
                    contentColor = when {
                        suggestion.confidence >= 0.8f -> Color(0xFF2E7D32)
                        suggestion.confidence >= 0.6f -> Color(0xFFF57F17)
                        else -> Color(0xFFC62828)
                    }
                ) {
                    Text("$confidencePct% confidence", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Transaction Info Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        suggestion.merchant,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            suggestion.paymentMethod,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (suggestion.accountLast4 != null) {
                            Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Text("A/c ending ${suggestion.accountLast4}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }
                Text(
                    text = "${if (suggestion.isCredit) "+" else "-"}${currencyFormatter.format(suggestion.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = if (suggestion.isCredit) greenColor else MaterialTheme.colorScheme.error
                )
            }

            // Raw notification test block expands
            var showRawText by remember { mutableStateOf(false) }
            Column {
                TextButton(
                    onClick = { showRawText = !showRawText },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (showRawText) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Text",
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Show raw log text for audit", fontSize = 11.sp)
                    }
                }
                if (showRawText) {
                    Text(
                        text = suggestion.rawText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 14.sp,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .fillMaxWidth()
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Quick category select rows
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Select Category for Expense entry:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoriesList.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryForQuickSave == cat,
                            onClick = { selectedCategoryForQuickSave = cat },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("suggest_quick_cat_$cat")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Triggers Bottom Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onIgnore,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dismiss_suggestion_btn_${suggestion.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Ignore", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ignore")
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_suggestion_btn_${suggestion.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                Button(
                    onClick = { onApprove(selectedCategoryForQuickSave) },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("approve_suggestion_btn_${suggestion.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Approve", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Approve")
                }
            }
        }
    }
}
