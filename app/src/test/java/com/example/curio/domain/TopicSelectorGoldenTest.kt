package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TopicSelectorGoldenTest {
    private val ids5 = listOf("t0", "t1", "t2", "t3", "t4")
    private val ids7 = listOf("t0", "t1", "t2", "t3", "t4", "t5", "t6")

    private val dates = listOf(
        LocalDate.of(2026, 10, 26),
        LocalDate.of(2026, 10, 27),
        LocalDate.of(2026, 10, 28),
        LocalDate.of(2026, 10, 31),
        LocalDate.of(2026, 11, 1),
        LocalDate.of(2026, 11, 30),
        LocalDate.of(2028, 2, 29),
    )

    @Test
    fun guidedFiveTopics() {
        val actual = dates.map { TopicSelector.guided(it, ids5) }
        assertEquals(listOf("t0", "t1", "t2", "t0", "t0", "t4", "t3"), actual)
    }

    @Test
    fun guidedSevenTopics() {
        val actual = dates.map { TopicSelector.guided(it, ids7) }
        assertEquals(listOf("t4", "t5", "t6", "t2", "t0", "t1", "t0"), actual)
    }

    @Test
    fun surpriseScienceFive() {
        val actual = dates.map { TopicSelector.surprise(it, "science", ids5) }
        assertEquals(listOf("t0", "t3", "t2", "t3", "t3", "t4", "t2"), actual)
    }

    @Test
    fun surpriseScienceSeven() {
        val actual = dates.map { TopicSelector.surprise(it, "science", ids7) }
        assertEquals(listOf("t0", "t2", "t3", "t6", "t6", "t0", "t4"), actual)
    }

    @Test
    fun surpriseHistoryFive() {
        val actual = dates.map { TopicSelector.surprise(it, "history", ids5) }
        assertEquals(listOf("t1", "t1", "t2", "t2", "t2", "t4", "t3"), actual)
    }

    @Test
    fun surpriseHistorySeven() {
        val actual = dates.map { TopicSelector.surprise(it, "history", ids7) }
        assertEquals(listOf("t1", "t4", "t0", "t1", "t1", "t4", "t0"), actual)
    }
}
