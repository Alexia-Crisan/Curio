package com.example.curio.domain

import java.time.LocalDate

data class ActivitySnapshot(
    val completedIds: Set<String>,
    val completedFeedDays: Set<LocalDate>,
    val savedIds: Set<String>,
    val deepReadIds: Set<String>,
    val bonusCompletedIds: Set<String>,
    val completedDays: Set<LocalDate>,
    val firstCompletedAt: Map<String, Long>,
)

object ActivitySnapshotBuilder {
    const val DEEP_READ_MS = 8_000L

    fun build(
        events: List<ViewEvent>,
        savedIds: Set<String>,
        choices: List<DayChoice>,
        c: ContentIndex,
    ): ActivitySnapshot {
        val known = events.filter { c.contains(it.discoveryId) }
        val completedRows = known.filter { it.completed }
        val completedByCard = completedRows.groupBy { it.discoveryId }

        val firstCompletedDay = completedByCard.mapValues { (_, rows) -> rows.minOf { it.date.toEpochDay() } }

        val deepReads = known.groupBy { it.discoveryId }
            .filter { (id, rows) ->
                c.discovery(id)?.type == ContentType.KNOWLEDGE &&
                    rows.sumOf { it.detailDwellMs } >= DEEP_READ_MS
            }.keys

        val bonus = completedRows
            .filter { it.source == ViewSource.BONUS && it.date.toEpochDay() == firstCompletedDay[it.discoveryId] }
            .map { it.discoveryId }.toSet()

        return ActivitySnapshot(
            completedIds = completedByCard.keys,
            completedFeedDays = completedRows.filter { it.source == ViewSource.FEED }.map { it.date }.toSet(),
            savedIds = savedIds.filter { c.contains(it) }.toSet(),
            deepReadIds = deepReads,
            bonusCompletedIds = bonus,
            completedDays = choices
                .filter { ProgressCalculator.dayProgress(it, known, c.membership).isComplete }
                .map { it.date }.toSet(),
            firstCompletedAt = completedByCard.mapValues { (_, rows) -> rows.minOf { it.firstViewedAt } },
        )
    }
}
