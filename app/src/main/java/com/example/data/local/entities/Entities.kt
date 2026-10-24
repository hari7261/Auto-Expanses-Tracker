package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["date"]),
        Index(value = ["category"])
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String, // e.g., Food, Shopping, Bills, Custom, etc.
    val merchant: String,
    val paymentMethod: String, // "UPI", "Card", "Netbanking", "Cash", "ATM"
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isCredit: Boolean = false, // false = DEBIT (default expense), true = CREDIT/REFUND
    val accountLast4: String? = null
)

@Entity(
    tableName = "budgets"
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "All" for overall monthly budget, or specific categories
    val monthlyLimit: Double,
    val alertPercent: Float = 0.8f // Alert user if spending exceeds this percentage (e.g. 80%)
)

@Entity(
    tableName = "notification_suggestions"
)
data class NotificationSuggestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val merchant: String,
    val paymentMethod: String,
    val sourceApp: String, // e.g. PhonePe, Paytm, HDFC, SBI
    val accountLast4: String? = null,
    val isCredit: Boolean = false,
    val timestamp: Long,
    val confidence: Float,
    val rawText: String,
    val status: String = "PENDING" // PENDING, APPROVED, IGNORED
)

@Entity(
    tableName = "app_allowlist"
)
data class AppAllowlist(
    @PrimaryKey @get:JvmName("getPackageNameGetter") val packageName: String,
    val displayName: String,
    val isEnabled: Boolean = true
)

@Entity(
    tableName = "sync_logs"
)
data class SyncLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SUCCESS", "FAILURE", "IN_PROGRESS"
    val operation: String, // "Backup Data", "Restore Data"
    val details: String // Details of sync (e.g. "Uploaded 5 expenses, 1 budget.")
)

