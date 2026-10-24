package com.example.curio.data.user

import androidx.room.withTransaction
import com.example.curio.domain.DailyMode
import com.example.curio.domain.DayChoice
import com.example.curio.domain.DayPlan
import com.example.curio.domain.TopicSelector
import java.time.Clock
import java.time.LocalDate

class DayPlanRepository(
    private val db: CurioDatabase,
    private val clock: Clock,
) {
    private val choices = db.dayChoiceDao()
    private val views = db.viewEventDao()

    suspend fun peek(date: LocalDate): DayChoice? = choices.find(date.toString())?.toDomain()

    suspend fun ensureToday(themeId: String, topicIds: List<String>, mode: DailyMode): DayPlan = db.withTransaction {
        val today = LocalDate.now(clock)
        val key = today.toString()
        val existing = choices.find(key)?.toDomain()
        val locked = views.completedFeedCount(key) > 0

        if (existing != null) {
            val unchanged = existing.themeId == themeId && existing.mode == mode
            if (unchanged || locked) {
                return@withTransaction DayPlan.Ready(existing, settingsLocked = locked && !unchanged)
            }
        }

        val topic = TopicSelector.select(mode, today, themeId, topicIds, storedChoice = null)
        when {
            topic != null -> {
                val choice = DayChoice(today, themeId, topic, mode)
                choices.upsert(choice.toEntity())
                DayPlan.Ready(choice)
            }
            mode == DailyMode.CHOOSE && topicIds.isNotEmpty() -> {
                choices.delete(key)
                DayPlan.NeedsChoice(today)
            }
            else -> {
                choices.delete(key)
                DayPlan.NoContent
            }
        }
    }

    suspend fun chooseTopic(themeId: String, topicIds: List<String>, topicId: String): DayPlan = db.withTransaction {
        require(topicId in topicIds) { "$topicId is not a topic of $themeId" }
        val today = LocalDate.now(clock)
        val key = today.toString()

        if (views.completedFeedCount(key) > 0) {
            choices.find(key)?.toDomain()?.let { return@withTransaction DayPlan.Ready(it, settingsLocked = true) }
        }
        val choice = DayChoice(today, themeId, topicId, DailyMode.CHOOSE)
        choices.upsert(choice.toEntity())
        DayPlan.Ready(choice)
    }
}
