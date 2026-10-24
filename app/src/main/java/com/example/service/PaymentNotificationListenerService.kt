package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.PrivateExpensesApplication
import com.example.data.local.entities.NotificationSuggestion
import com.example.data.parser.PaymentNotificationParser
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PaymentNotificationListenerService : NotificationListenerService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    companion object {
        private const val TAG = "PaymentNotificationSvc"
        @Volatile
        var instance: PaymentNotificationListenerService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Notification Service Created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.d(TAG, "Notification Service Connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
        Log.d(TAG, "Notification Service Disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        val packageName = sbn.packageName

        serviceScope.launch {
            try {
                val app = application as PrivateExpensesApplication
                val prefRepo = app.userPreferencesRepository
                val expenseRepo = app.expenseRepository

                // 1. Check if global parsing is enabled
                val isGlobalEnabled = prefRepo.isNotificationDetectionEnabled.first()
                if (!isGlobalEnabled) {
                    Log.d(TAG, "Notification parsing is globally disabled. Ignoring.")
                    return@launch
                }

                // 2. Fetch allowed apps and check if source app is allowlisted & enabled
                val allowedApps = expenseRepo.getActiveAllowlistedApps()
                val targetApp = allowedApps.find { it.packageName == packageName && it.isEnabled }
                if (targetApp == null) {
                    Log.d(TAG, "Ignoring notification from non-allowlisted package: $packageName")
                    return@launch
                }

                // 3. Extract all possible text elements safely
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() 
                    ?: extras.getCharSequence("android.title")?.toString() 
                    ?: ""
                
                val textParts = mutableListOf<String>()
                
                // Add standard text
                extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.let { textParts.add(it) }
                extras.getCharSequence("android.text")?.toString()?.let { textParts.add(it) }
                
                // Add big text if any
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.let { textParts.add(it) }
                
                // Add subtext if any
                extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.let { textParts.add(it) }
                
                // Add info text if any
                extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()?.let { textParts.add(it) }
                
                // Add ticker text if any
                sbn.notification.tickerText?.toString()?.let { textParts.add(it) }

                // Combine them uniquely, keeping original order but filtering empty/duplicates
                val combinedText = textParts.distinct().filter { it.isNotBlank() }.joinToString(" ")
                
                Log.d(TAG, "Processing notification from ${targetApp.displayName}. Extracted Title: $title, Combined Text: $combinedText")

                // 4. Parse transaction details
                val parsed = PaymentNotificationParser.parseNotification(title, combinedText)
                if (parsed != null) {
                    Log.d(TAG, "Parsed Transaction successfully: Amt = ${parsed.amount}, Merchant = ${parsed.merchant}, Conf = ${parsed.confidence}")
                    
                    // 5. Create suggestion
                    val suggestion = NotificationSuggestion(
                        amount = parsed.amount,
                        merchant = parsed.merchant,
                        paymentMethod = parsed.paymentMethod,
                        sourceApp = targetApp.displayName,
                        accountLast4 = parsed.accountLast4,
                        isCredit = parsed.isCredit,
                        timestamp = sbn.postTime,
                        confidence = parsed.confidence,
                        rawText = combinedText,
                        status = "PENDING"
                    )

                    // 6. Insert suggestion (will internally perform duplicate detection)
                    val inserted = expenseRepo.insertSuggestion(suggestion)
                    if (inserted) {
                        NotificationHelper.showTransactionDraftNotification(
                            context = this@PaymentNotificationListenerService,
                            amount = suggestion.amount,
                            merchant = suggestion.merchant,
                            isCredit = suggestion.isCredit,
                            appSource = suggestion.sourceApp
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        serviceJob.cancel()
    }
}
