package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TreeGrowthTest {
    private val D = LocalDate.of(2026, 10, 26)
    private val zone = ZoneId.of("Europe/Bucharest")

    private fun tree(done: Set<String>, at: Map<String, Long> = emptyMap()) =
        TreeBuilder.build(TestContent.index.themes[0], TestContent.index, snapshotOf(completed = done, firstAt = at))

    @Test
    fun noBaselineMeansNoGrowth() {
        val t = tree(setOf("a1", "a2"), mapOf("a1" to 100L, "a2" to 200L))
        val summary = TreeGrowth.since(t, null)
        assertEquals(0, summary.newLeafCount)
        assertTrue(summary.grownTopicIds.isEmpty())
        assertFalse(summary.stageChanged)
    }

    @Test
    fun leavesAfterTheBaselineAreNew() {
        val t = tree(setOf("a1", "a2", "a3"), mapOf("a1" to 100L, "a2" to 200L, "a3" to 300L))
        val summary = TreeGrowth.since(t, 150L)
        assertEquals(setOf("a2", "a3"), summary.newLeafIds)
        assertEquals(setOf("t1"), summary.grownTopicIds)
    }

    @Test
    fun aLeafExactlyAtTheBaselineIsNotNew() {
        val t = tree(setOf("a1", "a2", "a3"), mapOf("a1" to 100L, "a2" to 200L, "a3" to 300L))
        val summary = TreeGrowth.since(t, 200L)
        assertEquals(setOf("a3"), summary.newLeafIds)
    }

    @Test
    fun aLeafWithoutATimestampIsNeverNew() {
        val t = tree(setOf("a1"), emptyMap())
        val summary = TreeGrowth.since(t, 0L)
        assertEquals(0, summary.newLeafCount)
    }

    @Test
    fun theStageChangeIsDetected() {
        val t = tree(setOf("a1", "a2", "a3"), mapOf("a1" to 300L, "a2" to 300L, "a3" to 300L))
        val summary = TreeGrowth.since(t, 200L)
        assertEquals(GrowthStage.SEED, summary.stageBefore)
        assertEquals(GrowthStage.SAPLING, summary.stageAfter)
        assertTrue(summary.stageChanged)
    }

    @Test
    fun noStageChangeWhenTheStageHolds() {
        val t = tree(setOf("a1", "a2"), mapOf("a1" to 100L, "a2" to 300L))
        val summary = TreeGrowth.since(t, 200L)
        assertEquals(GrowthStage.SPROUT, summary.stageBefore)
        assertEquals(GrowthStage.SPROUT, summary.stageAfter)
        assertFalse(summary.stageChanged)
    }

    @Test
    fun onlyOldLeavesCountForTheStageBefore() {
        val t = tree(setOf("a1", "a2", "a3"), mapOf("a1" to 100L, "a2" to 300L, "a3" to 300L))
        val summary = TreeGrowth.since(t, 200L)
        assertEquals(GrowthStage.SPROUT, summary.stageBefore)
        assertEquals(GrowthStage.SAPLING, summary.stageAfter)
    }

    @Test
    fun grownTopicsAreListedOnce() {
        val t = tree(setOf("a1", "a2", "b1"), mapOf("a1" to 300L, "a2" to 300L, "b1" to 300L))
        val summary = TreeGrowth.since(t, 200L)
        assertEquals(setOf("t1", "t2"), summary.grownTopicIds)
    }

    @Test
    fun nothingNewMeansNoChange() {
        val t = tree(setOf("a1"), mapOf("a1" to 100L))
        val summary = TreeGrowth.since(t, 200L)
        assertEquals(0, summary.newLeafCount)
        assertFalse(summary.stageChanged)
    }

    @Test
    fun onDayCountsTheLocalDay() {
        val start = D.atStartOfDay(zone).toInstant().toEpochMilli()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val at = mapOf("a1" to start, "a2" to start + oneDayMs - 1, "a3" to start + oneDayMs, "a4" to start - 1)
        val t = tree(setOf("a1", "a2", "a3", "a4"), at)
        val summary = TreeGrowth.on(t, D, zone)
        assertEquals(setOf("a1", "a2"), summary.newLeafIds)
    }

    @Test
    fun onDayHandlesTheOctoberTimeChange() {
        val day = LocalDate.of(2026, 10, 25)
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val t = tree(setOf("a1", "a2"), mapOf("a1" to end - 1, "a2" to end))
        val summary = TreeGrowth.on(t, day, zone)
        assertEquals(setOf("a1"), summary.newLeafIds)
    }

    @Test
    fun onDayWithNoCompletionsIsEmpty() {
        val summary = TreeGrowth.on(tree(emptySet(), emptyMap()), D, zone)
        assertEquals(0, summary.newLeafCount)
    }
}
