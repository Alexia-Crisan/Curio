package com.example.curio.domain

import java.time.LocalDate

const val DAILY_FEED_SIZE = 6

/** Where a view happened. Higher priority wins when the same (discovery, date) is touched twice. */
enum class ViewSource(val priority: Int) {
    FEED(3),     // regular daily feed card
    BONUS(2),    // shake bonus card
    DETAIL(1)    // opened from Saved, QR or the tree: never counts as "active day" or day completion
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

/** One observation from the UI: "this card was on screen for X ms", "it was saved", "Detail was open for Y ms". */
data class ViewTouch(
    val discoveryId: String,
    val date: LocalDate,
    val source: ViewSource,
    val nowMs: Long,
    val dwellDeltaMs: Long = 0,
    val detailDwellDeltaMs: Long = 0,
    val markCompleted: Boolean = false,   // saved, or Detail opened
    val pageLeft: Boolean = false,        // the user swiped away: only then can a card count as skipped
)
