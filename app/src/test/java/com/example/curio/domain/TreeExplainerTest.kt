package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeExplainerTest {
    private fun node(done: Set<String>, bonus: Set<String> = emptySet(), at: Map<String, Long> = emptyMap()) =
        TreeBuilder.build(TestContent.index.themes[0], TestContent.index, snapshotOf(done, bonus, at)).branches.first { it.topic.id == "t1" }

    private fun manual(completed: Int, total: Int): TopicNode {
        val leaves = (1..total).map { LeafNode("a$it", "a$it", completed = it <= completed, isLandmark = false, viaBonus = false) }
        val topic = Topic("t1", "animals", "t1")
        return TopicNode(topic, completed, total, TreeBuilder.stateFor(completed, total), leaves, null)
    }

    private fun tree(done: Int, total: Int) =
        ThemeTree(TestContent.index.themes[0], TreeBuilder.stageFor(done, total), done, total, emptyList())

    @Test
    fun nothingDoneMeansStartAndThreeCardsToGrow() {
        val exp = TreeExplainer.explainTopic(node(emptySet()))
        assertEquals(NodeState.LOCKED, exp.state)
        assertEquals(NextStep.StartTopic(3), exp.next)
    }

    @Test
    fun oneCardNeedsTwoMore() {
        val exp = TreeExplainer.explainTopic(node(setOf("a1")))
        assertEquals(NodeState.SEEDED, exp.state)
        assertEquals(NextStep.ReachGrowing(2), exp.next)
    }

    @Test
    fun twoCardsNeedOneMore() {
        val exp = TreeExplainer.explainTopic(node(setOf("a1", "a2")))
        assertEquals(NextStep.ReachGrowing(1), exp.next)
    }

    @Test
    fun threeCardsMeansGrowingAndThreeLeft() {
        val exp = TreeExplainer.explainTopic(node(idsOf("a", 1..3).toSet()))
        assertEquals(NodeState.GROWING, exp.state)
        assertEquals(NextStep.FinishTopic(3), exp.next)
    }

    @Test
    fun fiveCardsNeedOneToFinish() {
        val exp = TreeExplainer.explainTopic(node(idsOf("a", 1..5).toSet()))
        assertEquals(NextStep.FinishTopic(1), exp.next)
    }

    @Test
    fun allSixIsDone() {
        val exp = TreeExplainer.explainTopic(node(idsOf("a", 1..6).toSet()))
        assertEquals(NodeState.COMPLETE, exp.state)
        assertEquals(NextStep.Done, exp.next)
    }

    @Test
    fun aTwoCardTopicFinishesInsteadOfGrowing() {
        assertEquals(NextStep.FinishTopic(2), TreeExplainer.explainTopic(manual(0, 2)).next)
        assertEquals(NextStep.FinishTopic(1), TreeExplainer.explainTopic(manual(1, 2)).next)
    }

    @Test
    fun aTopicWithoutCardsIsDone() {
        assertEquals(NextStep.Done, TreeExplainer.explainTopic(manual(0, 0)).next)
    }

    @Test
    fun landmarkFoundWhenItsCardIsDone() {
        val exp = TreeExplainer.explainTopic(node(setOf("a6")))
        assertTrue(exp.hasLandmark)
        assertTrue(exp.landmarkFound)
    }

    @Test
    fun landmarkMissingWhenItsCardIsHidden() {
        val exp = TreeExplainer.explainTopic(node(setOf("a1")))
        assertTrue(exp.hasLandmark)
        assertFalse(exp.landmarkFound)
    }

    @Test
    fun noLandmarkWhenTheTopicHasNoKnowledgeCard() {
        val exp = TreeExplainer.explainTopic(manual(0, 6))
        assertFalse(exp.hasLandmark)
        assertFalse(exp.landmarkFound)
    }

    @Test
    fun bonusLeavesAreCounted() {
        val exp = TreeExplainer.explainTopic(node(setOf("a1", "a2"), bonus = setOf("a1", "a2")))
        assertEquals(2, exp.bonusLeaves)
    }

    @Test
    fun aBonusLeafThatIsNotDoneIsNotCounted() {
        val exp = TreeExplainer.explainTopic(node(setOf("a1"), bonus = setOf("a1", "a2")))
        assertEquals(1, exp.bonusLeaves)
    }

    @Test
    fun theFirstCompletionTimeIsCarried() {
        val exp = TreeExplainer.explainTopic(node(setOf("a1", "a2"), at = mapOf("a1" to 500L, "a2" to 200L)))
        assertEquals(200L, exp.firstCompletedAtMs)
    }

    @Test
    fun noCompletionMeansNoTime() {
        val exp = TreeExplainer.explainTopic(node(emptySet()))
        assertNull(exp.firstCompletedAtMs)
    }

    @Test
    fun aHiddenLeafHasNoTitleAndCannotOpen() {
        val leaf = node(emptySet()).leaves.first { it.discoveryId == "a3" }
        val exp = TreeExplainer.explainLeaf(leaf)
        assertNull(exp.title)
        assertFalse(exp.completed)
        assertFalse(exp.canOpenDetail)
    }

    @Test
    fun aDoneLeafShowsItsTitleAndCanOpen() {
        val leaf = node(setOf("a3")).leaves.first { it.discoveryId == "a3" }
        val exp = TreeExplainer.explainLeaf(leaf)
        assertNotNull(exp.title)
        assertTrue(exp.completed)
        assertTrue(exp.canOpenDetail)
    }

    @Test
    fun leafFlagsAreCarried() {
        val leaf = node(setOf("a6"), bonus = setOf("a6")).leaves.first { it.discoveryId == "a6" }
        val exp = TreeExplainer.explainLeaf(leaf)
        assertTrue(exp.isLandmark)
        assertTrue(exp.viaBonus)
    }

    @Test
    fun fortyTwoNothingDone() {
        val exp = TreeExplainer.explainTheme(tree(0, 42))
        assertEquals(GrowthStage.SEED, exp.stage)
        assertEquals(GrowthStage.SPROUT, exp.nextStage)
        assertEquals(1, exp.cardsToNextStage)
    }

    @Test
    fun fortyTwoOneDone() {
        val exp = TreeExplainer.explainTheme(tree(1, 42))
        assertEquals(GrowthStage.SPROUT, exp.stage)
        assertEquals(GrowthStage.SAPLING, exp.nextStage)
        assertEquals(5, exp.cardsToNextStage)
    }

    @Test
    fun fortyTwoFiveDone() {
        val exp = TreeExplainer.explainTheme(tree(5, 42))
        assertEquals(GrowthStage.SPROUT, exp.stage)
        assertEquals(GrowthStage.SAPLING, exp.nextStage)
        assertEquals(1, exp.cardsToNextStage)
    }

    @Test
    fun fortyTwoSixDone() {
        val exp = TreeExplainer.explainTheme(tree(6, 42))
        assertEquals(GrowthStage.SAPLING, exp.stage)
        assertEquals(GrowthStage.TREE, exp.nextStage)
        assertEquals(8, exp.cardsToNextStage)
    }

    @Test
    fun fortyTwoThirteenDone() {
        val exp = TreeExplainer.explainTheme(tree(13, 42))
        assertEquals(GrowthStage.SAPLING, exp.stage)
        assertEquals(GrowthStage.TREE, exp.nextStage)
        assertEquals(1, exp.cardsToNextStage)
    }

    @Test
    fun fortyTwoFourteenDone() {
        val exp = TreeExplainer.explainTheme(tree(14, 42))
        assertEquals(GrowthStage.TREE, exp.stage)
        assertEquals(GrowthStage.MATURE, exp.nextStage)
        assertEquals(14, exp.cardsToNextStage)
    }

    @Test
    fun fortyTwoTwentyEightDone() {
        val exp = TreeExplainer.explainTheme(tree(28, 42))
        assertEquals(GrowthStage.MATURE, exp.stage)
        assertEquals(GrowthStage.BLOOM, exp.nextStage)
        assertEquals(14, exp.cardsToNextStage)
    }

    @Test
    fun fortyTwoFortyOneDone() {
        val exp = TreeExplainer.explainTheme(tree(41, 42))
        assertEquals(GrowthStage.MATURE, exp.stage)
        assertEquals(GrowthStage.BLOOM, exp.nextStage)
        assertEquals(1, exp.cardsToNextStage)
    }

    @Test
    fun fortyTwoAllDone() {
        val exp = TreeExplainer.explainTheme(tree(42, 42))
        assertEquals(GrowthStage.BLOOM, exp.stage)
        assertNull(exp.nextStage)
        assertNull(exp.cardsToNextStage)
    }

    @Test
    fun emptyTreeHasNoNextStage() {
        val exp = TreeExplainer.explainTheme(tree(0, 0))
        assertEquals(GrowthStage.SEED, exp.stage)
        assertNull(exp.nextStage)
        assertNull(exp.cardsToNextStage)
    }

    @Test
    fun twentyFourCards() {
        val exp = TreeExplainer.explainTheme(tree(1, 24))
        assertEquals(GrowthStage.SPROUT, exp.stage)
        assertEquals(GrowthStage.SAPLING, exp.nextStage)
        assertEquals(2, exp.cardsToNextStage)
    }
}
