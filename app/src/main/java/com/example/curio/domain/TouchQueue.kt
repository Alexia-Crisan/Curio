package com.example.curio.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.time.LocalDate

fun interface TouchSink {
    suspend fun record(touch: ViewTouch)
}

class TouchQueue(scope: CoroutineScope, private val sink: TouchSink) {
    private val channel = Channel<ViewTouch>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (touch in channel) {
                try {
                    sink.record(touch)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                }
            }
        }
    }

    fun offer(touch: ViewTouch) {
        channel.trySend(touch)
    }

    fun offerAll(touches: List<ViewTouch>) = touches.forEach(::offer)

    fun detailOpened(discoveryId: String, date: LocalDate, nowMs: Long) =
        offer(ViewTouch(discoveryId, date, ViewSource.DETAIL, nowMs, markCompleted = true))

    fun detailDwell(discoveryId: String, date: LocalDate, nowMs: Long, deltaMs: Long) =
        offer(ViewTouch(discoveryId, date, ViewSource.DETAIL, nowMs, detailDwellDeltaMs = deltaMs.coerceAtLeast(0)))
}
