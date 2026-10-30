package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedAssemblerFreshTest {
    private fun ids(items: List<FeedItem>) = items.map { it.discovery.id }
    private val c2 = TestContent.discoveries.first { it.id == "c2" }
    private val s1 = TestContent.discoveries.first { it.id == "s1" }
    private val a1 = TestContent.discoveries.first { it.id == "a1" }

    @Test
    fun theDefaultIsTheOldBehaviour() {
        val items = FeedAssembler.assemble("t1", TestContent.discoveries)
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5", "a6"), ids(items))
        assertTrue(items.all { it.kind == FeedItemKind.REGULAR })
    }

    @Test
    fun seenCardsGoLastInTheRegularPart() {
        val items = FeedAssembler.assemble("t1", TestContent.discoveries, completedBefore = setOf("a1", "a2"))
        assertEquals(listOf("a3", "a4", "a5", "a6", "a1", "a2"), ids(items))
        assertTrue(items.all { it.kind == FeedItemKind.REGULAR })
    }

    @Test
    fun bonusCardsKeepTheirPlaceAtTheEnd() {
        val items = FeedAssembler.assemble("t1", TestContent.discoveries, bonus = listOf(c2, s1), completedBefore = setOf("a1"))
        assertEquals(listOf("a2", "a3", "a4", "a5", "a6", "a1", "c2", "s1"), ids(items))
        assertEquals(listOf(FeedItemKind.REGULAR, FeedItemKind.REGULAR, FeedItemKind.REGULAR, FeedItemKind.REGULAR, FeedItemKind.REGULAR, FeedItemKind.REGULAR, FeedItemKind.BONUS, FeedItemKind.BONUS), items.map { it.kind })
    }

    @Test
    fun bonusCardsAreNeverReordered() {
        val items = FeedAssembler.assemble("t1", TestContent.discoveries, bonus = listOf(s1, c2), completedBefore = setOf("s1"))
        assertEquals(listOf("s1", "c2"), ids(items.filter { it.kind == FeedItemKind.BONUS }))
    }

    @Test
    fun revisitFlagsAreUnaffected() {
        val items = FeedAssembler.assemble("t1", TestContent.discoveries, bonus = listOf(c2), revisitIds = setOf("c2"), completedBefore = setOf("a1"))
        val bonusItem = items.first { it.kind == FeedItemKind.BONUS }
        assertTrue(bonusItem.isRevisit)
        assertTrue(items.filter { it.kind == FeedItemKind.REGULAR }.none { it.isRevisit })
    }

    @Test
    fun aBonusNeverRepeatsARegularCard() {
        val items = FeedAssembler.assemble("t1", TestContent.discoveries, bonus = listOf(a1, c2), completedBefore = setOf("a1"))
        assertEquals(listOf("a2", "a3", "a4", "a5", "a6", "a1", "c2"), ids(items))
    }

    @Test
    fun theSetOfRegularCardsNeverChanges() {
        val expected = setOf("a1", "a2", "a3", "a4", "a5", "a6")
        listOf(emptySet(), setOf("a1"), setOf("a2", "a4"), setOf("a1", "a2", "a3", "a4", "a5", "a6")).forEach { seen ->
            val items = FeedAssembler.assemble("t1", TestContent.discoveries, completedBefore = seen)
            assertEquals(expected, items.map { it.discovery.id }.toSet())
        }
    }

    @Test
    fun anUnknownTopicStaysEmpty() {
        val items = FeedAssembler.assemble("zzz", TestContent.discoveries, completedBefore = setOf("a1"))
        assertTrue(items.isEmpty())
    }
}
