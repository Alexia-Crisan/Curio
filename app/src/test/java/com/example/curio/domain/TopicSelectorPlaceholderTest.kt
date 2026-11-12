package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TopicSelectorPlaceholderTest {
    private val date = LocalDate.of(2026, 11, 12)
    private val ids = listOf("moon", "mars", "sun")

    @Test
    fun thePlaceholderSwitchIsOn() {
        assertEquals("mars", TopicConfig.PLACEHOLDER_TOPIC_ID)
    }

    @Test
    fun guidedReturnsThePlaceholderWhenItIsATopic() {
        assertEquals("mars", TopicSelector.select(DailyMode.GUIDED, date, "space", ids, placeholderTopicId = "mars"))
    }

    @Test
    fun surpriseReturnsThePlaceholderWhenItIsATopic() {
        assertEquals("mars", TopicSelector.select(DailyMode.SURPRISE, date, "space", ids, placeholderTopicId = "mars"))
    }

    @Test
    fun thePlaceholderWinsOverTheYesterdayAvoidance() {
        assertEquals("mars", TopicSelector.select(DailyMode.SURPRISE, date, "space", ids, avoidTopicId = "mars", placeholderTopicId = "mars"))
    }

    @Test
    fun chooseModeIsNotOverridden() {
        assertEquals("sun", TopicSelector.select(DailyMode.CHOOSE, date, "space", ids, storedChoice = "sun", placeholderTopicId = "mars"))
        assertNull(TopicSelector.select(DailyMode.CHOOSE, date, "space", ids, storedChoice = null, placeholderTopicId = "mars"))
    }

    @Test
    fun aPlaceholderThatIsNotATopicChangesNothing() {
        val other = listOf("a", "b", "c")
        assertEquals(TopicSelector.guided(date, other), TopicSelector.select(DailyMode.GUIDED, date, "space", other, placeholderTopicId = "mars"))
        assertEquals(TopicSelector.surprise(date, "space", other), TopicSelector.select(DailyMode.SURPRISE, date, "space", other, placeholderTopicId = "mars"))
    }

    @Test
    fun anEmptyPlaceholderChangesNothing() {
        assertEquals(TopicSelector.guided(date, ids), TopicSelector.select(DailyMode.GUIDED, date, "space", ids, placeholderTopicId = ""))
        assertEquals(TopicSelector.surprise(date, "space", ids), TopicSelector.select(DailyMode.SURPRISE, date, "space", ids, placeholderTopicId = ""))
    }

    @Test
    fun anEmptyTopicListStillGivesNull() {
        assertNull(TopicSelector.select(DailyMode.GUIDED, date, "space", emptyList(), placeholderTopicId = "mars"))
    }
}
