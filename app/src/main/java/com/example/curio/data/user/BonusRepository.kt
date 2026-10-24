package com.example.curio.data.user

import androidx.room.withTransaction
import com.example.curio.domain.Discovery
import com.example.curio.domain.ShakeBonusSelector
import com.example.curio.domain.ViewSource
import com.example.curio.domain.ViewTouch
import java.time.Clock
import java.time.LocalDate

class BonusRepository(
    private val db: CurioDatabase,
    private val allDiscoveries: List<Discovery>,
    private val clock: Clock,
    private val maxPerDay: Int = ShakeBonusSelector.MAX_BONUS_PER_DAY,
) {
    private val views = db.viewEventDao()
    private val byId = allDiscoveries.associateBy { it.id }

    /**
     * Pick a bonus and, if one was found, write its BONUS row immediately (completed = false).
     * Select + write are ONE transaction, so two quick shakes can never get the same card.
     * The caller must NOT call this while showing a past day's feed.
     */
    suspend fun reveal(themeId: String, todayTopicId: String): ShakeBonusSelector.Result = db.withTransaction {
        val today = LocalDate.now(clock)
        val events = views.getAll().map { it.toDomain() }
        val result = ShakeBonusSelector.select(today, themeId, todayTopicId, allDiscoveries, events, maxPerDay)
        if (result is ShakeBonusSelector.Result.Found) {
            views.record(ViewTouch(result.discovery.id, today, ViewSource.BONUS, nowMs = clock.millis()))
        }
        result
    }

    /** Today's bonus cards in the order they were revealed, to rebuild the feed after a restart. */
    suspend fun todaysBonusCards(): List<Discovery> =
        views.bonusRows(LocalDate.now(clock).toString()).mapNotNull { byId[it.discoveryId] }
}
