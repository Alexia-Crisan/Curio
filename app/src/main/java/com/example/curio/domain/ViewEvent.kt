package com.example.curio.domain

import java.time.LocalDate

const val DAILY_FEED_SIZE = 6

enum class ViewSource(val priority: Int) {
    FEED(3),
    BONUS(2),
    DETAIL(1)
}

data class ViewEvent(
    val discoveryId: String,
    val date: LocalDate,
    val firstViewedAt: Long,
    val dwellMs: Long,
    val completed: Boolean,
    val skipped: Boolean,
    val source: ViewSource,
    val detailDwellMs: Long,
)

data class ViewTouch(
    val discoveryId: String,
    val date: LocalDate,
    val source: ViewSource,
    val nowMs: Long,
    val dwellDeltaMs: Long = 0,
    val detailDwellDeltaMs: Long = 0,
    val markCompleted: Boolean = false,
    val pageLeft: Boolean = false,
)
