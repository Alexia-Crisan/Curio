package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DetailReadingTrackerTest {
    private val day = LocalDate.of(2026, 10, 26)

    private fun tracker(id: String = "a6") = DetailReadingTracker(id, day)

    private fun fold(touches: List<ViewTouch>, existing: ViewEvent? = null): ViewEvent {
        var event = existing
        touches.forEach { event = ViewEventRecorder.merge(event, it) }
        return event!!
    }

    private fun dwells(touches: List<ViewTouch>) = touches.map { it.detailDwellDeltaMs }

    private fun ev(
        id: String,
        date: LocalDate = day,
        completed: Boolean = true,
        source: ViewSource = ViewSource.FEED,
    ) = ViewEvent(
        discoveryId = id,
        date = date,
        firstViewedAt = 1_000,
        dwellMs = 3_000,
        completed = completed,
        skipped = false,
        source = source,
        detailDwellMs = 0,
    )

    @Test fun openCompletesTheCardOnceWithSourceDetail() {
        val t = tracker()
        val first = t.open(1_000)
        assertEquals(1, first.size)
        assertEquals(ViewSource.DETAIL, first[0].source)
        assertTrue(first[0].markCompleted)
        assertEquals(0L, first[0].detailDwellDeltaMs)
        assertEquals(0L, first[0].dwellDeltaMs)
        assertTrue(t.open(2_000).isEmpty())
    }

    @Test fun hiddenEmitsTheVisibleTime() {
        val t = tracker()
        t.visible(10_000)
        assertEquals(listOf(2_500L), dwells(t.hidden(12_500)))
    }

    @Test fun checkpointEmitsAndRestartsTheInterval() {
        val t = tracker()
        t.visible(10_000)
        assertEquals(listOf(3_000L), dwells(t.checkpoint(13_000)))
        assertEquals(listOf(3_000L), dwells(t.checkpoint(16_000)))
        assertEquals(listOf(500L), dwells(t.hidden(16_500)))
    }

    @Test fun hiddenOrCheckpointWithoutVisibleEmitNothing() {
        val t = tracker()
        assertTrue(t.hidden(5_000).isEmpty())
        assertTrue(t.checkpoint(5_000).isEmpty())
    }

    @Test fun hiddenTwiceEmitsOnlyOnce() {
        val t = tracker()
        t.visible(0)
        assertEquals(1, t.hidden(4_000).size)
        assertTrue(t.hidden(9_000).isEmpty())
    }

    @Test fun visibleTwiceKeepsTheFirstMark() {
        val t = tracker()
        t.visible(1_000)
        t.visible(5_000)
        assertEquals(listOf(4_000L), dwells(t.hidden(5_000)))
    }

    @Test fun zeroDeltaEmitsNothing() {
        val t = tracker()
        t.visible(7_000)
        assertTrue(t.checkpoint(7_000).isEmpty())
        assertTrue(t.hidden(7_000).isEmpty())
    }

    @Test fun aClockGoingBackwardsEmitsNothingAndResynchronises() {
        val t = tracker()
        t.visible(10_000)
        assertTrue(t.checkpoint(9_000).isEmpty())
        assertEquals(listOf(1_000L), dwells(t.hidden(10_000)))
    }

    @Test fun oneIntervalIsCappedAtTheMaximum() {
        val t = tracker()
        t.visible(0)
        assertEquals(listOf(DetailReadingTracker.MAX_DELTA_MS), dwells(t.hidden(1_000_000)))
    }

    @Test fun backgroundingStopsTheClockAndResumingStartsANewInterval() {
        val t = tracker()
        t.visible(10_000)
        assertEquals(listOf(2_000L), dwells(t.hidden(12_000)))
        t.visible(60_000)
        assertEquals(listOf(1_000L), dwells(t.hidden(61_000)))
    }

    @Test fun everyTouchCarriesTheNowOfTheCall() {
        val t = tracker()
        t.visible(10_000)
        val touch = t.checkpoint(13_000)[0]
        assertEquals(13_000L, touch.nowMs)
        assertEquals("a6", touch.discoveryId)
        assertEquals(day, touch.date)
    }

    @Test fun aDetailVisitNeverBeatsAnExistingFeedRow() {
        val feedRow = ev("a6", source = ViewSource.FEED, completed = true)
        val t = tracker()
        t.visible(10_000)
        val merged = fold(t.open(10_000) + t.hidden(15_000), existing = feedRow)
        assertEquals(ViewSource.FEED, merged.source)
        assertTrue(merged.completed)
        assertEquals(5_000L, merged.detailDwellMs)
    }

    @Test fun aDetailVisitAloneCompletesTheCardAsDetail() {
        val t = tracker("a1")
        val merged = fold(t.open(1_000))
        assertEquals(ViewSource.DETAIL, merged.source)
        assertTrue(merged.completed)
        assertFalse(merged.skipped)
        assertEquals(0L, merged.detailDwellMs)
    }

    @Test fun detailTimeAddsUpAcrossCheckpointsAndVisits() {
        val t = tracker()
        t.visible(0)
        val firstVisit = t.open(0) + t.checkpoint(3_000) + t.checkpoint(6_000) + t.hidden(7_000)
        t.visible(100_000)
        val secondVisit = t.hidden(101_000)
        assertEquals(8_000L, fold(firstVisit + secondVisit).detailDwellMs)
    }

    private fun snapshotAfter(id: String, vararg segments: Long) =
        ActivitySnapshotBuilder.build(
            listOf(fold(segments.map { ViewTouch(id, day, ViewSource.DETAIL, 1, detailDwellDeltaMs = it) })),
            emptySet(), emptyList(), TestContent.index,
        )

    @Test fun eightSecondsOnAKnowledgeCardIsADeepRead() {
        assertTrue("a6" in snapshotAfter("a6", 3_000, 3_000, 2_000).deepReadIds)
    }

    @Test fun sevenSecondsIsNot() {
        assertFalse("a6" in snapshotAfter("a6", 3_000, 3_000, 1_999).deepReadIds)
    }

    @Test fun aFactCardNeverCountsHoweverLongItIsRead() {
        assertFalse("a1" in snapshotAfter("a1", 60_000).deepReadIds)
    }

    @Test fun fiveDeepReadsUnlockTheDeepLearnerBadge() {
        val events = listOf("a6", "b6", "c6", "d6", "s6").map { id ->
            val t = DetailReadingTracker(id, day)
            t.visible(0)
            fold(t.open(0) + t.hidden(8_000))
        }
        val snapshot = ActivitySnapshotBuilder.build(events, emptySet(), emptyList(), TestContent.index)
        val evaluation = AchievementEvaluator.evaluate(snapshot, TestContent.index)
        assertEquals(5, evaluation.progress.first { it.def.id == AchievementId.DEEP_LEARNER }.current)
        assertTrue(AchievementId.DEEP_LEARNER in evaluation.newlyEarned.map { it.id })
    }

    @Test fun fourDeepReadsDoNot() {
        val events = listOf("a6", "b6", "c6", "d6").map { id ->
            val t = DetailReadingTracker(id, day)
            t.visible(0)
            fold(t.open(0) + t.hidden(8_000))
        }
        val snapshot = ActivitySnapshotBuilder.build(events, emptySet(), emptyList(), TestContent.index)
        val evaluation = AchievementEvaluator.evaluate(snapshot, TestContent.index)
        assertFalse(AchievementId.DEEP_LEARNER in evaluation.newlyEarned.map { it.id })
    }
}
