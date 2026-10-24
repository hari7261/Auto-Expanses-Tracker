package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val NOTIFICATION_DETECTION_ENABLED = booleanPreferencesKey("notification_detection_enabled")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val GLOBAL_BUDGET = doublePreferencesKey("global_budget")
        val BACKUP_SERVER_ENDPOINT = stringPreferencesKey("backup_server_endpoint")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
    }

    val isNotificationDetectionEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_DETECTION_ENABLED] ?: true
        }

    val backupServerEndpoint: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.BACKUP_SERVER_ENDPOINT] ?: "https://api.privateexpenses.secure/"
        }

    val isAutoSyncEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.AUTO_SYNC_ENABLED] ?: false
        }

    val themePreference: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.THEME_PREFERENCE] ?: "System"
        }

    val isBiometricLockEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_LOCK_ENABLED] ?: false
        }

    val globalBudget: Flow<Double> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.GLOBAL_BUDGET] ?: 20000.0
        }

    suspend fun setNotificationDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_DETECTION_ENABLED] = enabled
        }
    }

    suspend fun setThemePreference(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_PREFERENCE] = theme
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setGlobalBudget(budget: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GLOBAL_BUDGET] = budget
        }
    }

    suspend fun setBackupServerEndpoint(endpoint: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_SERVER_ENDPOINT] = endpoint
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SYNC_ENABLED] = enabled
        }
    }
}
