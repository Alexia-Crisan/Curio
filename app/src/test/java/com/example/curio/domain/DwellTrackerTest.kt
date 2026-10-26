package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DwellTrackerTest {
    private val D = LocalDate.of(2026, 10, 26)
    private val t = DwellTracker(D)

    private fun merge(touches: List<ViewTouch>): ViewEvent? =
        touches.fold<ViewTouch, ViewEvent?>(null) { acc, touch -> ViewEventRecorder.merge(acc, touch) }

    @Test
    fun showEmitsAZeroDwellTouch() {
        val touches = t.show("a1", ViewSource.FEED, 10_000)
        assertEquals(1, touches.size)
        val touch = touches.single()
        assertEquals("a1", touch.discoveryId)
        assertEquals(D, touch.date)
        assertEquals(ViewSource.FEED, touch.source)
        assertEquals(0L, touch.dwellDeltaMs)
        assertFalse(touch.pageLeft)
        assertFalse(touch.markCompleted)
    }

    @Test
    fun checkpointAccumulatesWithoutLeaving() {
        t.show("a1", ViewSource.FEED, 10_000)
        val c1 = t.checkpoint(13_000).single()
        assertEquals(3_000L, c1.dwellDeltaMs)
        assertFalse(c1.pageLeft)

        val c2 = t.checkpoint(15_000).single()
        assertEquals(2_000L, c2.dwellDeltaMs)
    }

    @Test
    fun leaveFlushesAndMarksPageLeft() {
        t.show("a1", ViewSource.FEED, 10_000)
        val touch = t.leave(11_000).single()
        assertEquals(1_000L, touch.dwellDeltaMs)
        assertTrue(touch.pageLeft)
    }

    @Test
    fun showingAnotherCardLeavesTheFirst() {
        t.show("a1", ViewSource.FEED, 10_000)
        val touches = t.show("a2", ViewSource.FEED, 12_000)
        assertEquals(2, touches.size)
        assertEquals("a1", touches[0].discoveryId)
        assertEquals(2_000L, touches[0].dwellDeltaMs)
        assertTrue(touches[0].pageLeft)

        assertEquals("a2", touches[1].discoveryId)
        assertEquals(0L, touches[1].dwellDeltaMs)
        assertFalse(touches[1].pageLeft)
    }

    @Test
    fun theSameCardTwiceIsIgnored() {
        t.show("a1", ViewSource.FEED, 10_000)
        val touches = t.show("a1", ViewSource.FEED, 11_000)
        assertTrue(touches.isEmpty())
    }

    @Test
    fun theSameIdWithAnotherSourceIsANewShow() {
        t.show("a1", ViewSource.FEED, 10_000)
        val touches = t.show("a1", ViewSource.BONUS, 11_000)
        assertEquals(2, touches.size)
    }

    @Test
    fun pauseStopsTheClock() {
        t.show("a1", ViewSource.FEED, 10_000)
        val p = t.pause(12_000).single()
        assertEquals(2_000L, p.dwellDeltaMs)

        t.resume(60_000)
        val c = t.checkpoint(61_000).single()
        assertEquals(1_000L, c.dwellDeltaMs)
    }

    @Test
    fun leaveWhilePausedAddsNothing() {
        t.show("a1", ViewSource.FEED, 10_000)
        t.pause(12_000)
        val l = t.leave(50_000).single()
        assertEquals(0L, l.dwellDeltaMs)
        assertTrue(l.pageLeft)
    }

    @Test
    fun pauseTwiceIsHarmless() {
        t.show("a1", ViewSource.FEED, 10_000)
        t.pause(12_000)
        assertTrue(t.pause(13_000).isEmpty())
    }

    @Test
    fun resumeWithoutACardDoesNothing() {
        t.resume(1_000)
        assertTrue(t.checkpoint(5_000).isEmpty())
    }

    @Test
    fun checkpointWithoutACardIsEmpty() {
        assertTrue(t.checkpoint(1_000).isEmpty())
    }

    @Test
    fun aHugeGapIsCapped() {
        t.show("a1", ViewSource.FEED, 0)
        val c = t.checkpoint(1_000_000).single()
        assertEquals(DwellTracker.MAX_DELTA_MS, c.dwellDeltaMs)
    }

    @Test
    fun aBackwardsClockAddsNothing() {
        t.show("a1", ViewSource.FEED, 10_000)
        val c = t.checkpoint(9_000).single()
        assertEquals(0L, c.dwellDeltaMs)
    }

    @Test
    fun theTouchesCarryTheTrackersDate() {
        val tracker27 = DwellTracker(LocalDate.of(2026, 10, 27))
        val touches = tracker27.show("a1", ViewSource.FEED, 0)
        assertEquals(LocalDate.of(2026, 10, 27), touches.single().date)
    }

    @Test
    fun threeSecondsOnScreenCompletesTheCard() {
        val s = t.show("a1", ViewSource.FEED, 0)
        val c = t.checkpoint(3_000)
        val event = merge(s + c)!!
        assertTrue(event.completed)
        assertEquals(3_000L, event.dwellMs)
        assertFalse(event.skipped)
        assertEquals(ViewSource.FEED, event.source)
    }

    @Test
    fun twoSecondsAtACheckpointIsNotComplete() {
        val s = t.show("a1", ViewSource.FEED, 0)
        val c = t.checkpoint(2_000)
        val event = merge(s + c)!!
        assertFalse(event.completed)
    }

    @Test
    fun aQuickSwipeIsSkipped() {
        val s = t.show("a1", ViewSource.FEED, 0)
        val l = t.leave(1_000)
        val event = merge(s + l)!!
        assertFalse(event.completed)
        assertTrue(event.skipped)
    }

    @Test
    fun aSwipeAtExactlyThreeSecondsIsComplete() {
        val s = t.show("a1", ViewSource.FEED, 0)
        val l = t.leave(3_000)
        val event = merge(s + l)!!
        assertTrue(event.completed)
        assertFalse(event.skipped)
    }

    @Test
    fun aSwipeAtOneAndAHalfSecondsIsNotSkipped() {
        val s = t.show("a1", ViewSource.FEED, 0)
        val l = t.leave(1_500)
        val event = merge(s + l)!!
        assertFalse(event.completed)
        assertFalse(event.skipped)
    }

    @Test
    fun bonusSourceIsKept() {
        val s = t.show("c2", ViewSource.BONUS, 0)
        val c = t.checkpoint(3_000)
        val event = merge(s + c)!!
        assertEquals(ViewSource.BONUS, event.source)
        assertTrue(event.completed)
    }
}
