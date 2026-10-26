package com.example.curio.domain

enum class NodeState { LOCKED, SEEDED, GROWING, COMPLETE }
enum class GrowthStage { SEED, SPROUT, SAPLING, TREE, MATURE, BLOOM }

data class LeafNode(
    val discoveryId: String,
    val title: String?,
    val completed: Boolean,
    val isLandmark: Boolean,
    val viaBonus: Boolean,
    val completedAtMs: Long? = null,
)

data class TopicNode(
    val topic: Topic,
    val completed: Int,
    val total: Int,
    val state: NodeState,
    val leaves: List<LeafNode>,
    val firstCompletedAtMs: Long?,
) {
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

data class ThemeTree(
    val theme: Theme,
    val stage: GrowthStage,
    val completed: Int,
    val total: Int,
    val branches: List<TopicNode>,
) {
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

object TreeConfig {
    const val SAPLING_FROM_PERCENT = 12
    const val TREE_FROM_PERCENT = 33
    const val MATURE_FROM_PERCENT = 66

    val LANDMARK_TYPE: ContentType = ContentType.KNOWLEDGE
}
