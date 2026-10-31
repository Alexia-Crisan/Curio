package com.example.curio.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.curio.domain.DailyMode

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "curio_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val DAILY_MODE = stringPreferencesKey("daily_mode")
        val SHAKE_ENABLED = booleanPreferencesKey("shake_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val TILT_ENABLED = booleanPreferencesKey("tilt_enabled")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val TREE_LAST_VISITED_AT = longPreferencesKey("tree_last_visited_at")
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_DONE] ?: false
    }

    val selectedTheme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_THEME] ?: "space"
    }

    val dailyMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DAILY_MODE] ?: "GUIDED"
    }

    val shakeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHAKE_ENABLED] ?: true
    }

    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAPTICS_ENABLED] ?: true
    }

    val tiltEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.TILT_ENABLED] ?: true
    }

    val darkMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE] ?: "SYSTEM"
    }

    val treeLastVisitedAt: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.TREE_LAST_VISITED_AT]
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setSelectedTheme(themeId: String) {
        context.dataStore.edit { prefs -> prefs[Keys.SELECTED_THEME] = themeId }
    }

    suspend fun setDailyMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[Keys.DAILY_MODE] = mode }
    }

    suspend fun setShakeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.SHAKE_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setTiltEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.TILT_ENABLED] = enabled }
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[Keys.DARK_MODE] = mode }
    }

    suspend fun setTreeLastVisitedAt(timestamp: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.TREE_LAST_VISITED_AT] = timestamp }
    }

    suspend fun clearTreeLastVisitedAt() {
        context.dataStore.edit { prefs -> prefs.remove(Keys.TREE_LAST_VISITED_AT) }
    }

    suspend fun completeOnboarding(themeId: String, mode: DailyMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_THEME] = themeId
            prefs[Keys.DAILY_MODE] = mode.name
            prefs[Keys.ONBOARDING_DONE] = true
        }
    }
}
