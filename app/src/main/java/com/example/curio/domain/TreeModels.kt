package com.example.curio.domain

object TopicProgress {
    const val EXPLORED_AT = 3
}

data class ActivitySnapshot(
    val completedIds: Set<String> = emptySet(),
    val completedFeedDays: Set<String> = emptySet(),
    val savedIds: Set<String> = emptySet(),
    val deepReadIds: Set<String> = emptySet(),
    val bonusCompletedIds: Set<String> = emptySet(),
    val completedDays: Set<String> = emptySet(),
    val firstCompletedAt: Map<String, Long> = emptyMap(),
)

data class ContentIndex(
    val themes: List<Theme>,
    val discoveries: List<Discovery>,
) {
    fun discoveriesOf(topicId: String): List<Discovery> =
        discoveries.filter { it.subcategoryId == topicId || it.themeId == topicId }
}

object ActivitySnapshotBuilder {
    fun build(
        events: List<ViewEvent>,
        savedIds: Set<String>,
        days: List<String>,
        index: ContentIndex,
    ): ActivitySnapshot {
        val completedIds = events.filter { it.completed }.map { it.discoveryId }.toSet()
        val bonusCompletedIds = events.filter { it.completed && it.source == ViewSource.BONUS }.map { it.discoveryId }.toSet()
        val firstCompletedAt = events.filter { it.completed }
            .groupBy { it.discoveryId }
            .mapValues { (_, evs) -> evs.minOf { it.firstViewedAt } }

        return ActivitySnapshot(
            completedIds = completedIds,
            bonusCompletedIds = bonusCompletedIds,
            savedIds = savedIds,
            firstCompletedAt = firstCompletedAt,
        )
    }
}

enum class NodeState { LOCKED, SEEDED, GROWING, COMPLETE }
enum class GrowthStage { SEED, SPROUT, SAPLING, TREE, MATURE, BLOOM }

data class LeafNode(
    val discoveryId: String,
    val title: String?,          // null until completed: titles are the facts, never spoiled
    val completed: Boolean,
    val isLandmark: Boolean,     // the topic's KNOWLEDGE card (flower leaf)
    val viaBonus: Boolean,       // first completed through a shake bonus (spark marker)
)

data class TopicNode(
    val topic: Topic,
    val completed: Int,
    val total: Int,
    val state: NodeState,
    val leaves: List<LeafNode>,          // dataset order
    val firstCompletedAtMs: Long?,       // earliest completion among this topic's leaves, for "why did this grow"
) {
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

data class ThemeTree(
    val theme: Theme,
    val stage: GrowthStage,
    val completed: Int,
    val total: Int,
    val branches: List<TopicNode>,       // the theme's topic order
) {
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

object TreeConfig {
    // Growth stage of a theme, from completed / total. Integer percentages, evaluated without floats.
    const val SAPLING_FROM_PERCENT = 12
    const val TREE_FROM_PERCENT = 33
    const val MATURE_FROM_PERCENT = 66

    /** The landmark leaf of a topic is its first card of this type, in dataset order. */
    val LANDMARK_TYPE: ContentType = ContentType.KNOWLEDGE
}
