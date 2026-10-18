package com.warrantywallet.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.warrantywallet.app.core.model.AppLanguage
import com.warrantywallet.app.core.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "warranty_wallet_prefs")

class PreferenceManager(private val context: Context) {

    private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    private val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")
    private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_IS_GUEST = booleanPreferencesKey("is_guest")
    private val KEY_LAST_SYNC_MILLIS = longPreferencesKey("last_sync_millis")

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_MODE] ?: AppThemeMode.SYSTEM.name
        try {
            AppThemeMode.valueOf(raw)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    val language: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_APP_LANGUAGE] ?: AppLanguage.ENGLISH.code
        AppLanguage.entries.find { it.code == raw } ?: AppLanguage.ENGLISH
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = language.code
        }
    }

    val userId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    suspend fun setUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId != null) {
                prefs[KEY_USER_ID] = userId
                prefs[KEY_IS_GUEST] = false
            } else {
                prefs.remove(KEY_USER_ID)
                prefs[KEY_IS_GUEST] = true
            }
        }
    }

    val isGuest: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_GUEST] ?: true
    }

    suspend fun setGuestMode(isGuest: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_GUEST] = isGuest
        }
    }

    val lastSyncMillis: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_MILLIS]
    }

    suspend fun setLastSyncMillis(millis: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_MILLIS] = millis
        }
    }

    suspend fun clearPreferences() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
