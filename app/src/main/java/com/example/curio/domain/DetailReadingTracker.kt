package com.example.curio.domain

import java.time.LocalDate

class DetailReadingTracker(
    private val discoveryId: String,
    private val date: LocalDate,
) {
    private var opened = false
    private var visibleSince: Long? = null

    fun open(nowMs: Long): List<ViewTouch> {
        if (opened) return emptyList()
        opened = true
        return listOf(ViewTouch(discoveryId, date, ViewSource.DETAIL, nowMs, markCompleted = true))
    }

    fun visible(nowMs: Long) {
        if (visibleSince == null) visibleSince = nowMs
    }

    fun checkpoint(nowMs: Long): List<ViewTouch> {
        val since = visibleSince ?: return emptyList()
        visibleSince = nowMs
        return dwellTouch(nowMs - since, nowMs)
    }

    fun hidden(nowMs: Long): List<ViewTouch> {
        val since = visibleSince ?: return emptyList()
        visibleSince = null
        return dwellTouch(nowMs - since, nowMs)
    }

    private fun dwellTouch(deltaMs: Long, nowMs: Long): List<ViewTouch> {
        val delta = deltaMs.coerceIn(0L, MAX_DELTA_MS)
        if (delta == 0L) return emptyList()
        return listOf(ViewTouch(discoveryId, date, ViewSource.DETAIL, nowMs, detailDwellDeltaMs = delta))
    }

    companion object {
        const val MAX_DELTA_MS = 120_000L
        const val CHECKPOINT_MS = 3_000L
    }
}
