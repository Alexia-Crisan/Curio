package com.example.curio.domain

import kotlinx.coroutines.flow.Flow

interface ActivitySource {
    fun observeSnapshot(): Flow<ActivitySnapshot>
}

fun interface CurrentThemeSource {
    fun observe(): Flow<String?>
}

fun interface DailyModeSource {
    fun observe(): Flow<DailyMode>
}

interface AchievementSource {
    fun observeProgress(): Flow<List<AchievementProgress>>
    fun observeNextPending(): Flow<AchievementDef?>
    fun observeUnseenCount(): Flow<Int>
    suspend fun markNotified(id: AchievementId)
    suspend fun markAllSeen()
}
