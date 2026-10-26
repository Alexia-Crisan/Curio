package com.example.curio.domain

import kotlinx.coroutines.flow.Flow

data class UserSettings(val themeId: String?, val mode: DailyMode)

interface SettingsStore {
    fun observe(): Flow<UserSettings>
    suspend fun setTheme(themeId: String)
    suspend fun setMode(mode: DailyMode)
    suspend fun completeOnboarding(themeId: String, mode: DailyMode)
}

fun interface ProgressResetter {
    suspend fun resetAll()
}
