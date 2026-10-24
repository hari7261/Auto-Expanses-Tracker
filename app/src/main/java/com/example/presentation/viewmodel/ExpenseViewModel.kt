package com.example.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PrivateExpensesApplication
import com.example.data.local.entities.AppAllowlist
import com.example.data.local.entities.Budget
import com.example.data.local.entities.Expense
import com.example.data.local.entities.NotificationSuggestion
import com.example.data.local.entities.SyncLog
import com.example.data.remote.BackupServiceFactory
import com.example.data.remote.SyncBackupPayload
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PrivateExpensesApplication
    private val expenseRepository = app.expenseRepository
    private val prefRepo = app.userPreferencesRepository

    init {
        viewModelScope.launch {
            try {
                val allowlistDao = app.database.appAllowlistDao()
                val currentApps = allowlistDao.getAllAllowedApps().first()
                val defaultPackages = listOf(
                    AppAllowlist("com.phonepe.app", "PhonePe", true),
                    AppAllowlist("com.google.android.apps.nbu.paisa.user", "Google Pay", true),
                    AppAllowlist("net.one97.paytm", "Paytm", true),
                    AppAllowlist("in.org.npci.upiapp", "BHIM UPI", true),
                    AppAllowlist("com.dreamplug.androidcash", "CRED", true),
                    AppAllowlist("com.mobikwik_new", "MobiKwik", true),
                    AppAllowlist("com.freecharge.android", "Freecharge", true),
                    AppAllowlist("in.amazon.mShop.android.shopping", "Amazon Pay", true),
                    AppAllowlist("com.snapwork.hdfc", "HDFC Bank", true),
                    AppAllowlist("com.icicibank.imobile", "ICICI Bank iMobile", true),
                    AppAllowlist("com.sbi.yonolite", "SBI YONO Lite", true),
                    AppAllowlist("com.axis.mobile", "Axis Mobile", true),
                    AppAllowlist("com.msf.kbank.mobile", "Kotak Mobile Banking", true),
                    AppAllowlist("com.google.android.apps.messaging", "Google Messages (SMS)", true),
                    AppAllowlist("com.samsung.android.messaging", "Samsung Messages (SMS)", true),
                    AppAllowlist("com.android.mms", "Default Messages (SMS)", true),
                    AppAllowlist("com.oneplus.mms", "OnePlus Messages (SMS)", true)
                )
                for (defaultApp in defaultPackages) {
                    if (currentApps.none { it.packageName == defaultApp.packageName }) {
                        allowlistDao.insertApp(defaultApp)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Synchronization UI State
    private val _syncState = MutableStateFlow("Idle") // "Idle", "Syncing...", "Success", "Error"
    val syncState = _syncState.asStateFlow()

    // Sync preferences flows
    val backupServerEndpoint = prefRepo.backupServerEndpoint
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://api.privateexpenses.secure/")

    val isAutoSyncEnabled = prefRepo.isAutoSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val syncLogs = expenseRepository.recentSyncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings data
    val isNotificationDetectionEnabled = prefRepo.isNotificationDetectionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themePreference = prefRepo.themePreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")

    val isBiometricLockEnabled = prefRepo.isBiometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val globalBudget = prefRepo.globalBudget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20000.0)

    // DB data
    val expenses = expenseRepository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets = expenseRepository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSuggestions = expenseRepository.pendingSuggestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allowedApps = expenseRepository.allAllowedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions filtering state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    private val _selectedPaymentFilter = MutableStateFlow<String?>(null)
    val selectedPaymentFilter = _selectedPaymentFilter.asStateFlow()

    // Date range filter: default to null
    private val _startDateFilter = MutableStateFlow<Long?>(null)
    val startDateFilter = _startDateFilter.asStateFlow()

    private val _endDateFilter = MutableStateFlow<Long?>(null)
    val endDateFilter = _endDateFilter.asStateFlow()

    // Combine filters dynamically in UI Flow
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        combine(expenses, searchQuery, selectedCategoryFilter) { exp, q, cat -> Triple(exp, q, cat) },
        combine(selectedPaymentFilter, startDateFilter, endDateFilter) { pay, start, end -> Triple(pay, start, end) }
    ) { left, right ->
        val (rawExpenses, query, category) = left
        val (payment, start, end) = right
        rawExpenses.filter { expense ->
            val matchesQuery = query.isEmpty() || 
                expense.merchant.contains(query, ignoreCase = true) || 
                expense.notes.contains(query, ignoreCase = true)
            
            val matchesCategory = category == null || expense.category == category
            val matchesPayment = payment == null || expense.paymentMethod == payment
            val matchesStart = start == null || expense.date >= start
            val matchesEnd = end == null || expense.date <= end

            matchesQuery && matchesCategory && matchesPayment && matchesStart && matchesEnd
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI actions setter
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCategoryFilter(category: String?) { _selectedCategoryFilter.value = category }
    fun setPaymentFilter(payment: String?) { _selectedPaymentFilter.value = payment }
    fun setDateRangeFilter(start: Long?, end: Long?) {
        _startDateFilter.value = start
        _endDateFilter.value = end
    }
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategoryFilter.value = null
        _selectedPaymentFilter.value = null
        _startDateFilter.value = null
        _endDateFilter.value = null
    }

    // Expenses manipulate actions
    fun addExpense(expense: Expense) = viewModelScope.launch {
        expenseRepository.insertExpense(expense)
        autoSyncIfEnabled()
    }

    fun updateExpense(expense: Expense) = viewModelScope.launch {
        expenseRepository.updateExpense(expense)
        autoSyncIfEnabled()
    }

    fun deleteExpense(expense: Expense) = viewModelScope.launch {
        expenseRepository.deleteExpense(expense)
        autoSyncIfEnabled()
    }

    // Sync actions and auto-sync executor
    private fun autoSyncIfEnabled() {
        if (isAutoSyncEnabled.value) {
            viewModelScope.launch {
                try {
                    val endpoint = backupServerEndpoint.value
                    val response = BackupServiceFactory.create(endpoint).backupData(
                        authToken = "Bearer SECURE_TOKEN",
                        payload = SyncBackupPayload(expenses.value, budgets.value)
                    )
                    expenseRepository.insertSyncLog(
                        SyncLog(
                            status = "SUCCESS",
                            operation = "Backup (Auto)",
                            details = "Synced on change: ${response.message}"
                        )
                    )
                } catch (e: Exception) {
                    expenseRepository.insertSyncLog(
                        SyncLog(
                            status = "FAILURE",
                            operation = "Backup (Auto)",
                            details = "Auto-sync failed: ${e.localizedMessage ?: "Connection error"}"
                        )
                    )
                }
            }
        }
    }

    fun changeBackupServerEndpoint(url: String) = viewModelScope.launch {
        prefRepo.setBackupServerEndpoint(url)
    }

    fun toggleAutoSync(enabled: Boolean) = viewModelScope.launch {
        prefRepo.setAutoSyncEnabled(enabled)
    }

    fun triggerSyncNow() = viewModelScope.launch {
        _syncState.value = "Syncing..."
        val endpoint = backupServerEndpoint.value
        
        // Log in progress state
        val startLogId = expenseRepository.insertSyncLog(
            SyncLog(
                status = "IN_PROGRESS",
                operation = "Backup Data",
                details = "Contacting sync server..."
            )
        )

        try {
            val response = BackupServiceFactory.create(endpoint).backupData(
                authToken = "Bearer SECURE_TOKEN",
                payload = SyncBackupPayload(expenses.value, budgets.value)
            )
            
            // Add success log
            expenseRepository.insertSyncLog(
                SyncLog(
                    status = "SUCCESS",
                    operation = "Backup Data",
                    details = "Manual backup completed successfully. Server message: ${response.message}"
                )
            )
            _syncState.value = "Success"
        } catch (e: Exception) {
            // Add failure log
            expenseRepository.insertSyncLog(
                SyncLog(
                    status = "FAILURE",
                    operation = "Backup Data",
                    details = "Backup failed: ${e.localizedMessage ?: "Failed to connect to sync endpoint"}"
                )
            )
            _syncState.value = "Error"
        } finally {
            delay(3000)
            _syncState.value = "Idle"
        }
    }

    fun triggerRestoreNow() = viewModelScope.launch {
        _syncState.value = "Syncing..."
        val endpoint = backupServerEndpoint.value
        
        expenseRepository.insertSyncLog(
            SyncLog(
                status = "IN_PROGRESS",
                operation = "Restore Data",
                details = "Fetching backup snapshot from ledger endpoint..."
            )
        )

        try {
            val payload = BackupServiceFactory.create(endpoint).restoreData(
                authToken = "Bearer SECURE_TOKEN"
            )
            
            // CLEAR and update local database table records
            app.database.clearAllTables()
            
            // Re-populate system configuration allowlist apps first to preserve functionality
            val allowlistDao = app.database.appAllowlistDao()
            val initialApps = listOf(
                AppAllowlist("com.phonepe.app", "PhonePe", true),
                AppAllowlist("com.google.android.apps.nbu.paisa.user", "Google Pay", true),
                AppAllowlist("net.one97.paytm", "Paytm", true),
                AppAllowlist("in.org.npci.upiapp", "BHIM UPI", true),
                AppAllowlist("com.dreamplug.androidcash", "CRED", true),
                AppAllowlist("com.mobikwik_new", "MobiKwik", true),
                AppAllowlist("com.freecharge.android", "Freecharge", true),
                AppAllowlist("in.amazon.mShop.android.shopping", "Amazon Pay", true),
                AppAllowlist("com.snapwork.hdfc", "HDFC Bank", true),
                AppAllowlist("com.icicibank.imobile", "ICICI Bank iMobile", true),
                AppAllowlist("com.sbi.yonolite", "SBI YONO Lite", true),
                AppAllowlist("com.axis.mobile", "Axis Mobile", true),
                AppAllowlist("com.msf.kbank.mobile", "Kotak Mobile Banking", true),
                AppAllowlist("com.google.android.apps.messaging", "Google Messages (SMS)", true),
                AppAllowlist("com.samsung.android.messaging", "Samsung Messages (SMS)", true),
                AppAllowlist("com.android.mms", "Default Messages (SMS)", true),
                AppAllowlist("com.oneplus.mms", "OnePlus Messages (SMS)", true)
            )
            for (app in initialApps) {
                allowlistDao.insertApp(app)
            }

            // Restore elements
            for (expense in payload.expenses) {
                expenseRepository.insertExpense(expense.copy(id = 0)) // re-insert as new keys to avoid conflict or reuse
            }
            for (budget in payload.budgets) {
                expenseRepository.insertBudget(budget.copy(id = 0))
            }

            expenseRepository.insertSyncLog(
                SyncLog(
                    status = "SUCCESS",
                    operation = "Restore Data",
                    details = "Restored ${payload.expenses.size} expenses and ${payload.budgets.size} budgets from ledger server."
                )
            )
            _syncState.value = "Success"
        } catch (e: Exception) {
            expenseRepository.insertSyncLog(
                SyncLog(
                    status = "FAILURE",
                    operation = "Restore Data",
                    details = "Restore crashed: ${e.localizedMessage ?: "Empty backup payload or bad sync payload structure."}"
                )
            )
            _syncState.value = "Error"
        } finally {
            delay(3000)
            _syncState.value = "Idle"
        }
    }

    fun clearSyncHistory() = viewModelScope.launch {
        expenseRepository.clearSyncLogs()
    }

    // Resolving Suggestions
    fun approveSuggestion(suggestionId: Int, finalExpense: Expense) = viewModelScope.launch {
        expenseRepository.resolveSuggestion(suggestionId, saveAsExpense = true, finalExpense = finalExpense)
    }

    fun ignoreSuggestion(suggestionId: Int) = viewModelScope.launch {
        expenseRepository.resolveSuggestion(suggestionId, saveAsExpense = false)
    }

    fun simulateNotification(appName: String, text: String) = viewModelScope.launch {
        try {
            val parsedResult = com.example.data.parser.PaymentNotificationParser.parseNotification("", text)
            if (parsedResult != null) {
                val suggestion = NotificationSuggestion(
                    amount = parsedResult.amount,
                    merchant = parsedResult.merchant,
                    paymentMethod = parsedResult.paymentMethod,
                    sourceApp = appName,
                    accountLast4 = parsedResult.accountLast4,
                    isCredit = parsedResult.isCredit,
                    timestamp = System.currentTimeMillis(),
                    confidence = parsedResult.confidence,
                    rawText = text,
                    status = "PENDING"
                )
                expenseRepository.insertSuggestion(suggestion)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Budget modifiers
    fun addOrUpdateBudget(budget: Budget) = viewModelScope.launch {
        val existing = budgets.value.find { it.category == budget.category }
        if (existing != null) {
            expenseRepository.updateBudget(budget.copy(id = existing.id))
        } else {
            expenseRepository.insertBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) = viewModelScope.launch {
        expenseRepository.deleteBudget(budget)
    }

    // System Settings Actions
    fun toggleNotificationDetection(enabled: Boolean) = viewModelScope.launch {
        prefRepo.setNotificationDetectionEnabled(enabled)
    }

    fun syncNotificationsFromStatusBar(context: Context, onResult: (Boolean, Int) -> Unit) = viewModelScope.launch {
        val serviceInstance = com.example.service.PaymentNotificationListenerService.instance
        if (serviceInstance == null) {
            onResult(false, 0)
            return@launch
        }

        var parsedCount = 0
        try {
            val activeNotifications = serviceInstance.getActiveNotifications()
            if (activeNotifications != null) {
                // Fetch enabled allowlisted apps
                val allowedApps = expenseRepository.getActiveAllowlistedApps()
                for (sbn in activeNotifications) {
                    val packageName = sbn.packageName
                    
                    // Is this source package in the allowed and enabled list?
                    val targetApp = allowedApps.find { it.packageName == packageName && it.isEnabled }
                    if (targetApp != null) {
                        val extras = sbn.notification.extras ?: continue
                        val title = extras.getString(android.app.Notification.EXTRA_TITLE)
                        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
                        
                        val parsed = com.example.data.parser.PaymentNotificationParser.parseNotification(title, text)
                        if (parsed != null) {
                            // Strictly avoid duplicates:
                            // Check 1: Check if same raw text already exists in notification suggestions
                            val duplicateByText = app.database.notificationSuggestionDao()
                                .findSuggestionByRawText(text ?: "")
                            if (duplicateByText != null) {
                                continue
                            }

                            // Check 2: Check standard duplicate (same amount, merchant, within 15 minutes)
                            val duplicateByDetails = app.database.notificationSuggestionDao()
                                .findDuplicate(
                                    amount = parsed.amount,
                                    timestamp = sbn.postTime,
                                    merchant = parsed.merchant,
                                    timeWindowMs = 15 * 60 * 1000L
                                )
                            if (duplicateByDetails != null) {
                                continue
                            }

                            // Check 3: Check if an actual Expense exists with same amount and merchant on same day
                            val expensesList = expenses.value
                            val isAlreadySaved = expensesList.any { exp ->
                                exp.amount == parsed.amount &&
                                exp.merchant.equals(parsed.merchant, ignoreCase = true) &&
                                Math.abs(exp.date - sbn.postTime) < 24 * 3600 * 1000L
                            }
                            if (isAlreadySaved) {
                                continue
                            }

                            // 4. Create and insert suggestion
                            val suggestion = NotificationSuggestion(
                                amount = parsed.amount,
                                merchant = parsed.merchant,
                                paymentMethod = parsed.paymentMethod,
                                sourceApp = targetApp.displayName,
                                accountLast4 = parsed.accountLast4,
                                isCredit = parsed.isCredit,
                                timestamp = sbn.postTime,
                                confidence = parsed.confidence,
                                rawText = text ?: "",
                                status = "PENDING"
                            )
                            expenseRepository.insertSuggestion(suggestion)
                            parsedCount++
                        }
                    }
                }
            }
            onResult(true, parsedCount)
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(true, 0)
        }
    }

    fun toggleAppPermission(packageName: String, enabled: Boolean) = viewModelScope.launch {
        expenseRepository.updateAppStatus(packageName, enabled)
    }

    fun addCustomApp(packageName: String, displayName: String) = viewModelScope.launch {
        expenseRepository.insertAppToAllowlist(AppAllowlist(packageName, displayName, true))
    }

    fun removeAppFromAllowlist(app: AppAllowlist) = viewModelScope.launch {
        expenseRepository.deleteAppFromAllowlist(app)
    }

    fun changeThemePreference(theme: String) = viewModelScope.launch {
        prefRepo.setThemePreference(theme)
    }

    fun toggleBiometricLock(enabled: Boolean) = viewModelScope.launch {
        prefRepo.setBiometricLockEnabled(enabled)
    }

    fun updateGlobalBudget(amount: Double) = viewModelScope.launch {
        prefRepo.setGlobalBudget(amount)
        addOrUpdateBudget(Budget(category = "All", monthlyLimit = amount))
    }

    fun clearAllData() = viewModelScope.launch {
        app.database.clearAllTables()
        prefRepo.setGlobalBudget(20000.0)
        // Re-populate apps list
        val allowlistDao = app.database.appAllowlistDao()
        val initialApps = listOf(
            AppAllowlist("com.phonepe.app", "PhonePe", true),
            AppAllowlist("com.google.android.apps.nbu.paisa.user", "Google Pay", true),
            AppAllowlist("net.one97.paytm", "Paytm", true),
            AppAllowlist("in.org.npci.upiapp", "BHIM UPI", true),
            AppAllowlist("com.dreamplug.androidcash", "CRED", true),
            AppAllowlist("com.mobikwik_new", "MobiKwik", true),
            AppAllowlist("com.freecharge.android", "Freecharge", true),
            AppAllowlist("in.amazon.mShop.android.shopping", "Amazon Pay", true),
            AppAllowlist("com.snapwork.hdfc", "HDFC Bank", true),
            AppAllowlist("com.icicibank.imobile", "ICICI Bank iMobile", true),
            AppAllowlist("com.sbi.yonolite", "SBI YONO Lite", true),
            AppAllowlist("com.axis.mobile", "Axis Mobile", true),
            AppAllowlist("com.msf.kbank.mobile", "Kotak Mobile Banking", true),
            AppAllowlist("com.google.android.apps.messaging", "Google Messages (SMS)", true),
            AppAllowlist("com.samsung.android.messaging", "Samsung Messages (SMS)", true),
            AppAllowlist("com.android.mms", "Default Messages (SMS)", true),
            AppAllowlist("com.oneplus.mms", "OnePlus Messages (SMS)", true)
        )
        for (app in initialApps) {
            allowlistDao.insertApp(app)
        }
    }

    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            try {
                val expenseList = expenses.value
                val csvHeader = "ID,Amount,Category,Merchant,PaymentMethod,Date,Notes,Type\n"
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val csvBody = expenseList.joinToString("\n") { expense ->
                    val type = if (expense.isCredit) "CREDIT" else "DEBIT"
                    val formattedDate = sdf.format(Date(expense.date))
                    // Escaping commas in notes / merchant
                    val escapedMerchant = expense.merchant.replace("\"", "\"\"")
                    val escapedNotes = expense.notes.replace("\"", "\"\"")
                    "${expense.id},${expense.amount},${expense.category},\"$escapedMerchant\",${expense.paymentMethod},$formattedDate,\"$escapedNotes\",$type"
                }
                val fullCsv = csvHeader + csvBody

                // Create cache file
                val file = File(context.cacheDir, "private_expenses_export.csv")
                file.writeText(fullCsv)

                // Share intent
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Private Expenses Export")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(Intent.createChooser(intent, "Export Expenses CSV"))
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
