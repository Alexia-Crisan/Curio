package com.example.curio.domain

import java.time.LocalDate
import java.time.YearMonth

class TopicMembership(private val byTopic: Map<String, Set<String>>) {
    fun discoveriesOf(topicId: String): Set<String> = byTopic[topicId].orEmpty()

    companion object {
        fun from(discoveries: List<Discovery>) = TopicMembership(
            discoveries.groupBy({ it.topicId }, { it.id }).mapValues { it.value.toSet() }
        )
    }
}

enum class DayStatus { NOT_STARTED, IN_PROGRESS, COMPLETE }

data class DayProgress(val date: LocalDate, val topicId: String, val completed: Int, val total: Int) {
    val isComplete: Boolean get() = total > 0 && completed >= total
    val status: DayStatus
        get() = when {
            isComplete -> DayStatus.COMPLETE
            completed > 0 -> DayStatus.IN_PROGRESS
            else -> DayStatus.NOT_STARTED
        }
}

data class MonthProgress(val month: YearMonth, val days: Map<LocalDate, DayProgress>) {
    val daysInMonth: Int get() = month.lengthOfMonth()
    val daysCompleted: Int get() = days.values.count { it.isComplete }
    fun statusOf(date: LocalDate): DayStatus = days[date]?.status ?: DayStatus.NOT_STARTED
}

object ProgressCalculator {

    fun dayProgress(choice: DayChoice, events: Collection<ViewEvent>, membership: TopicMembership): DayProgress {
        val topicItems = membership.discoveriesOf(choice.topicId)
        val done = events.asSequence()
            .filter { it.date == choice.date }
            .filter { it.completed && it.source != ViewSource.BONUS }
            .filter { it.discoveryId in topicItems }
            .map { it.discoveryId }
            .toSet().size
        return DayProgress(choice.date, choice.topicId, done, topicItems.size)
    }

    fun monthProgress(
        month: YearMonth,
        events: List<ViewEvent>,
        choices: List<DayChoice>,
        membership: TopicMembership,
    ): MonthProgress {
        val eventsByDate = events.groupBy { it.date }
        val days = choices
            .filter { YearMonth.from(it.date) == month }
            .associate { it.date to dayProgress(it, eventsByDate[it.date].orEmpty(), membership) }
        return MonthProgress(month, days)
    }
}
