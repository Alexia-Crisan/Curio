package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TreePerformanceTest {
    private val src = Source("n", "https://example.com", "CC0")

    private fun synthContent(): ContentIndex {
        val themes = (0..2).map { i ->
            val topics = (0..6).map { j -> Topic("th${i}t${j}", "th$i", "Topic $i-$j") }
            Theme("th$i", "Theme $i", topics)
        }
        val discoveries = mutableListOf<Discovery>()
        themes.forEach { theme ->
            theme.topics.forEach { topic ->
                (1..6).forEach { k ->
                    val type = if (k == 6) ContentType.KNOWLEDGE else ContentType.FACT
                    discoveries.add(
                        Discovery("${topic.id}c$k", theme.id, topic.id, type, "Card $k", source = src)
                    )
                }
            }
        }
        return ContentIndex(themes, discoveries)
    }

    private val content = synthContent()
    private val snapshot = snapshotOf(
        completed = content.discoveries.filterIndexed { index, _ -> index % 2 == 0 }.map { it.id }.toSet()
    )

    private fun medianNs(block: () -> Unit): Long {
        repeat(300) { block() }
        val times = LongArray(200)
        repeat(200) { i ->
            val t0 = System.nanoTime()
            block()
            times[i] = System.nanoTime() - t0
        }
        times.sort()
        return times[100]
    }

    @Test
    fun treeBuildIsDeterministic() {
        val t1 = TreeBuilder.buildAll(content, snapshot)
        val t2 = TreeBuilder.buildAll(content, snapshot)
        val content2 = synthContent()
        val t3 = TreeBuilder.buildAll(content2, snapshot)
        assertEquals(t1, t2)
        assertEquals(t1, t3)
    }

    @Test
    fun treeBuildTakesUnderFiveMilliseconds() {
        val ns = medianNs { TreeBuilder.buildAll(content, snapshot) }
        assertTrue("median build time $ns ns >= 5_000_000 ns", ns < 5_000_000L)
    }

    @Test
    fun badgeEvaluationTakesUnderFiveMilliseconds() {
        val ns = medianNs { AchievementEvaluator.evaluate(snapshot, content) }
        assertTrue("median eval time $ns ns >= 5_000_000 ns", ns < 5_000_000L)
    }

    @Test
    fun everyLeafBelongsToExactlyOneTopic() {
        val trees = TreeBuilder.buildAll(content, snapshot)
        val allLeafIds = trees.flatMap { it.branches }.flatMap { it.leaves }.map { it.discoveryId }
        assertEquals(126, allLeafIds.size)
        assertEquals(126, allLeafIds.toSet().size)
    }

    @Test
    fun everyTopicHasSixLeavesAndALandmark() {
        val trees = TreeBuilder.buildAll(content, snapshot)
        trees.flatMap { it.branches }.forEach { branch ->
            assertEquals(6, branch.leaves.size)
            assertEquals(1, branch.leaves.count { it.isLandmark })
        }
    }

    @Test
    fun lockedLeavesLeakNoTitles() {
        val trees = TreeBuilder.buildAll(content, snapshot)
        trees.flatMap { it.branches }.flatMap { it.leaves }.forEach { leaf ->
            if (!leaf.completed) {
                assertNull(leaf.title)
            }
        }
    }

    @Test
    fun anEmptySnapshotLocksEverything() {
        val trees = TreeBuilder.buildAll(content, snapshotOf())
        assertTrue(trees.flatMap { it.branches }.all { it.state == NodeState.LOCKED })
        assertTrue(trees.all { it.stage == GrowthStage.SEED })
    }
}
