package com.example.curio.domain

import java.time.LocalDate

class DwellTracker(private val date: LocalDate) {
    private var currentId: String? = null
    private var currentSource: ViewSource = ViewSource.FEED
    private var markedAt = 0L
    private var running = false

    fun show(id: String, source: ViewSource, nowMs: Long): List<ViewTouch> {
        if (currentId == id && currentSource == source) return emptyList()
        val out = mutableListOf<ViewTouch>()
        out += leave(nowMs)
        currentId = id
        currentSource = source
        markedAt = nowMs
        running = true
        out += ViewTouch(id, date, source, nowMs)
        return out
    }

    fun checkpoint(nowMs: Long): List<ViewTouch> {
        if (currentId == null || !running) return emptyList()
        return listOf(flush(nowMs, pageLeft = false))
    }

    fun pause(nowMs: Long): List<ViewTouch> {
        if (currentId == null || !running) return emptyList()
        val touch = flush(nowMs, pageLeft = false)
        running = false
        return listOf(touch)
    }

    fun resume(nowMs: Long) {
        if (currentId != null && !running) {
            markedAt = nowMs
            running = true
        }
    }

    fun leave(nowMs: Long): List<ViewTouch> {
        if (currentId == null) return emptyList()
        val touch = flush(nowMs, pageLeft = true)
        currentId = null
        running = false
        return listOf(touch)
    }

    private fun flush(nowMs: Long, pageLeft: Boolean): ViewTouch {
        val delta = if (running) (nowMs - markedAt).coerceIn(0L, MAX_DELTA_MS) else 0L
        markedAt = nowMs
        return ViewTouch(currentId!!, date, currentSource, nowMs, dwellDeltaMs = delta, pageLeft = pageLeft)
    }

    companion object {
        const val MAX_DELTA_MS = 120_000L
        const val CHECKPOINT_MS = 3_000L
    }
}
