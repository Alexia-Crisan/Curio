package com.example.curio.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.curio.domain.DailyMode
import com.example.curio.domain.SettingsStore
import com.example.curio.domain.UserSettings

class PreferencesSettingsStore(private val prefs: SettingsRepository) : SettingsStore {

    override fun observe(): Flow<UserSettings> =
        combine(prefs.selectedTheme, prefs.dailyMode) { theme, mode ->
            UserSettings(
                themeId = theme.takeIf { it.isNotBlank() },
                mode = try { DailyMode.valueOf(mode) } catch (e: Exception) { DailyMode.GUIDED }
            )
        }.distinctUntilChanged()

    override suspend fun setTheme(themeId: String) {
        prefs.setSelectedTheme(themeId)
    }

    override suspend fun setMode(mode: DailyMode) {
        prefs.setDailyMode(mode.name)
    }

    override suspend fun completeOnboarding(themeId: String, mode: DailyMode) {
        prefs.completeOnboarding(themeId, mode)
    }
}
