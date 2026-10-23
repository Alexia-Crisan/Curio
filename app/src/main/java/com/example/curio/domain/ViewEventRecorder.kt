package com.example.curio.domain

object ViewEventRecorder {
    const val COMPLETE_AT_MS = 3_000L   // plan: settled >= 3 s
    const val SKIP_BELOW_MS = 1_500L    // plan: settled < 1.5 s

    fun merge(existing: ViewEvent?, t: ViewTouch): ViewEvent {
        val dwell = (existing?.dwellMs ?: 0L) + t.dwellDeltaMs
        val detail = (existing?.detailDwellMs ?: 0L) + t.detailDwellDeltaMs

        // plan: completed = settled >= 3 s OR Detail opened OR saved. Once true, it stays true.
        val completed = existing?.completed == true || t.markCompleted || dwell >= COMPLETE_AT_MS

        val source = listOfNotNull(existing?.source, t.source).maxByOrNull { it.priority }!!

        // "skipped" is a future signal: only decided when the user leaves the card, never when completed.
        val skipped = if (t.pageLeft) dwell < SKIP_BELOW_MS else (existing?.skipped ?: false)

        return ViewEvent(
            discoveryId = t.discoveryId,
            date = t.date,
            firstViewedAt = existing?.firstViewedAt ?: t.nowMs,
            dwellMs = dwell,
            completed = completed,
            skipped = skipped && !completed,
            source = source,
            detailDwellMs = detail,
        )
    }
}
