package com.example.curio.domain

import java.time.LocalDate

data class ActivitySnapshot(
    val completedIds: Set<String>,            // distinct completed discoveries (any date, any source), known content only
    val completedFeedDays: Set<LocalDate>,    // "active days": >= 1 completed FEED row
    val savedIds: Set<String>,                // currently saved, known content only
    val deepReadIds: Set<String>,             // KNOWLEDGE cards with >= 8 s total on Detail
    val bonusCompletedIds: Set<String>,       // bonus cards completed for the FIRST time (revisits excluded)
    val completedDays: Set<LocalDate>,        // days whose chosen topic is fully completed
    val firstCompletedAt: Map<String, Long>,  // discoveryId -> earliest firstViewedAt of a completed row
)

object ActivitySnapshotBuilder {
    const val DEEP_READ_MS = 8_000L

    fun build(
        events: List<ViewEvent>,
        savedIds: Set<String>,
        choices: List<DayChoice>,
        c: ContentIndex,
    ): ActivitySnapshot {
        val known = events.filter { c.contains(it.discoveryId) }        // removed content never counts
        val completedRows = known.filter { it.completed }
        val completedByCard = completedRows.groupBy { it.discoveryId }

        val firstCompletedDay = completedByCard.mapValues { (_, rows) -> rows.minOf { it.date.toEpochDay() } }

        val deepReads = known.groupBy { it.discoveryId }
            .filter { (id, rows) ->
                c.discovery(id)?.type == ContentType.KNOWLEDGE &&
                    rows.sumOf { it.detailDwellMs } >= DEEP_READ_MS
            }.keys

        // A bonus counts only if it was the card's first completion ever: a "revisit" is not a hidden discovery.
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
