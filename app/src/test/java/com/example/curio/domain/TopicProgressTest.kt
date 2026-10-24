package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TopicProgressTest {
    private val c = TestContent.index

    @Test fun everyTopicIsListedEvenWithZero() {
        val res = TopicProgress.completedByTopic(emptySet(), c)
        assertEquals(setOf("t1", "t2", "t3", "t4", "u1", "u2"), res.keys)
        assertTrue(res.values.all { it == 0 })
    }

    @Test fun countsCompletedPerTopic() {
        val res = TopicProgress.completedByTopic(setOf("a1", "a2", "b1"), c)
        assertEquals(2, res["t1"])
        assertEquals(1, res["t2"])
    }

    @Test fun unknownIdsAreIgnored() {
        val res = TopicProgress.completedByTopic(setOf("a1", "ghost"), c)
        assertEquals(1, res.values.sum())
    }

    @Test fun exploredNeedsThreeDiscoveries() {
        assertEquals(0, TopicProgress.exploredTopicsByTheme(setOf("a1", "a2"), c)["animals"])
        assertEquals(1, TopicProgress.exploredTopicsByTheme(setOf("a1", "a2", "a3"), c)["animals"])
    }

    @Test fun exploredTopicsAreCountedPerTheme() {
        val completed = setOf("a1", "a2", "a3", "b1", "b2", "b3", "s1", "s2", "s3")
        val res = TopicProgress.exploredTopicsByTheme(completed, c)
        assertEquals(2, res["animals"])
        assertEquals(1, res["space"])
    }

    @Test fun completedByThemeCountsDistinctIds() {
        val completed = setOf("a1", "b1", "s1")
        val res = TopicProgress.completedByTheme(completed, c)
        assertEquals(2, res["animals"])
        assertEquals(1, res["space"])
        assertEquals(0, res["oceans"])
    }
}
