package com.example.curio.domain

sealed interface NextStep {
    data class StartTopic(val cardsToGrow: Int) : NextStep
    data class ReachGrowing(val cardsLeft: Int) : NextStep
    data class FinishTopic(val cardsLeft: Int) : NextStep
    data object Done : NextStep
}

data class TopicExplanation(
    val topicId: String,
    val state: NodeState,
    val completed: Int,
    val total: Int,
    val next: NextStep,
    val firstCompletedAtMs: Long?,
    val bonusLeaves: Int,
    val hasLandmark: Boolean,
    val landmarkFound: Boolean,
)

data class LeafExplanation(
    val discoveryId: String,
    val title: String?,
    val completed: Boolean,
    val isLandmark: Boolean,
    val viaBonus: Boolean,
) {
    val canOpenDetail: Boolean get() = completed
}

data class ThemeExplanation(
    val completed: Int,
    val total: Int,
    val stage: GrowthStage,
    val nextStage: GrowthStage?,
    val cardsToNextStage: Int?,
)

object TreeExplainer {

    fun explainTopic(node: TopicNode): TopicExplanation {
        val next = when {
            node.total == 0 || node.completed >= node.total -> NextStep.Done
            node.completed >= TopicProgress.EXPLORED_AT -> NextStep.FinishTopic(node.total - node.completed)
            node.total <= TopicProgress.EXPLORED_AT -> NextStep.FinishTopic(node.total - node.completed)
            node.completed == 0 -> NextStep.StartTopic(TopicProgress.EXPLORED_AT)
            else -> NextStep.ReachGrowing(TopicProgress.EXPLORED_AT - node.completed)
        }
        val landmark = node.leaves.firstOrNull { it.isLandmark }
        return TopicExplanation(
            topicId = node.topic.id,
            state = node.state,
            completed = node.completed,
            total = node.total,
            next = next,
            firstCompletedAtMs = node.firstCompletedAtMs,
            bonusLeaves = node.leaves.count { it.viaBonus },
            hasLandmark = landmark != null,
            landmarkFound = landmark?.completed == true,
        )
    }

    fun explainLeaf(leaf: LeafNode): LeafExplanation =
        LeafExplanation(leaf.discoveryId, leaf.title, leaf.completed, leaf.isLandmark, leaf.viaBonus)

    fun explainTheme(tree: ThemeTree): ThemeExplanation {
        val total = tree.total
        val done = tree.completed
        val stage = TreeBuilder.stageFor(done, total)
        var nextStage: GrowthStage? = null
        var cardsLeft: Int? = null
        var candidate = done + 1
        while (candidate <= total) {
            val s = TreeBuilder.stageFor(candidate, total)
            if (s != stage) {
                nextStage = s
                cardsLeft = candidate - done
                break
            }
            candidate++
        }
        return ThemeExplanation(done, total, stage, nextStage, cardsLeft)
    }
}
