package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivitySnapshotBuilderTest {
    private val c = TestContent.index

    private fun build(
        events: List<ViewEvent> = emptyList(),
        saved: Set<String> = emptySet(),
        choices: List<DayChoice> = emptyList(),
    ) = ActivitySnapshotBuilder.build(events, saved, choices, c)

    @Test fun emptyHistoryGivesAnEmptySnapshot() {
        val s = build()
        assertTrue(s.completedIds.isEmpty())
        assertTrue(s.completedFeedDays.isEmpty())
        assertTrue(s.savedIds.isEmpty())
        assertTrue(s.deepReadIds.isEmpty())
        assertTrue(s.bonusCompletedIds.isEmpty())
        assertTrue(s.completedDays.isEmpty())
    }

    @Test fun completedIdsAreDistinctAcrossDates() {
        val events = listOf(
            ev("a1", DAY, at = 1000),
            ev("a1", DAY.plusDays(1), at = 2000),
            ev("a2", DAY, at = 3000),
        )
        val s = build(events)
        assertEquals(setOf("a1", "a2"), s.completedIds)
    }

    @Test fun rowsThatWereNotCompletedAreIgnored() {
        val s = build(listOf(ev("a1", completed = false)))
        assertTrue(s.completedIds.isEmpty())
    }

    @Test fun unknownDiscoveryIdsAreIgnored() {
        val s = build(listOf(ev("ghost")))
        assertTrue(s.completedIds.isEmpty())
    }

    @Test fun activeDaysNeedACompletedFeedRow() {
        val events = listOf(
            ev("a1", DAY, source = ViewSource.FEED),
            ev("a2", DAY.plusDays(1), source = ViewSource.DETAIL),
            ev("a3", DAY.plusDays(2), source = ViewSource.BONUS),
            ev("a4", DAY.plusDays(3), source = ViewSource.FEED, completed = false),
        )
        val s = build(events)
        assertEquals(setOf(DAY), s.completedFeedDays)
    }

    @Test fun deepReadNeedsEightSecondsOfDetailOnAKnowledgeCard() {
        val events = listOf(
            ev("a6", detail = 8000),
            ev("b6", detail = 7999),
            ev("a5", detail = 20000),
        )
        val s = build(events)
        assertEquals(setOf("a6"), s.deepReadIds)
    }

    @Test fun detailTimeIsSummedAcrossDays() {
        val events = listOf(
            ev("c6", DAY, detail = 5000),
            ev("c6", DAY.plusDays(1), detail = 3000),
        )
        val s = build(events)
        assertEquals(setOf("c6"), s.deepReadIds)
    }

    @Test fun aFirstTimeBonusCompletionCounts() {
        val s = build(listOf(ev("b1", source = ViewSource.BONUS)))
        assertEquals(setOf("b1"), s.bonusCompletedIds)
    }

    @Test fun aBonusThatWasNeverCompletedDoesNotCount() {
        val s = build(listOf(ev("b1", completed = false, source = ViewSource.BONUS)))
        assertTrue(s.bonusCompletedIds.isEmpty())
    }

    @Test fun aRevisitedBonusDoesNotCount() {
        val events = listOf(
            ev("b1", DAY.minusDays(3), source = ViewSource.FEED),
            ev("b1", DAY, source = ViewSource.BONUS),
        )
        val s = build(events)
        assertTrue(s.bonusCompletedIds.isEmpty())
    }

    @Test fun aBonusLaterSeenInTheFeedStillCounts() {
        val events = listOf(
            ev("b1", DAY, source = ViewSource.BONUS),
            ev("b1", DAY.plusDays(7), source = ViewSource.FEED),
        )
        val s = build(events)
        assertEquals(setOf("b1"), s.bonusCompletedIds)
    }

    @Test fun completedDaysUseTopicMembership() {
        val choices = listOf(
            DayChoice(DAY, "animals", "t1", DailyMode.GUIDED),
            DayChoice(DAY.plusDays(1), "animals", "t1", DailyMode.GUIDED),
        )
        val events = idsOf("a").map { ev(it, DAY) } +
                idsOf("a", 1..4).map { ev(it, DAY.plusDays(1)) } +
                listOf(ev("b1", DAY.plusDays(1), source = ViewSource.BONUS), ev("b2", DAY.plusDays(1), source = ViewSource.BONUS))
        val s = build(events, choices = choices)
        assertEquals(setOf(DAY), s.completedDays)
    }

    @Test fun savedIdsAreLimitedToKnownContent() {
        val s = build(saved = setOf("a1", "ghost"))
        assertEquals(setOf("a1"), s.savedIds)
    }

    @Test fun firstCompletionTimeIsTheEarliestRow() {
        val events = listOf(
            ev("a1", DAY, at = 5000),
            ev("a1", DAY.plusDays(1), at = 2000),
        )
        val s = build(events)
        assertEquals(2000L, s.firstCompletedAt["a1"])
    }
}
