package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TopicSelectorAvoidTest {
    private val ids5 = listOf("t0", "t1", "t2", "t3", "t4")
    private val ids7 = listOf("t0", "t1", "t2", "t3", "t4", "t5", "t6")
    private val D1 = LocalDate.of(2026, 10, 26)
    private val D2 = LocalDate.of(2026, 10, 27)
    private val D3 = LocalDate.of(2026, 10, 28)
    private val D4 = LocalDate.of(2026, 10, 31)
    private val D5 = LocalDate.of(2026, 11, 1)
    private val D6 = LocalDate.of(2026, 11, 30)
    private val D7 = LocalDate.of(2028, 2, 29)
    private val dates = listOf(D1, D2, D3, D4, D5, D6, D7)

    private fun raw(d: LocalDate, theme: String = "science", ids: List<String> = ids5) =
        TopicSelector.surprise(d, theme, ids)

    @Test
    fun nullAvoidEqualsTheOldValues() {
        val expected = listOf("t0", "t3", "t2", "t3", "t3", "t4", "t2")
        val actual = dates.map { TopicSelector.surprise(it, "science", ids5, null) }
        assertEquals(expected, actual)
    }

    @Test
    fun nullAvoidEqualsTheThreeArgumentCall() {
        dates.forEach { d ->
            listOf("science", "history").forEach { th ->
                listOf(ids5, ids7).forEach { ids ->
                    assertEquals(TopicSelector.surprise(d, th, ids), TopicSelector.surprise(d, th, ids, null))
                }
            }
        }
    }

    @Test
    fun avoidingAnotherTopicChangesNothing() {
        dates.forEach { d ->
            val r = raw(d)
            val avoid = ids5.first { it != r }
            assertEquals(r, TopicSelector.surprise(d, "science", ids5, avoid))
        }
    }

    @Test
    fun avoidingTheRawPickGivesAnotherTopic() {
        dates.forEach { d ->
            val r = raw(d)
            val res = TopicSelector.surprise(d, "science", ids5, r)
            assertNotEquals(r, res)
        }
    }

    @Test
    fun redrawScienceFive() {
        val expected = listOf("t3", "t0", "t4", "t1", "t0", "t1", "t1")
        val actual = dates.map { d -> TopicSelector.surprise(d, "science", ids5, raw(d)) }
        assertEquals(expected, actual)
    }

    @Test
    fun redrawScienceSeven() {
        val expected = listOf("t5", "t4", "t1", "t2", "t2", "t5", "t3")
        val actual = dates.map { d -> TopicSelector.surprise(d, "science", ids7, TopicSelector.surprise(d, "science", ids7)) }
        assertEquals(expected, actual)
    }

    @Test
    fun redrawHistoryFive() {
        val expected = listOf("t0", "t0", "t0", "t1", "t1", "t0", "t0")
        val actual = dates.map { d -> TopicSelector.surprise(d, "history", ids5, TopicSelector.surprise(d, "history", ids5)) }
        assertEquals(expected, actual)
    }

    @Test
    fun redrawHistorySeven() {
        val expected = listOf("t0", "t0", "t6", "t4", "t3", "t1", "t6")
        val actual = dates.map { d -> TopicSelector.surprise(d, "history", ids7, TopicSelector.surprise(d, "history", ids7)) }
        assertEquals(expected, actual)
    }

    @Test
    fun theAvoidedTopicIsNeverReturned() {
        var curr = D1
        for (i in 1..731) {
            ids7.forEach { avoid ->
                val res = TopicSelector.surprise(curr, "science", ids7, avoid)
                assertNotEquals(avoid, res)
            }
            curr = curr.plusDays(1)
        }
    }

    @Test
    fun theResultIsAlwaysAKnownTopic() {
        var curr = D1
        for (i in 1..100) {
            ids7.forEach { avoid ->
                val res = TopicSelector.surprise(curr, "science", ids7, avoid)
                assertTrue(res in ids7)
            }
            curr = curr.plusDays(1)
        }
    }

    @Test
    fun aSingleTopicIgnoresTheAvoid() {
        assertEquals("t0", TopicSelector.surprise(D1, "science", listOf("t0"), "t0"))
    }

    @Test
    fun twoTopicsAlternateWhenAvoided() {
        val ids = listOf("t0", "t1")
        val r = TopicSelector.surprise(D1, "science", ids)
        val res = TopicSelector.surprise(D1, "science", ids, r)
        assertNotEquals(r, res)
        assertTrue(res in ids)
    }

    @Test
    fun anUnknownAvoidIsIgnored() {
        assertEquals(raw(D1), TopicSelector.surprise(D1, "science", ids5, "zzz"))
    }

    @Test
    fun theRedrawIsDeterministic() {
        val r = raw(D1)
        val first = TopicSelector.surprise(D1, "science", ids5, r)
        for (i in 1..100) {
            assertEquals(first, TopicSelector.surprise(D1, "science", ids5, r))
        }
    }

    @Test
    fun theRedrawUsesSeveralTopicsOverTime() {
        val distinct = (0..199).map { offset ->
            val d = D1.plusDays(offset.toLong())
            TopicSelector.surprise(d, "science", ids5, raw(d))
        }.toSet()
        assertTrue(distinct.size >= 3)
    }

    @Test
    fun anEmptyListIsStillNull() {
        assertEquals(null, TopicSelector.surprise(D1, "science", emptyList(), "t0"))
    }

    @Test
    fun selectPassesTheAvoidOnlyToSurprise() {
        val r = raw(D1)
        assertEquals(TopicSelector.surprise(D1, "science", ids5, r), TopicSelector.select(DailyMode.SURPRISE, D1, "science", ids5, null, avoidTopicId = r))
        assertEquals(TopicSelector.guided(D1, ids5), TopicSelector.select(DailyMode.GUIDED, D1, "science", ids5, null, "t0"))
        assertEquals("t2", TopicSelector.select(DailyMode.CHOOSE, D1, "science", ids5, "t2", "t2"))
    }

    @Test
    fun theSwitchIsOn() {
        assertTrue(TopicConfig.SURPRISE_AVOIDS_YESTERDAY)
    }
}
