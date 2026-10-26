package com.example.curio.debug

import com.example.curio.domain.ContentIndex
import com.example.curio.domain.DailyMode
import com.example.curio.domain.DayChoice
import com.example.curio.domain.Topic
import com.example.curio.domain.ViewEventRecorder
import com.example.curio.domain.ViewSource
import com.example.curio.domain.ViewTouch
import java.time.LocalDate

enum class DemoPreset(val key: String) {
    FRESH("fresh"),
    ALMOST_THERE("almost_there"),
    TWO_THEMES("two_themes");

    companion object {
        fun fromKey(key: String?): DemoPreset? = entries.firstOrNull { it.key == key }
    }
}

data class DemoPlan(val touches: List<ViewTouch>, val choices: List<DayChoice>)

object DemoPresets {
    private const val DAY_MS = 86_400_000L
    private const val DAILY_FEED_SIZE = 6

    fun build(preset: DemoPreset, content: ContentIndex, today: LocalDate, nowMs: Long, todayTopicId: String?): DemoPlan? =
        when (preset) {
            DemoPreset.FRESH -> DemoPlan(emptyList(), emptyList())
            DemoPreset.ALMOST_THERE -> almostThere(content, today, nowMs, todayTopicId)
            DemoPreset.TWO_THEMES -> twoThemes(content, today, nowMs)
        }

    private fun fullTopics(content: ContentIndex): List<Topic> =
        content.topics.filter { content.discoveriesOf(it.id).size >= DAILY_FEED_SIZE }

    private fun completed(id: String, date: LocalDate, source: ViewSource, atMs: Long) =
        ViewTouch(id, date, source, atMs, dwellDeltaMs = ViewEventRecorder.COMPLETE_AT_MS, markCompleted = true)

    private fun almostThere(content: ContentIndex, today: LocalDate, nowMs: Long, todayTopicId: String?): DemoPlan? {
        val topics = fullTopics(content).filter { it.id != todayTopicId }
        if (topics.size < 5) return null
        val touches = mutableListOf<ViewTouch>()
        val choices = mutableListOf<DayChoice>()

        topics.take(3).forEachIndexed { index, topic ->
            val date = today.minusDays(index + 1L)
            val themeId = content.themeIdOfTopic(topic.id) ?: return null
            choices += DayChoice(date, themeId, topic.id, DailyMode.GUIDED)
            content.discoveriesOf(topic.id).take(DAILY_FEED_SIZE).forEach {
                touches += completed(it.id, date, ViewSource.FEED, nowMs - (index + 1L) * DAY_MS)
            }
        }

        val fourth = topics[3]
        val fourthDate = today.minusDays(4)
        choices += DayChoice(fourthDate, content.themeIdOfTopic(fourth.id) ?: return null, fourth.id, DailyMode.GUIDED)
        content.discoveriesOf(fourth.id).take(4).forEach {
            touches += completed(it.id, fourthDate, ViewSource.FEED, nowMs - 4L * DAY_MS)
        }

        val fifth = topics[4]
        content.discoveriesOf(fifth.id).take(2).forEach {
            touches += completed(it.id, fourthDate, ViewSource.BONUS, nowMs - 4L * DAY_MS)
        }
        return DemoPlan(touches, choices)
    }

    private fun twoThemes(content: ContentIndex, today: LocalDate, nowMs: Long): DemoPlan? {
        val themes = content.themes.filter { theme ->
            theme.available && theme.topics.size >= 2 &&
                content.discoveriesOf(theme.topics[0].id).size >= DAILY_FEED_SIZE
        }.take(2)
        if (themes.size < 2) return null
        val touches = mutableListOf<ViewTouch>()
        val choices = mutableListOf<DayChoice>()

        themes.forEachIndexed { index, theme ->
            val first = theme.topics[0]
            val firstDate = today.minusDays(index + 1L)
            choices += DayChoice(firstDate, theme.id, first.id, DailyMode.GUIDED)
            content.discoveriesOf(first.id).take(DAILY_FEED_SIZE).forEach {
                touches += completed(it.id, firstDate, ViewSource.FEED, nowMs - (index + 1L) * DAY_MS)
            }
            val second = theme.topics[1]
            val secondDate = today.minusDays(index + 3L)
            choices += DayChoice(secondDate, theme.id, second.id, DailyMode.GUIDED)
            content.discoveriesOf(second.id).take(3).forEach {
                touches += completed(it.id, secondDate, ViewSource.FEED, nowMs - 3L * DAY_MS)
            }
        }
        return DemoPlan(touches, choices)
    }
}
