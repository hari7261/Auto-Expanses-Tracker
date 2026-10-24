package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
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

class SmsReceiver : BroadcastReceiver() {
    private val receiverJob = SupervisorJob()
    private val receiverScope = CoroutineScope(receiverJob + Dispatchers.IO)

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val pendingResult = goAsync()
            val bundle = intent.extras
            if (bundle == null) {
                pendingResult.finish()
                return
            }
            try {
                val pdus = bundle.get("pdus") as Array<*>?
                if (pdus == null) {
                    pendingResult.finish()
                    return
                }
                val format = bundle.getString("format")
                val applicationContext = context.applicationContext as PrivateExpensesApplication
                val expenseRepo = applicationContext.expenseRepository
                val prefRepo = applicationContext.userPreferencesRepository

                receiverScope.launch {
                    try {
                        val isGlobalEnabled = prefRepo.isNotificationDetectionEnabled.first()
                        if (!isGlobalEnabled) {
                            Log.d(TAG, "Detection is disabled, skipping SMS parsing.")
                            return@launch
                        }

                        for (pdu in pdus) {
                            val sms = SmsMessage.createFromPdu(pdu as ByteArray, format)
                            val messageBody = sms.messageBody ?: continue
                            val sender = sms.originatingAddress ?: "Unknown Sender"
                            val timestamp = sms.timestampMillis

                            Log.d(TAG, "Received SMS from $sender: $messageBody")

                            // Parse the SMS text
                            val parsed = PaymentNotificationParser.parseNotification(sender, messageBody)
                            if (parsed != null) {
                                Log.d(TAG, "Successfully parsed transaction from SMS! Amount: ${parsed.amount}, Merchant: ${parsed.merchant}")
                                
                                val suggestion = NotificationSuggestion(
                                    amount = parsed.amount,
                                    merchant = parsed.merchant,
                                    paymentMethod = parsed.paymentMethod,
                                    sourceApp = "SMS: $sender",
                                    accountLast4 = parsed.accountLast4,
                                    isCredit = parsed.isCredit,
                                    timestamp = timestamp,
                                    confidence = parsed.confidence,
                                    rawText = messageBody,
                                    status = "PENDING"
                                )
                                val inserted = expenseRepo.insertSuggestion(suggestion)
                                if (inserted) {
                                    NotificationHelper.showTransactionDraftNotification(
                                        context = context,
                                        amount = suggestion.amount,
                                        merchant = suggestion.merchant,
                                        isCredit = suggestion.isCredit,
                                        appSource = suggestion.sourceApp
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in coroutine processing incoming SMS", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming SMS", e)
                pendingResult.finish()
            }
        }
    }
}
