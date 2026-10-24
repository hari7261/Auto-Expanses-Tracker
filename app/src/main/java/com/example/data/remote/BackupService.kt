package com.example.data.remote

import com.example.data.local.entities.Expense
import com.example.data.local.entities.Budget
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

class SyncBackupPayload(
    val expenses: List<Expense>,
    val budgets: List<Budget>
)

class SyncResponse(
    val status: String,
    val message: String,
    val timestamp: Long
)

interface BackupApiService {
    @POST("v1/backup")
    suspend fun backupData(
        @Header("Authorization") authToken: String,
        @Body payload: SyncBackupPayload
    ): SyncResponse

    @GET("v1/restore")
    suspend fun restoreData(
        @Header("Authorization") authToken: String
    ): SyncBackupPayload
}

object BackupServiceFactory {
    // In-memory persistent "remote" backend simulated storage
    private var remoteExpenses: List<Expense> = emptyList()
    private var remoteBudgets: List<Budget> = emptyList()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private class MockSyncInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url = request.url.toString()

            return when {
                url.contains("v1/backup") -> {
                    // Simulate typical network roundtrip latency
                    try {
                        Thread.sleep(1200)
                    } catch (e: InterruptedException) {
                        // ignore
                    }

                    var countSaved = 0
                    val requestBody = request.body
                    if (requestBody != null) {
                        try {
                            val buffer = okio.Buffer()
                            requestBody.writeTo(buffer)
                            val json = buffer.readUtf8()
                            
                            val adapter = moshi.adapter(SyncBackupPayload::class.java)
                            val payload = adapter.fromJson(json)
                            if (payload != null) {
                                remoteExpenses = payload.expenses
                                remoteBudgets = payload.budgets
                                countSaved = remoteExpenses.size
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val successJson = """
                        {
                            "status": "SUCCESS",
                            "message": "Data backed up to secure private ledger backend. Synced $countSaved expenses and ${remoteBudgets.size} budgets.",
                            "timestamp": ${System.currentTimeMillis()}
                        }
                    """.trimIndent()

                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(successJson.toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                }
                url.contains("v1/restore") -> {
                    // Simulate typical network roundtrip latency
                    try {
                        Thread.sleep(1200)
                    } catch (e: InterruptedException) {
                        // ignore
                    }

                    // If back-end is empty, pre-populate with some beautiful, Apple-designed mock data to restore
                    if (remoteExpenses.isEmpty()) {
                        val now = System.currentTimeMillis()
                        val dayMs = 24 * 60 * 60 * 1000L
                        remoteExpenses = listOf(
                            Expense(amount = 1850.0, category = "Food", merchant = "Apple Cafe", paymentMethod = "UPI", date = now - 3 * 3600 * 1000L, notes = "Lunch with design team"),
                            Expense(amount = 45000.0, category = "Shopping", merchant = "Apple Store", paymentMethod = "Card", date = now - 1 * dayMs, notes = "iPhone 16 Pro Cover"),
                            Expense(amount = 899.0, category = "Bills", merchant = "iCloud Sync", paymentMethod = "UPI", date = now - 2 * dayMs, notes = "2TB Monthly storage plan"),
                            Expense(amount = 120000.0, category = "Others", merchant = "Credit Salary", paymentMethod = "Netbanking", date = now - 4 * dayMs, notes = "Monthly salary transfer", isCredit = true)
                        )
                        remoteBudgets = listOf(
                            Budget(category = "All", monthlyLimit = 50000.0, alertPercent = 0.8f),
                            Budget(category = "Food", monthlyLimit = 8000.0, alertPercent = 0.9f),
                            Budget(category = "Shopping", monthlyLimit = 15000.0, alertPercent = 0.8f)
                        )
                    }

                    val payload = SyncBackupPayload(remoteExpenses, remoteBudgets)
                    val adapter = moshi.adapter(SyncBackupPayload::class.java)
                    val responseJson = adapter.toJson(payload)

                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(responseJson.toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                }
                else -> {
                    chain.proceed(request)
                }
            }
        }
    }

    fun create(baseUrl: String = "https://api.privateexpenses.secure/"): BackupApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(MockSyncInterceptor())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(BackupApiService::class.java)
    }
}
