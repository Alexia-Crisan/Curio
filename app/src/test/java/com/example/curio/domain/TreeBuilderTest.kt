package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.curio.domain.GrowthStage.BLOOM
import com.example.curio.domain.GrowthStage.MATURE
import com.example.curio.domain.GrowthStage.SAPLING
import com.example.curio.domain.GrowthStage.SEED
import com.example.curio.domain.GrowthStage.SPROUT
import com.example.curio.domain.GrowthStage.TREE
import com.example.curio.domain.NodeState.COMPLETE
import com.example.curio.domain.NodeState.GROWING
import com.example.curio.domain.NodeState.LOCKED
import com.example.curio.domain.NodeState.SEEDED

class TreeBuilderTest {
    private val c = TestContent.index
    private val animals = c.themes.first { it.id == "animals" }

    private fun snap(
        completed: Set<String> = emptySet(),
        bonus: Set<String> = emptySet(),
        firstAt: Map<String, Long> = emptyMap(),
    ) = ActivitySnapshot(
        completedIds = completed,
        completedFeedDays = emptySet(),
        savedIds = emptySet(),
        deepReadIds = emptySet(),
        bonusCompletedIds = bonus,
        completedDays = emptySet(),
        firstCompletedAt = firstAt,
    )

    private fun tree(s: ActivitySnapshot = snap(), theme: Theme = animals, index: ContentIndex = c) =
        TreeBuilder.build(theme, index, s)

    private fun ThemeTree.branch(topicId: String) = branches.first { it.topic.id == topicId }
    private fun idsOf(prefix: String, range: IntRange = 1..6) = range.map { "$prefix$it" }
    private fun ids(prefix: String, range: IntRange) = idsOf(prefix, range).toSet()

    private val bigCards = (1..7).flatMap { t -> (1..6).map { i -> TestContent.disc("g${t}_$i", "big", "g$t") } }
    private val bigTheme = TestContent.theme("big", *(1..7).map { "g$it" }.toTypedArray())
    private val big = ContentIndex(listOf(bigTheme), bigCards)

    private fun ev(
        id: String,
        source: ViewSource = ViewSource.FEED,
        completed: Boolean = true,
        nowMs: Long = 1_000,
    ) = ViewEvent(
        discoveryId = id,
        date = java.time.LocalDate.of(2026, 10, 26),
        firstViewedAt = nowMs,
        dwellMs = 3_000,
        completed = completed,
        skipped = false,
        source = source,
        detailDwellMs = 0,
    )

    @Test fun anEmptyHistoryGivesASeedWithFourLockedBranches() {
        val t = tree()
        assertEquals(SEED, t.stage)
        assertEquals(0, t.completed)
        assertEquals(24, t.total)
        assertEquals(4, t.branches.size)
        assertTrue(t.branches.all { it.state == LOCKED && it.completed == 0 && it.firstCompletedAtMs == null })
    }

    @Test fun everyBranchHasSixLeavesInDatasetOrder() {
        tree().branches.forEach { b ->
            assertEquals(6, b.leaves.size)
            assertEquals(6, b.total)
        }
        assertEquals(idsOf("b"), tree().branch("t2").leaves.map { it.discoveryId })
    }

    @Test fun branchesFollowTheThemeTopicOrder() =
        assertEquals(listOf("t1", "t2", "t3", "t4"), tree().branches.map { it.topic.id })

    @Test fun topicStateBoundariesForSixCards() {
        val expected = mapOf(0 to LOCKED, 1 to SEEDED, 2 to SEEDED, 3 to GROWING, 4 to GROWING, 5 to GROWING, 6 to COMPLETE)
        expected.forEach { (done, state) -> assertEquals("$done of 6", state, TreeBuilder.stateFor(done, 6)) }
    }

    @Test fun theBuiltBranchesFollowTheSameBoundaries() {
        val t = tree(snap(completed = ids("a", 1..3) + ids("b", 1..2) + ids("c", 1..6)))
        assertEquals(GROWING, t.branch("t1").state)
        assertEquals(SEEDED, t.branch("t2").state)
        assertEquals(COMPLETE, t.branch("t3").state)
        assertEquals(LOCKED, t.branch("t4").state)
    }

    @Test fun aSmallTopicIsCompleteWhenAllItsCardsAreDone() {
        assertEquals(COMPLETE, TreeBuilder.stateFor(2, 2))
        assertEquals(SEEDED, TreeBuilder.stateFor(1, 2))
        assertEquals(COMPLETE, TreeBuilder.stateFor(3, 3))
    }

    @Test fun aTopicWithoutCardsIsLocked() = assertEquals(LOCKED, TreeBuilder.stateFor(0, 0))

    @Test fun growingIsExactlyTheExploredTopicRule() {
        for (done in 0..5) {
            val explored = done >= TopicProgress.EXPLORED_AT
            assertEquals("$done of 6", explored, TreeBuilder.stateFor(done, 6) == GROWING)
        }
    }

    @Test fun completingOneTopicDoesNotChangeTheOthers() {
        val t = tree(snap(completed = ids("a", 1..6)))
        assertEquals(COMPLETE, t.branch("t1").state)
        assertTrue(listOf("t2", "t3", "t4").all { t.branch(it).state == LOCKED })
    }

    @Test fun stageThresholdsForFortyTwoCards() {
        val expected = listOf(
            0 to SEED, 1 to SPROUT, 5 to SPROUT, 6 to SAPLING, 13 to SAPLING,
            14 to TREE, 27 to TREE, 28 to MATURE, 41 to MATURE, 42 to BLOOM,
        )
        expected.forEach { (n, stage) -> assertEquals("$n of 42", stage, TreeBuilder.stageFor(n, 42)) }
    }

    @Test fun stageThresholdsForTwelveCards() {
        val expected = listOf(
            1 to SPROUT, 2 to SAPLING, 3 to SAPLING, 4 to TREE, 7 to TREE, 8 to MATURE, 11 to MATURE, 12 to BLOOM,
        )
        expected.forEach { (n, stage) -> assertEquals("$n of 12", stage, TreeBuilder.stageFor(n, 12)) }
    }

    @Test fun aThemeWithoutCardsStaysASeed() = assertEquals(SEED, TreeBuilder.stageFor(0, 0))

    @Test fun theBuiltStageUsesTheWholeTheme() {
        val firstSix = bigCards.take(6).map { it.id }.toSet()
        assertEquals(SAPLING, TreeBuilder.build(bigTheme, big, snap(completed = firstSix)).stage)
        val first14 = bigCards.take(14).map { it.id }.toSet()
        assertEquals(TREE, TreeBuilder.build(bigTheme, big, snap(completed = first14)).stage)
    }

    @Test fun aFullThemeIsInBloom() {
        val t = tree(snap(completed = ids("a", 1..6) + ids("b", 1..6) + ids("c", 1..6) + ids("d", 1..6)))
        assertEquals(BLOOM, t.stage)
        assertTrue(t.branches.all { it.state == COMPLETE })
    }

    @Test fun themeTotalsSumTheBranches() {
        val t = tree(snap(completed = ids("a", 1..2) + ids("c", 1..4)))
        assertEquals(6, t.completed)
        assertEquals(24, t.total)
        assertEquals(0.25f, t.fraction, 0.001f)
    }

    @Test fun aLeafIsLitOnlyWhenItsCardIsCompleted() {
        val leaves = tree(snap(completed = setOf("a2", "a4"))).branch("t1").leaves
        assertEquals(listOf(false, true, false, true, false, false), leaves.map { it.completed })
    }

    @Test fun undiscoveredLeavesHideTheirTitle() {
        val leaves = tree(snap(completed = setOf("a1"))).branch("t1").leaves
        assertNull(leaves[1].title)
        assertTrue(leaves.drop(1).all { it.title == null })
    }

    @Test fun discoveredLeavesShowTheirTitle() =
        assertEquals("a1", tree(snap(completed = setOf("a1"))).branch("t1").leaves[0].title)

    @Test fun theNoCardsLeakATitleWhenNothingIsDone() =
        assertTrue(tree().branches.flatMap { it.leaves }.all { it.title == null })

    @Test fun theLandmarkIsTheKnowledgeCard() {
        val leaves = tree().branch("t1").leaves
        assertEquals(listOf("a6"), leaves.filter { it.isLandmark }.map { it.discoveryId })
    }

    @Test fun aTopicWithoutAKnowledgeCardHasNoLandmark() {
        val cards = (1..6).map { TestContent.disc("k$it", "x", "k1") }
        val idx = ContentIndex(listOf(TestContent.theme("x", "k1")), cards)
        assertTrue(TreeBuilder.build(idx.themes.first(), idx, snap()).branches.single().leaves.none { it.isLandmark })
    }

    @Test fun onlyTheFirstOfSeveralKnowledgeCardsIsTheLandmark() {
        val cards = listOf(
            TestContent.disc("k1", "x", "k1"),
            TestContent.disc("k2", "x", "k1", ContentType.KNOWLEDGE),
            TestContent.disc("k3", "x", "k1", ContentType.KNOWLEDGE),
        )
        val idx = ContentIndex(listOf(TestContent.theme("x", "k1")), cards)
        val leaves = TreeBuilder.build(idx.themes.first(), idx, snap()).branches.single().leaves
        assertEquals(listOf("k2"), leaves.filter { it.isLandmark }.map { it.discoveryId })
    }

    @Test fun aBonusCompletionRevealsALockedBranchAndGetsTheSparkMarker() {
        val t = tree(snap(completed = setOf("b1"), bonus = setOf("b1")))
        assertEquals(SEEDED, t.branch("t2").state)
        assertTrue(t.branch("t2").leaves[0].viaBonus)
        assertEquals(LOCKED, t.branch("t1").state)
    }

    @Test fun aRevisitedBonusIsLitWithoutTheSparkMarker() {
        val leaf = tree(snap(completed = setOf("b1"), bonus = emptySet())).branch("t2").leaves[0]
        assertTrue(leaf.completed)
        assertFalse(leaf.viaBonus)
    }

    @Test fun aBonusThatWasNotCompletedIsNotShownAsLit() {
        val leaf = tree(snap(completed = emptySet(), bonus = setOf("b1"))).branch("t2").leaves[0]
        assertFalse(leaf.completed)
        assertFalse(leaf.viaBonus)
    }

    @Test fun unknownIdsAreIgnored() {
        val t = tree(snap(completed = setOf("ghost", "a1")))
        assertEquals(1, t.completed)
    }

    @Test fun completionsInAnotherThemeDoNotAffectThisTree() {
        val t = tree(snap(completed = ids("s", 1..6)))
        assertEquals(0, t.completed)
        assertEquals(SEED, t.stage)
    }

    @Test fun theEarliestCompletionOfATopicIsReported() {
        val s = snap(completed = setOf("a1", "a2"), firstAt = mapOf("a1" to 5_000L, "a2" to 2_000L, "b1" to 1L))
        assertEquals(2_000L, tree(s).branch("t1").firstCompletedAtMs)
        assertNull(tree(s).branch("t3").firstCompletedAtMs)
    }

    @Test fun buildAllSkipsUnavailableAndEmptyThemes() {
        val withEmpty = ContentIndex(c.themes + TestContent.theme("empty"), c.discoveries)
        assertEquals(listOf("animals", "space"), TreeBuilder.buildAll(withEmpty, snap()).map { it.theme.id })
    }

    @Test fun buildingTwiceGivesIdenticalTrees() {
        val s = snap(completed = ids("a", 1..4) + setOf("b1", "s1"), bonus = setOf("b1"), firstAt = mapOf("a1" to 9L))
        assertEquals(TreeBuilder.buildAll(c, s), TreeBuilder.buildAll(c, s))
    }

    @Test fun theTreeFollowsTheRealSnapshotBuilder() {
        val events = listOf(
            ev("a1"),
            ev("a2", source = ViewSource.DETAIL),
            ev("b1", source = ViewSource.BONUS),
            ev("c1", completed = false),
        )
        val s = ActivitySnapshotBuilder.build(events, emptySet(), emptyList(), c)
        val t = tree(s)
        assertEquals(SEEDED, t.branch("t1").state)
        assertTrue(t.branch("t1").leaves[1].completed)
        assertTrue(t.branch("t2").leaves[0].viaBonus)
        assertEquals(LOCKED, t.branch("t3").state)
    }

    @Test fun buildingEveryTreeIsFast() {
        val s = snap(completed = bigCards.take(30).map { it.id }.toSet())
        val start = System.nanoTime()
        repeat(200) { TreeBuilder.build(bigTheme, big, s) }
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("200 builds took $ms ms", ms < 2_000)
    }
}
