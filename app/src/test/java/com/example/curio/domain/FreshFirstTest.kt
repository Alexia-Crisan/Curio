package com.example.curio.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshFirstTest {
    private val DAY = java.time.LocalDate.of(2026, 10, 26)
    private val cards = TestContent.discoveries.filter { it.topicId == "t1" }
    private fun ids(list: List<Discovery>) = list.map { it.id }

    @Test
    fun nothingSeenKeepsDatasetOrder() {
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5", "a6"), ids(FreshFirst.order(cards, emptySet())))
    }

    @Test
    fun seenCardsMoveToTheEnd() {
        assertEquals(listOf("a3", "a4", "a5", "a6", "a1", "a2"), ids(FreshFirst.order(cards, setOf("a1", "a2"))))
    }

    @Test
    fun orderInsideEachGroupFollowsDatasetOrder() {
        assertEquals(listOf("a1", "a3", "a4", "a6", "a2", "a5"), ids(FreshFirst.order(cards, setOf("a2", "a5"))))
    }

    @Test
    fun everythingSeenKeepsDatasetOrder() {
        val allSeen = setOf("a1", "a2", "a3", "a4", "a5", "a6")
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5", "a6"), ids(FreshFirst.order(cards, allSeen)))
    }

    @Test
    fun unknownIdsAreIgnored() {
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5", "a6"), ids(FreshFirst.order(cards, setOf("zzz"))))
    }

    @Test
    fun allSixCardsAreAlwaysPresent() {
        val result = FreshFirst.order(cards, setOf("a3"))
        assertEquals(6, result.size)
        assertEquals(cards.map { it.id }.toSet(), result.map { it.id }.toSet())
    }

    @Test
    fun theInputIsNotModified() {
        FreshFirst.order(cards, setOf("a1"))
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5", "a6"), ids(cards))
    }

    @Test
    fun anEmptyListStaysEmpty() {
        assertTrue(FreshFirst.order(emptyList(), setOf("a1")).isEmpty())
    }

    @Test
    fun oneSeenCardAtTheEndStaysAtTheEnd() {
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5", "a6"), ids(FreshFirst.order(cards, setOf("a6"))))
    }

    @Test
    fun oneSeenCardAtTheStartMovesLast() {
        assertEquals(listOf("a2", "a3", "a4", "a5", "a6", "a1"), ids(FreshFirst.order(cards, setOf("a1"))))
    }

    @Test
    fun theFeedComposerStillReturnsDatasetOrder() {
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5", "a6"), ids(FeedComposer.feedFor("t1", TestContent.discoveries)))
    }

    @Test
    fun theSwitchIsOn() {
        assertTrue(FeedConfig.FRESH_FIRST)
    }

    @Test
    fun theDefaultSourceKnowsNothing() = runTest {
        assertTrue(FreshnessSource.None.seenBefore(DAY).isEmpty())
    }
}
