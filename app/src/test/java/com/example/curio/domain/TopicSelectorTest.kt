package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TopicSelectorTest {
    private val ids = (0..6).map { "t$it" }

    private fun d(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)

    @Test fun guidedUsesTheEpochDayModuloTheTopicCount() {
        assertEquals("t4", TopicSelector.guided(d(2026, 10, 26), ids))
        assertEquals("t5", TopicSelector.guided(d(2026, 10, 27), ids))
        assertEquals("t6", TopicSelector.guided(d(2026, 10, 28), ids))
        assertEquals("t0", TopicSelector.guided(d(2026, 10, 29), ids))
    }

    @Test fun guidedAdvancesOneTopicPerDay() {
        val start = d(2026, 10, 1)
        repeat(21) { i ->
            val today = ids.indexOf(TopicSelector.guided(start.plusDays(i.toLong()), ids))
            val tomorrow = ids.indexOf(TopicSelector.guided(start.plusDays(i + 1L), ids))
            assertEquals((today + 1) % ids.size, tomorrow)
        }
    }

    @Test fun guidedNeverRepeatsOnConsecutiveDays() {
        val three = listOf("t0", "t1", "t2")
        var day = d(2026, 1, 1)
        repeat(800) {
            assertNotEquals(TopicSelector.guided(day, three), TopicSelector.guided(day.plusDays(1), three))
            day = day.plusDays(1)
        }
    }

    @Test fun guidedContinuesAcrossTheMonthEnd() {
        val three = listOf("t0", "t1", "t2")
        assertNotEquals(TopicSelector.guided(d(2026, 10, 31), three), TopicSelector.guided(d(2026, 11, 1), three))
        assertNotEquals(TopicSelector.guided(d(2026, 12, 31), three), TopicSelector.guided(d(2027, 1, 1), three))
    }

    @Test fun guidedReachesEveryTopicOfALongList() {
        val sixty = (0 until 60).map { "t$it" }
        val used = (0 until 60).map { TopicSelector.guided(d(2026, 10, 1).plusDays(it.toLong()), sixty) }.toSet()
        assertEquals(60, used.size)
    }

    @Test fun guidedRepeatsAfterOneFullCycle() {
        repeat(30) { i ->
            val day = d(2026, 10, 1).plusDays(i.toLong())
            assertEquals(TopicSelector.guided(day, ids), TopicSelector.guided(day.plusDays(ids.size.toLong()), ids))
        }
    }

    @Test fun guidedWorksForDatesBeforeTheEpoch() {
        assertTrue(TopicSelector.guided(d(1960, 1, 1), ids) in ids)
        val before = ids.indexOf(TopicSelector.guided(d(1969, 12, 31), ids))
        val after = ids.indexOf(TopicSelector.guided(d(1970, 1, 1), ids))
        assertEquals((before + 1) % ids.size, after)
    }

    @Test fun guidedEmptyListIsNull() {
        assertNull(TopicSelector.guided(d(2026, 10, 1), emptyList()))
    }

    @Test fun surpriseIsStableForTheSameDay() {
        val first = TopicSelector.surprise(d(2026, 10, 26), "animals", ids)
        repeat(100) {
            assertEquals(first, TopicSelector.surprise(d(2026, 10, 26), "animals", ids))
        }
    }

    @Test fun surpriseAlwaysReturnsAKnownTopic() {
        var startDate = d(2026, 1, 1)
        repeat(731) {
            val result = TopicSelector.surprise(startDate, "animals", ids)
            assertTrue(result in ids)
            startDate = startDate.plusDays(1)
        }
    }

    @Test fun surpriseUsesSeveralTopicsOverAMonth() {
        val topicsUsed = (1..31).mapNotNull {
            TopicSelector.surprise(d(2026, 10, it), "animals", ids)
        }.toSet()
        assertTrue(topicsUsed.size >= 4)
    }

    @Test fun surpriseDiffersBetweenThemesOnSomeDays() {
        var foundDifferent = false
        for (i in 1..60) {
            val date = d(2026, 10, 1).plusDays(i.toLong())
            val a = TopicSelector.surprise(date, "animals", ids)
            val s = TopicSelector.surprise(date, "space", ids)
            if (a != s) {
                foundDifferent = true
                break
            }
        }
        assertTrue(foundDifferent)
    }

    @Test fun surpriseEmptyListIsNull() {
        assertNull(TopicSelector.surprise(d(2026, 10, 1), "animals", emptyList()))
    }

    @Test fun surpriseGoldenValues() {
        val actual = (1..10).map { TopicSelector.surprise(d(2026, 10, it), "animals", ids) }
        val expected = listOf("t5", "t0", "t1", "t0", "t4", "t6", "t6", "t2", "t1", "t2")
        assertEquals(expected, actual)
    }

    @Test fun chooseReturnsThePick() {
        assertEquals("t3", TopicSelector.choose(ids, "t3"))
    }

    @Test fun chooseWithoutPickIsNull() {
        assertNull(TopicSelector.choose(ids, null))
    }

    @Test fun chooseIgnoresAPickFromAnotherTheme() {
        assertNull(TopicSelector.choose(ids, "penguins"))
    }
}
