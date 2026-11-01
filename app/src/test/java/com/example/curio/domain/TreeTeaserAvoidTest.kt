package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TreeTeaserAvoidTest {
    private val ids = listOf("t1", "t2", "t3", "t4")
    private val D = LocalDate.of(2026, 10, 26)
    private val zone = ZoneId.of("Europe/Bucharest")

    private fun tree(done: Set<String> = emptySet()) =
        ThemeTree(
            theme = Theme("animals", "Animals", ids.map { Topic(it, "animals", "Topic $it") }),
            stage = GrowthStage.SEED,
            completed = done.size,
            total = ids.size,
            branches = ids.map { tId ->
                TopicNode(
                    topic = Topic(tId, "animals", "Topic $tId"),
                    state = if (tId in done) NodeState.COMPLETE else NodeState.LOCKED,
                    completed = if (tId in done) 6 else 0,
                    total = 6,
                    leaves = emptyList(),
                    firstCompletedAtMs = null,
                )
            },
        )

    @Test
    fun theDefaultKeepsTheOldPrediction() {
        val t = tree()
        val next = TreeTeaser.nextVisit(t, DailyMode.SURPRISE, D)
        assertNotNull(next)
        assertEquals(NextVisit(D.plusDays(1), TopicSelector.surprise(D.plusDays(1), "animals", ids)!!), next)
    }

    @Test
    fun avoidRepeatsHonoursTodaysTopic() {
        val t = tree()
        val prev = TopicSelector.surprise(D.plusDays(1), "animals", ids)!!
        val next = TreeTeaser.nextVisit(t, DailyMode.SURPRISE, D, previousTopicId = prev, avoidRepeats = true)
        assertNotNull(next)
        assertNotEquals(prev, next?.topicId)
        assertEquals(TopicSelector.surprise(D.plusDays(1), "animals", ids, prev), next?.topicId)
    }

    @Test
    fun thePreviousTopicIsIgnoredWithoutTheFlag() {
        val t = tree()
        val prev = TopicSelector.surprise(D.plusDays(1), "animals", ids)!!
        val next = TreeTeaser.nextVisit(t, DailyMode.SURPRISE, D, previousTopicId = prev, avoidRepeats = false)
        assertEquals(NextVisit(D.plusDays(1), prev), next)
    }

    @Test
    fun guidedIgnoresTheFlag() {
        val t = tree()
        val next1 = TreeTeaser.nextVisit(t, DailyMode.GUIDED, D, previousTopicId = "t3", avoidRepeats = true)
        val next2 = TreeTeaser.nextVisit(t, DailyMode.GUIDED, D)
        assertEquals(next2, next1)
    }

    @Test
    fun chooseStillGivesNull() {
        val t = tree()
        assertNull(TreeTeaser.nextVisit(t, DailyMode.CHOOSE, D, avoidRepeats = true))
    }

    @Test
    fun noPreviousTopicStillWorksWithTheFlag() {
        val t = tree()
        val next = TreeTeaser.nextVisit(t, DailyMode.SURPRISE, D, previousTopicId = null, avoidRepeats = true)
        assertEquals(NextVisit(D.plusDays(1), TopicSelector.surprise(D.plusDays(1), "animals", ids)!!), next)
    }

    @Test
    fun theChainNeverReturnsTheSameTopicTwiceInARow() {
        val t = tree(setOf("t1", "t2", "t3"))
        val next = TreeTeaser.nextVisit(t, DailyMode.SURPRISE, D, previousTopicId = "t4", avoidRepeats = true)
        assertNotNull(next)
    }

    @Test
    fun theBuilderPassesTodaysTopic() {
        val t = tree()
        val trees = listOf(t)
        val teaser = TeaserBuilder.forFeed(trees, "animals", DailyMode.SURPRISE, D, zone, todayTopicId = "t1")
        val expectedNext = TreeTeaser.nextVisit(t, DailyMode.SURPRISE, D, previousTopicId = "t1", avoidRepeats = true)
        assertEquals(expectedNext, teaser?.next)
    }
}
