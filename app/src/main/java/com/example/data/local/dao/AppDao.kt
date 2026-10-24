package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.AppAllowlist
import com.example.data.local.entities.Budget
import com.example.data.local.entities.Expense
import com.example.data.local.entities.NotificationSuggestion
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesInRange(startDate: Long, endDate: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE (merchant LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%') ORDER BY date DESC")
    fun searchExpenses(query: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT SUM(amount) FROM expenses WHERE isCredit = 0 AND date BETWEEN :startDate AND :endDate")
    fun getTotalSpendingInRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE isCredit = 0 AND date >= :todayStart")
    fun getTodaySpending(todayStart: Long): Flow<Double?>

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE isCredit = 0 AND date BETWEEN :startDate AND :endDate GROUP BY category")
    fun getCategoryBreakdown(startDate: Long, endDate: Long): Flow<List<CategorySum>>
}

data class CategorySum(
    val category: String,
    val total: Double
)

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    suspend fun getBudgetByCategory(category: String): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)
}

@Dao
interface NotificationSuggestionDao {
    @Query("SELECT * FROM notification_suggestions WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingSuggestions(): Flow<List<NotificationSuggestion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: NotificationSuggestion)

    @Query("UPDATE notification_suggestions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Query("SELECT * FROM notification_suggestions WHERE amount = :amount AND ABS(timestamp - :timestamp) < :timeWindowMs AND merchant = :merchant LIMIT 1")
    suspend fun findDuplicate(amount: Double, timestamp: Long, merchant: String, timeWindowMs: Long): NotificationSuggestion?

    @Query("SELECT * FROM notification_suggestions WHERE rawText = :rawText LIMIT 1")
    suspend fun findSuggestionByRawText(rawText: String): NotificationSuggestion?
}

@Dao
interface AppAllowlistDao {
    @Query("SELECT * FROM app_allowlist")
    fun getAllAllowedApps(): Flow<List<AppAllowlist>>

    @Query("SELECT * FROM app_allowlist WHERE isEnabled = 1")
    suspend fun getActiveAllowlistedApps(): List<AppAllowlist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppAllowlist)

    @Query("UPDATE app_allowlist SET isEnabled = :isEnabled WHERE packageName = :packageName")
    suspend fun updateAppStatus(packageName: String, isEnabled: Boolean)

    @Delete
    suspend fun deleteApp(app: AppAllowlist)
}

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 30")
    fun getRecentSyncLogs(): Flow<List<com.example.data.local.entities.SyncLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: com.example.data.local.entities.SyncLog)

    @Query("DELETE FROM sync_logs")
    suspend fun clearSyncLogs()
}

