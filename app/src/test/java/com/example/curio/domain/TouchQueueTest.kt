package com.example.curio.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TouchQueueTest {
    private val scope = TestScope()
    private val D = LocalDate.of(2026, 10, 26)

    private fun touch(id: String, n: Int) = ViewTouch(id, D, ViewSource.FEED, n.toLong())

    @Test
    fun touchesAreWrittenInOrder() {
        val seen = mutableListOf<ViewTouch>()
        val queue = TouchQueue(scope) { seen += it }
        queue.offer(touch("a1", 1))
        queue.offer(touch("a2", 2))
        queue.offer(touch("a3", 3))
        scope.runCurrent()
        assertEquals(listOf("a1", "a2", "a3"), seen.map { it.discoveryId })
    }

    @Test
    fun offerAllKeepsTheOrder() {
        val seen = mutableListOf<ViewTouch>()
        val queue = TouchQueue(scope) { seen += it }
        queue.offerAll(listOf(touch("a1", 1), touch("a2", 2)))
        queue.offer(touch("a3", 3))
        scope.runCurrent()
        assertEquals(listOf("a1", "a2", "a3"), seen.map { it.discoveryId })
    }

    @Test
    fun aFailingWriteDoesNotStopTheQueue() {
        val seen = mutableListOf<ViewTouch>()
        val queue = TouchQueue(scope) {
            if (it.discoveryId == "bad") throw IllegalStateException("simulated failure")
            seen += it
        }
        queue.offer(touch("a1", 1))
        queue.offer(touch("bad", 2))
        queue.offer(touch("a2", 3))
        scope.runCurrent()
        assertEquals(listOf("a1", "a2"), seen.map { it.discoveryId })
    }

    @Test
    fun detailOpenedMarksCompletedWithDetailSource() {
        val seen = mutableListOf<ViewTouch>()
        val queue = TouchQueue(scope) { seen += it }
        queue.detailOpened("a1", D, 5)
        scope.runCurrent()
        assertEquals(1, seen.size)
        val t = seen.single()
        assertEquals(ViewSource.DETAIL, t.source)
        assertEquals(true, t.markCompleted)
        assertEquals(0L, t.dwellDeltaMs)
    }

    @Test
    fun detailDwellIsClampedToZeroOrMore() {
        val seen = mutableListOf<ViewTouch>()
        val queue = TouchQueue(scope) { seen += it }
        queue.detailDwell("a1", D, 5, -100)
        queue.detailDwell("a1", D, 5, 8000)
        scope.runCurrent()
        assertEquals(2, seen.size)
        assertEquals(0L, seen[0].detailDwellDeltaMs)
        assertEquals(8000L, seen[1].detailDwellDeltaMs)
    }

    @Test
    fun detailTouchNeverBeatsAFeedRow() {
        val feedEvent = ViewEvent("a1", D, 1, 3000, completed = true, skipped = false, source = ViewSource.FEED, detailDwellMs = 0)
        val touch = ViewTouch("a1", D, ViewSource.DETAIL, 5, markCompleted = true)
        val merged = ViewEventRecorder.merge(feedEvent, touch)
        assertEquals(ViewSource.FEED, merged.source)
    }
}
