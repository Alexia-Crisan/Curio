package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ViewEventRecorderTest {
    private val day = LocalDate.of(2026, 10, 26)

    private fun touch(
        source: ViewSource = ViewSource.FEED, now: Long = 1_000, dwell: Long = 0,
        detail: Long = 0, complete: Boolean = false, left: Boolean = false,
    ) = ViewTouch("discovery_001", day, source, now, dwell, detail, complete, left)

    @Test fun firstTouchCreatesRow() {
        val e = ViewEventRecorder.merge(null, touch(now = 5_000, dwell = 500))
        assertEquals(5_000, e.firstViewedAt)
        assertEquals(500, e.dwellMs)
        assertFalse(e.completed)
    }

    @Test fun dwellAccumulatesAcrossTouches() {
        val a = ViewEventRecorder.merge(null, touch(dwell = 1_000))
        val b = ViewEventRecorder.merge(a, touch(now = 9_000, dwell = 2_500))
        assertEquals(3_500, b.dwellMs)
        assertEquals(1_000, b.firstViewedAt)        // first view time never moves
        assertTrue(b.completed)
    }

    @Test fun threeSecondBoundaryIsExact() {
        assertFalse(ViewEventRecorder.merge(null, touch(dwell = 2_999)).completed)
        assertTrue(ViewEventRecorder.merge(null, touch(dwell = 3_000)).completed)
    }

    @Test fun saveOrDetailOpenCompletesImmediately() =
        assertTrue(ViewEventRecorder.merge(null, touch(complete = true)).completed)

    @Test fun completionIsSticky() {
        val done = ViewEventRecorder.merge(null, touch(complete = true))
        assertTrue(ViewEventRecorder.merge(done, touch(now = 2_000)).completed)
    }

    @Test fun feedBeatsDetailInEitherOrder() {
        val feedFirst = ViewEventRecorder.merge(ViewEventRecorder.merge(null, touch(ViewSource.FEED)), touch(ViewSource.DETAIL))
        val detailFirst = ViewEventRecorder.merge(ViewEventRecorder.merge(null, touch(ViewSource.DETAIL)), touch(ViewSource.FEED))
        assertEquals(ViewSource.FEED, feedFirst.source)
        assertEquals(ViewSource.FEED, detailFirst.source)
    }

    @Test fun bonusBeatsDetail() {
        val e = ViewEventRecorder.merge(ViewEventRecorder.merge(null, touch(ViewSource.DETAIL)), touch(ViewSource.BONUS))
        assertEquals(ViewSource.BONUS, e.source)
    }

    @Test fun quickSwipeAwayIsSkipped() =
        assertTrue(ViewEventRecorder.merge(null, touch(dwell = 400, left = true)).skipped)

    @Test fun bonusRowInsertedAtRevealIsNotSkipped() =
        assertFalse(ViewEventRecorder.merge(null, touch(ViewSource.BONUS, dwell = 0)).skipped)   // pageLeft = false

    @Test fun completedCardIsNeverSkipped() =
        assertFalse(ViewEventRecorder.merge(null, touch(dwell = 400, complete = true, left = true)).skipped)

    @Test fun laterCompletionClearsEarlierSkip() {
        val skipped = ViewEventRecorder.merge(null, touch(dwell = 400, left = true))
        val later = ViewEventRecorder.merge(skipped, touch(dwell = 3_000, left = true))
        assertFalse(later.skipped)
        assertTrue(later.completed)
    }

    @Test fun detailTimeIsTrackedSeparatelyFromCardTime() {
        val a = ViewEventRecorder.merge(null, touch(ViewSource.DETAIL, detail = 4_000, complete = true))
        val b = ViewEventRecorder.merge(a, touch(ViewSource.DETAIL, detail = 5_000))
        assertEquals(9_000, b.detailDwellMs)
        assertEquals(0, b.dwellMs)
    }
}
