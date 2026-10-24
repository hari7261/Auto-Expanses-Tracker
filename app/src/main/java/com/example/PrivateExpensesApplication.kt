package com.example

import android.app.Application
import com.example.data.local.database.AppDatabase
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class PrivateExpensesApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val expenseRepository by lazy {
        ExpenseRepository(
            database.expenseDao(),
            database.budgetDao(),
            database.notificationSuggestionDao(),
            database.appAllowlistDao(),
            database.syncLogDao()
        )
    }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
}
