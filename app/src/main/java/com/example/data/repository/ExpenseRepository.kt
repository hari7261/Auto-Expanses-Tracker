package com.example.data.repository

import com.example.data.local.dao.*
import com.example.data.local.entities.AppAllowlist
import com.example.data.local.entities.Budget
import com.example.data.local.entities.Expense
import com.example.data.local.entities.NotificationSuggestion
import com.example.data.local.entities.SyncLog
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val suggestionDao: NotificationSuggestionDao,
    private val allowlistDao: AppAllowlistDao,
    private val syncLogDao: SyncLogDao
) {
    // Sync actions
    val recentSyncLogs: Flow<List<SyncLog>> = syncLogDao.getRecentSyncLogs()

    suspend fun insertSyncLog(log: SyncLog) =
        syncLogDao.insertSyncLog(log)

    suspend fun clearSyncLogs() =
        syncLogDao.clearSyncLogs()

    // Expense actions
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesInRange(startDate: Long, endDate: Long): Flow<List<Expense>> =
        expenseDao.getExpensesInRange(startDate, endDate)

    fun searchExpenses(query: String): Flow<List<Expense>> =
        expenseDao.searchExpenses(query)

    suspend fun insertExpense(expense: Expense): Long =
        expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) =
        expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) =
        expenseDao.deleteExpense(expense)

    fun getTotalSpendingInRange(startDate: Long, endDate: Long): Flow<Double?> =
        expenseDao.getTotalSpendingInRange(startDate, endDate)

    fun getTodaySpending(todayStart: Long): Flow<Double?> =
        expenseDao.getTodaySpending(todayStart)

    fun getCategoryBreakdown(startDate: Long, endDate: Long): Flow<List<CategorySum>> =
        expenseDao.getCategoryBreakdown(startDate, endDate)

    // Budget actions
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

    suspend fun getBudgetByCategory(category: String): Budget? =
        budgetDao.getBudgetByCategory(category)

    suspend fun insertBudget(budget: Budget) =
        budgetDao.insertBudget(budget)

    suspend fun updateBudget(budget: Budget) =
        budgetDao.updateBudget(budget)

    suspend fun deleteBudget(budget: Budget) =
        budgetDao.deleteBudget(budget)

    // Notification suggestions
    val pendingSuggestions: Flow<List<NotificationSuggestion>> =
        suggestionDao.getPendingSuggestions()

    suspend fun insertSuggestion(suggestion: NotificationSuggestion): Boolean {
        // 1. Check if same raw text already exists in database (exact message body match)
        if (suggestion.rawText.isNotEmpty()) {
            val duplicateByText = suggestionDao.findSuggestionByRawText(suggestion.rawText)
            if (duplicateByText != null) {
                return false
            }
        }

        // 2. Detect close duplicates: same amount, similar timestamp (15 minute window), and same merchant
        val duplicate = suggestionDao.findDuplicate(
            amount = suggestion.amount,
            timestamp = suggestion.timestamp,
            merchant = suggestion.merchant,
            timeWindowMs = 15 * 60 * 1000L // 15 mins
        )
        return if (duplicate == null) {
            suggestionDao.insertSuggestion(suggestion)
            true
        } else {
            false
        }
    }

    suspend fun resolveSuggestion(id: Int, saveAsExpense: Boolean, finalExpense: Expense? = null) {
        if (saveAsExpense && finalExpense != null) {
            expenseDao.insertExpense(finalExpense)
            suggestionDao.updateStatus(id, "APPROVED")
        } else {
            suggestionDao.updateStatus(id, "IGNORED")
        }
    }

    suspend fun ignoreSuggestion(id: Int) {
        suggestionDao.updateStatus(id, "IGNORED")
    }

    // App Allowlist
    val allAllowedApps: Flow<List<AppAllowlist>> = allowlistDao.getAllAllowedApps()

    suspend fun getActiveAllowlistedApps(): List<AppAllowlist> =
        allowlistDao.getActiveAllowlistedApps()

    suspend fun insertAppToAllowlist(app: AppAllowlist) =
        allowlistDao.insertApp(app)

    suspend fun updateAppStatus(packageName: String, isEnabled: Boolean) =
        allowlistDao.updateAppStatus(packageName, isEnabled)

    suspend fun deleteAppFromAllowlist(app: AppAllowlist) =
        allowlistDao.deleteApp(app)
}
