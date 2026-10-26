package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TreeTeaserTest {
    private val D = LocalDate.of(2026, 10, 26)

    private fun tree(done: Set<String> = emptySet()) =
        TreeBuilder.build(TestContent.index.themes[0], TestContent.index, snapshotOf(completed = done))

    @Test
    fun guidedPicksTheNextDaysTopic() {
        val next = TreeTeaser.nextVisit(tree(), DailyMode.GUIDED, D)
        assertNotNull(next)
        assertEquals(LocalDate.of(2026, 10, 27), next!!.date)
        assertEquals("t3", next.topicId)
    }

    @Test
    fun guidedSkipsACompletedTopic() {
        val done = idsOf("c", 1..6).toSet()
        val next = TreeTeaser.nextVisit(tree(done), DailyMode.GUIDED, D)
        assertNotNull(next)
        assertEquals(LocalDate.of(2026, 10, 28), next!!.date)
        assertEquals("t4", next.topicId)
    }

    @Test
    fun guidedSkipsSeveralCompletedTopics() {
        val done = (idsOf("c", 1..6) + idsOf("d", 1..6)).toSet()
        val next = TreeTeaser.nextVisit(tree(done), DailyMode.GUIDED, D)
        assertNotNull(next)
        assertEquals(LocalDate.of(2026, 10, 29), next!!.date)
        assertEquals("t1", next.topicId)
    }

    @Test
    fun allTopicsCompleteGivesNull() {
        val allCards = (idsOf("a", 1..6) + idsOf("b", 1..6) + idsOf("c", 1..6) + idsOf("d", 1..6)).toSet()
        assertNull(TreeTeaser.nextVisit(tree(allCards), DailyMode.GUIDED, D))
    }

    @Test
    fun chooseModeGivesNull() {
        assertNull(TreeTeaser.nextVisit(tree(), DailyMode.CHOOSE, D))
    }

    @Test
    fun surpriseUsesTheSelector() {
        val next = TreeTeaser.nextVisit(tree(), DailyMode.SURPRISE, D)
        assertNotNull(next)
        assertEquals(LocalDate.of(2026, 10, 27), next!!.date)
        val expectedTopic = TopicSelector.surprise(LocalDate.of(2026, 10, 27), "animals", listOf("t1", "t2", "t3", "t4"))
        assertEquals(expectedTopic, next.topicId)
    }

    @Test
    fun surpriseSkipsACompletedTopic() {
        val nextDate = LocalDate.of(2026, 10, 27)
        val firstTopic = TopicSelector.surprise(nextDate, "animals", listOf("t1", "t2", "t3", "t4"))!!
        val prefix = mapOf("t1" to "a", "t2" to "b", "t3" to "c", "t4" to "d").getValue(firstTopic)
        val done = idsOf(prefix, 1..6).toSet()

        val next = TreeTeaser.nextVisit(tree(done), DailyMode.SURPRISE, D)
        assertNotNull(next)
        assertTrue(next!!.date > nextDate)
        val expectedTopic = TopicSelector.surprise(next.date, "animals", listOf("t1", "t2", "t3", "t4"))
        assertEquals(expectedTopic, next.topicId)
    }

    @Test
    fun theHorizonLimitsTheSearch() {
        val done = idsOf("c", 1..6).toSet()
        assertNull(TreeTeaser.nextVisit(tree(done), DailyMode.GUIDED, D, horizonDays = 1))
    }

    @Test
    fun theMonthEndRollsOver() {
        val oct31 = LocalDate.of(2026, 10, 31)
        val next = TreeTeaser.nextVisit(tree(), DailyMode.GUIDED, oct31)
        assertNotNull(next)
        assertEquals(LocalDate.of(2026, 11, 1), next!!.date)
        assertEquals("t1", next.topicId)
    }

    @Test
    fun theDateAfterIsExcluded() {
        val next = TreeTeaser.nextVisit(tree(), DailyMode.GUIDED, D)
        assertNotNull(next)
        assertEquals(LocalDate.of(2026, 10, 27), next!!.date)
    }

    @Test
    fun aThemeWithoutBranchesGivesNull() {
        val emptyTree = ThemeTree(TestContent.index.themes[2], GrowthStage.SEED, 0, 0, emptyList())
        assertNull(TreeTeaser.nextVisit(emptyTree, DailyMode.GUIDED, D))
    }
}
