package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TopicSelectorTest {
    private val ids = (0..6).map { "t$it" }

    private fun d(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)

    @Test fun guidedFirstOfMonthIsFirstTopic() {
        assertEquals("t0", TopicSelector.guided(d(2026, 10, 1), ids))
    }

    @Test fun guidedCyclesEverySevenDays() {
        assertEquals("t6", TopicSelector.guided(d(2026, 10, 7), ids))
        assertEquals("t0", TopicSelector.guided(d(2026, 10, 8), ids))
    }

    @Test fun guidedDay31() {
        assertEquals("t2", TopicSelector.guided(d(2026, 10, 31), ids))
    }

    @Test fun guidedFebruary28() {
        assertEquals("t6", TopicSelector.guided(d(2027, 2, 28), ids))
    }

    @Test fun guidedLeapDay() {
        assertEquals("t0", TopicSelector.guided(d(2028, 2, 29), ids))
    }

    @Test fun guidedDependsOnlyOnDayOfMonth() {
        assertEquals(TopicSelector.guided(d(2026, 10, 5), ids), TopicSelector.guided(d(2026, 11, 5), ids))
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
