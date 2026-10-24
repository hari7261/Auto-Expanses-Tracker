package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Expense::class,
        Budget::class,
        NotificationSuggestion::class,
        AppAllowlist::class,
        SyncLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun notificationSuggestionDao(): NotificationSuggestionDao
    abstract fun appAllowlistDao(): AppAllowlistDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "private_expenses_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            // Populate allowlisted apps
            val allowlistDao = db.appAllowlistDao()
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

            // Populate some budgets (e.g. overall budget)
            val budgetDao = db.budgetDao()
            budgetDao.insertBudget(Budget(category = "All", monthlyLimit = 20000.0, alertPercent = 0.8f))
        }
    }
}
