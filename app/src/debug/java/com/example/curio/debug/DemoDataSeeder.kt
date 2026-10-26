package com.example.curio.debug

import kotlinx.coroutines.flow.first
import com.example.curio.di.AppContainer
import com.example.curio.data.user.toEntity
import com.example.curio.domain.AchievementId
import com.example.curio.domain.DayPlan
import java.time.LocalDate

class DemoDataSeeder(private val container: AppContainer) {

    sealed interface Result {
        data class Applied(val preset: DemoPreset, val touches: Int) : Result
        data class Failed(val reason: String) : Result
    }

    suspend fun apply(preset: DemoPreset): Result {
        val settings = container.settingsStore.observe().first()
        val themeId = settings.themeId ?: return Result.Failed("No theme selected: finish onboarding first")

        container.userActivity.resetAll()
        container.treeVisits.clear()

        val topicIds = container.contentIndex.topicsOf(themeId).map { it.id }
        val todayTopic = when (val plan = container.dayPlans.ensureToday(themeId, topicIds, settings.mode)) {
            is DayPlan.Ready -> plan.choice.topicId
            is DayPlan.NeedsChoice -> {
                val pick = topicIds.firstOrNull() ?: return Result.Failed("The theme has no topics")
                container.dayPlans.chooseTopic(themeId, topicIds, pick)
                pick
            }
            DayPlan.NoContent -> return Result.Failed("The selected theme has no content")
        }

        val now = container.clock.now().toEpochMilli()
        val today = LocalDate.now(container.javaClock)
        val plan = DemoPresets.build(preset, container.contentIndex, today, now, todayTopic)
            ?: return Result.Failed("The dataset is too small for the preset ${preset.key}")

        val views = container.database.viewEventDao()
        plan.touches.forEach { views.record(it) }
        plan.choices.forEach { container.database.dayChoiceDao().upsert(it.toEntity()) }

        container.achievements.evaluateNow()
        repeat(AchievementId.values().size) {
            val pending = container.achievements.observeNextPending().first() ?: return@repeat
            container.achievements.markNotified(pending.id)
        }
        container.achievements.markAllSeen()

        return Result.Applied(preset, plan.touches.size)
    }
}
