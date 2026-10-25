package com.example.curio.domain

object ViewEventRecorder {
    const val COMPLETE_AT_MS = 3_000L
    const val SKIP_BELOW_MS = 1_500L

    fun merge(existing: ViewEvent?, t: ViewTouch): ViewEvent {
        val dwell = (existing?.dwellMs ?: 0L) + t.dwellDeltaMs
        val detail = (existing?.detailDwellMs ?: 0L) + t.detailDwellDeltaMs

        val completed = existing?.completed == true || t.markCompleted || dwell >= COMPLETE_AT_MS

        val source = listOfNotNull(existing?.source, t.source).maxByOrNull { it.priority }!!

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
