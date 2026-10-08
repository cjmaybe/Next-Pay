package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.OperationMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nextpay_preferences")

class PreferencesRepository(private val context: Context) {

    companion object {
        val KEY_OPERATION_MODE = stringPreferencesKey("operation_mode") // "AUTO", "ADVANCED", "MANUAL"
        val KEY_DEFAULT_SIM_SLOT = intPreferencesKey("default_sim_slot") // 1 or 2
        val KEY_SOUND_FEEDBACK = booleanPreferencesKey("sound_feedback")
        val KEY_VIBRATION_FEEDBACK = booleanPreferencesKey("vibration_feedback")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_LAST_BALANCE = stringPreferencesKey("last_balance")
        val KEY_LAST_BALANCE_TIMESTAMP = longPreferencesKey("last_balance_timestamp")
        val KEY_LAST_BALANCE_ACCOUNT = stringPreferencesKey("last_balance_account")
        val KEY_SECURITY_PIN = stringPreferencesKey("security_pin")
        val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val KEY_AUTO_OVERLAY_DISMISS = booleanPreferencesKey("auto_overlay_dismiss")
    }

    val operationMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_OPERATION_MODE] ?: "AUTO"
    }

    val operationModeEnum: Flow<OperationMode> = context.dataStore.data.map { prefs ->
        val mode = prefs[KEY_OPERATION_MODE] ?: "AUTO"
        try {
            OperationMode.valueOf(mode)
        } catch (_: Exception) {
            OperationMode.AUTO
        }
    }

    val defaultSimSlot: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_SIM_SLOT] ?: 1
    }

    val soundFeedback: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOUND_FEEDBACK] ?: true
    }

    val vibrationFeedback: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_VIBRATION_FEEDBACK] ?: true
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val lastBalance: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_BALANCE] ?: ""
    }

    val lastBalanceText: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_BALANCE]?.ifBlank { null }
    }

    val lastBalanceTimestamp: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_BALANCE_TIMESTAMP]?.takeIf { it > 0 }
    }

    val lastBalanceAccount: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_BALANCE_ACCOUNT] ?: ""
    }

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LOCK_ENABLED] ?: false
    }

    suspend fun setOperationMode(mode: String) {
        context.dataStore.edit { it[KEY_OPERATION_MODE] = mode }
    }

    suspend fun setOperationMode(mode: OperationMode) {
        context.dataStore.edit { it[KEY_OPERATION_MODE] = mode.name }
    }

    suspend fun setDefaultSimSlot(slot: Int) {
        context.dataStore.edit { it[KEY_DEFAULT_SIM_SLOT] = slot }
    }

    suspend fun setSoundFeedback(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_FEEDBACK] = enabled }
    }

    suspend fun setVibrationFeedback(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATION_FEEDBACK] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun saveBalance(balance: String, account: String) {
        context.dataStore.edit {
            it[KEY_LAST_BALANCE] = balance
            it[KEY_LAST_BALANCE_ACCOUNT] = account
            it[KEY_LAST_BALANCE_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun setLastBalance(text: String, timestamp: Long) {
        context.dataStore.edit {
            it[KEY_LAST_BALANCE] = text
            it[KEY_LAST_BALANCE_TIMESTAMP] = timestamp
        }
    }

    suspend fun clearLastBalance() {
        context.dataStore.edit {
            it.remove(KEY_LAST_BALANCE)
            it.remove(KEY_LAST_BALANCE_TIMESTAMP)
            it.remove(KEY_LAST_BALANCE_ACCOUNT)
        }
    }

    suspend fun setAppLock(enabled: Boolean, pin: String? = null) {
        context.dataStore.edit {
            it[KEY_APP_LOCK_ENABLED] = enabled
            if (pin != null) {
                it[KEY_SECURITY_PIN] = pin
            }
        }
    }
}
