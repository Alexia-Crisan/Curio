package com.example.curio.domain

import kotlinx.coroutines.flow.Flow

/** Anything that can emit the user's activity snapshot. UserActivityRepository implements it. */
interface ActivitySource {
    fun observeSnapshot(): Flow<ActivitySnapshot>
}

/** The theme the user picked for this month (from the settings store). null = none chosen yet. */
fun interface CurrentThemeSource {
    fun observe(): Flow<String?>
}

interface AchievementSource {
    fun observeProgress(): Flow<List<AchievementProgress>>   // only the active (MVP) badges
    fun observeNextPending(): Flow<AchievementDef?>          // oldest unlock whose banner was not shown yet
    fun observeUnseenCount(): Flow<Int>                      // for the "new" dot on the Progress tab
    suspend fun markNotified(id: AchievementId)              // call when the banner has been shown
    suspend fun markAllSeen()                                // call when the Achievements screen is opened
}
