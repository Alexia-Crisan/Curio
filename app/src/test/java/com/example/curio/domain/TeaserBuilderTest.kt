package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TeaserBuilderTest {
    private val D = LocalDate.of(2026, 10, 26)
    private val zone = ZoneId.of("Europe/Bucharest")
    private val trees = TreeBuilder.buildAll(TestContent.index, snapshotOf())

    @Test
    fun countsTheLeavesOfTheDay() {
        val start = D.atStartOfDay(zone).toInstant().toEpochMilli()
        val snapshot = snapshotOf(completed = setOf("a1", "a2"), firstAt = mapOf("a1" to start, "a2" to start + 1000L))
        val t = TreeBuilder.buildAll(TestContent.index, snapshot)
        val teaser = TeaserBuilder.forFeed(t, "animals", DailyMode.GUIDED, D, zone)
        assertNotNull(teaser)
        assertEquals(2, teaser!!.leavesToday)
        assertEquals(GrowthStage.SPROUT, teaser.stageNow)
    }

    @Test
    fun aStageChangeTodayIsReported() {
        val start = D.atStartOfDay(zone).toInstant().toEpochMilli()
        val at = mapOf("a1" to start, "a2" to start + 100L, "a3" to start + 200L)
        val snapshot = snapshotOf(completed = setOf("a1", "a2", "a3"), firstAt = at)
        val t = TreeBuilder.buildAll(TestContent.index, snapshot)
        val teaser = TeaserBuilder.forFeed(t, "animals", DailyMode.GUIDED, D, zone)
        assertNotNull(teaser)
        assertTrue(teaser!!.stageChangedToday)
        assertEquals(GrowthStage.SAPLING, teaser.stageNow)
    }

    @Test
    fun yesterdaysLeavesDoNotCount() {
        val start = D.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val snapshot = snapshotOf(completed = setOf("a1"), firstAt = mapOf("a1" to start))
        val t = TreeBuilder.buildAll(TestContent.index, snapshot)
        val teaser = TeaserBuilder.forFeed(t, "animals", DailyMode.GUIDED, D, zone)
        assertNotNull(teaser)
        assertEquals(0, teaser!!.leavesToday)
    }

    @Test
    fun carriesTheNextVisit() {
        val teaser = TeaserBuilder.forFeed(trees, "animals", DailyMode.GUIDED, D, zone)
        assertNotNull(teaser)
        assertEquals(TreeTeaser.nextVisit(trees[0], DailyMode.GUIDED, D), teaser!!.next)
    }

    @Test
    fun hasContentWhenOnlyTheNextVisitExists() {
        val teaser = TeaserBuilder.forFeed(trees, "animals", DailyMode.GUIDED, D, zone)
        assertNotNull(teaser)
        assertTrue(teaser!!.hasContent)
    }

    @Test
    fun hasNoContentWhenEverythingIsDoneAndNothingHappenedToday() {
        val allCards = (idsOf("a", 1..6) + idsOf("b", 1..6) + idsOf("c", 1..6) + idsOf("d", 1..6)).toSet()
        val snapshot = snapshotOf(completed = allCards, firstAt = allCards.associateWith { 100L })
        val t = TreeBuilder.buildAll(TestContent.index, snapshot)
        val teaser = TeaserBuilder.forFeed(t, "animals", DailyMode.GUIDED, D, zone)
        assertNotNull(teaser)
        assertFalse(teaser!!.hasContent)
    }

    @Test
    fun anUnknownThemeGivesNull() {
        assertNull(TeaserBuilder.forFeed(trees, "oceans", DailyMode.GUIDED, D, zone))
    }

    @Test
    fun chooseModeStillReportsTodaysLeaves() {
        val start = D.atStartOfDay(zone).toInstant().toEpochMilli()
        val snapshot = snapshotOf(completed = setOf("a1"), firstAt = mapOf("a1" to start))
        val t = TreeBuilder.buildAll(TestContent.index, snapshot)
        val teaser = TeaserBuilder.forFeed(t, "animals", DailyMode.CHOOSE, D, zone)
        assertNotNull(teaser)
        assertEquals(1, teaser!!.leavesToday)
        assertNull(teaser.next)
        assertTrue(teaser.hasContent)
    }
}
