package com.example.curio.domain

import java.time.LocalDate
import java.util.Random

object ShakeBonusSelector {
    const val MAX_BONUS_PER_DAY = 3

    sealed interface Result {
        data class Found(val discovery: Discovery, val isRevisit: Boolean) : Result
        data object CapReached : Result
        data object NothingLeft : Result
    }

    fun select(
        date: LocalDate,
        themeId: String,
        todayTopicId: String,
        allDiscoveries: List<Discovery>,
        events: Collection<ViewEvent>,
        maxPerDay: Int = MAX_BONUS_PER_DAY,
    ): Result {
        val shownAsBonusToday = events
            .filter { it.date == date && it.source == ViewSource.BONUS }
            .map { it.discoveryId }
            .toSet()
        if (shownAsBonusToday.size >= maxPerDay) return Result.CapReached

        val candidates = allDiscoveries
            .filter { it.themeId == themeId && it.topicId != todayTopicId && it.id !in shownAsBonusToday }
            .sortedBy { it.id }
        if (candidates.isEmpty()) return Result.NothingLeft

        val viewedIds = events.map { it.discoveryId }.toSet()
        val unviewed = candidates.filter { it.id !in viewedIds }
        if (unviewed.isNotEmpty()) {
            val seed = date.toEpochDay() * 1000L + themeId.hashCode() + shownAsBonusToday.size
            return Result.Found(unviewed[Random(seed).nextInt(unviewed.size)], isRevisit = false)
        }

        val lastViewedAt = events.groupBy { it.discoveryId }.mapValues { (_, rows) -> rows.maxOf { it.firstViewedAt } }
        val oldest = candidates.minWith(compareBy<Discovery> { lastViewedAt[it.id] ?: Long.MIN_VALUE }.thenBy { it.id })
        return Result.Found(oldest, isRevisit = true)
    }
}
