package com.example.curio.debug

import com.example.curio.data.user.CurioDatabase
import com.example.curio.data.user.UserActivityRepository
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.ViewSource
import com.example.curio.domain.ViewTouch
import java.time.Clock
import java.time.LocalDate

class DebugSeeder(
    private val db: CurioDatabase,
    private val content: ContentIndex,
    private val activity: UserActivityRepository,
    private val clock: Clock,
) {
    suspend fun completeCards(count: Int) {
        val today = LocalDate.now(clock)
        content.discoveries.take(count).forEach {
            db.viewEventDao().record(ViewTouch(it.id, today, ViewSource.FEED, clock.millis(), markCompleted = true))
        }
    }
    suspend fun saveCards(count: Int) =
        content.discoveries.take(count).forEach { activity.setSaved(it.id, true, ViewSource.DETAIL, recordActivity = false) }
    suspend fun reset() = activity.resetAll()
}
