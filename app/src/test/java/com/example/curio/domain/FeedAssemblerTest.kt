package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedAssemblerTest {
    private val discoveries = TestContent.discoveries

    @Test
    fun sixRegularCardsInDatasetOrder() {
        val items = FeedAssembler.assemble("t1", discoveries)
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5", "a6"), items.map { it.discovery.id })
        assertTrue(items.all { it.kind == FeedItemKind.REGULAR })
        assertTrue(items.none { it.isRevisit })
    }

    @Test
    fun bonusCardsComeAfterTheSixInRevealOrder() {
        val b1 = TestContent.disc("c2", "animals", "t3")
        val b2 = TestContent.disc("s1", "space", "u1")
        val items = FeedAssembler.assemble("t1", discoveries, listOf(b1, b2))
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5", "a6", "c2", "s1"), items.map { it.discovery.id })
        assertEquals(listOf(FeedItemKind.REGULAR, FeedItemKind.REGULAR, FeedItemKind.REGULAR, FeedItemKind.REGULAR, FeedItemKind.REGULAR, FeedItemKind.REGULAR, FeedItemKind.BONUS, FeedItemKind.BONUS), items.map { it.kind })
    }

    @Test
    fun aBonusNeverRepeatsARegularCard() {
        val b1 = TestContent.disc("a2", "animals", "t1")
        val b2 = TestContent.disc("c2", "animals", "t3")
        val items = FeedAssembler.assemble("t1", discoveries, listOf(b1, b2, b2))
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5", "a6", "c2"), items.map { it.discovery.id })
    }

    @Test
    fun revisitFlagOnlyOnTheListedIds() {
        val b1 = TestContent.disc("c2", "animals", "t3")
        val b2 = TestContent.disc("s1", "space", "u1")
        val items = FeedAssembler.assemble("t1", discoveries, listOf(b1, b2), revisitIds = setOf("c2"))
        val c2Item = items.first { it.discovery.id == "c2" }
        val s1Item = items.first { it.discovery.id == "s1" }
        assertTrue(c2Item.isRevisit)
        assertFalse(s1Item.isRevisit)
    }

    @Test
    fun anUnknownTopicGivesAnEmptyFeed() {
        val items = FeedAssembler.assemble("zzz", discoveries)
        assertTrue(items.isEmpty())
    }

    @Test
    fun sourceFollowsTheKind() {
        val b1 = TestContent.disc("c2", "animals", "t3")
        val items = FeedAssembler.assemble("t1", discoveries, listOf(b1))
        val reg = items.first { it.kind == FeedItemKind.REGULAR }
        val bon = items.first { it.kind == FeedItemKind.BONUS }
        assertEquals(ViewSource.FEED, reg.source)
        assertEquals(ViewSource.BONUS, bon.source)
    }
}
