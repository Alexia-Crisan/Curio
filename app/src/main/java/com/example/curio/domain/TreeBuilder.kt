package com.example.curio.domain

object TreeBuilder {

    /** LOCKED 0 · SEEDED 1-2 · GROWING >= EXPLORED_AT (3) · COMPLETE when every card is done. */
    fun stateFor(completed: Int, total: Int): NodeState = when {
        total > 0 && completed >= total -> NodeState.COMPLETE
        completed >= TopicProgress.EXPLORED_AT -> NodeState.GROWING
        completed >= 1 -> NodeState.SEEDED
        else -> NodeState.LOCKED
    }

    /** Percent thresholds, compared with integers so 5 of 42 (11.9 %) is still a sprout. */
    fun stageFor(completed: Int, total: Int): GrowthStage = when {
        total <= 0 || completed <= 0 -> GrowthStage.SEED
        completed >= total -> GrowthStage.BLOOM
        completed * 100 < TreeConfig.SAPLING_FROM_PERCENT * total -> GrowthStage.SPROUT
        completed * 100 < TreeConfig.TREE_FROM_PERCENT * total -> GrowthStage.SAPLING
        completed * 100 < TreeConfig.MATURE_FROM_PERCENT * total -> GrowthStage.TREE
        else -> GrowthStage.MATURE
    }

    fun build(theme: Theme, c: ContentIndex, s: ActivitySnapshot): ThemeTree {
        val branches = theme.topics.map { topicNode(it, c, s) }
        val completed = branches.sumOf { it.completed }
        val total = branches.sumOf { it.total }
        return ThemeTree(theme, stageFor(completed, total), completed, total, branches)
    }

    /** One tree per theme that can actually be shown (available and with topics). */
    fun buildAll(c: ContentIndex, s: ActivitySnapshot): List<ThemeTree> =
        c.themes.filter { it.available && it.topics.isNotEmpty() }.map { build(it, c, s) }

    private fun topicNode(topic: Topic, c: ContentIndex, s: ActivitySnapshot): TopicNode {
        val items = c.discoveriesOf(topic.id)
        val landmarkId = items.firstOrNull { it.type == TreeConfig.LANDMARK_TYPE }?.id
        val leaves = items.map { d ->
            val done = d.id in s.completedIds
            LeafNode(
                discoveryId = d.id,
                title = if (done) d.title else null,
                completed = done,
                isLandmark = d.id == landmarkId,
                viaBonus = done && d.id in s.bonusCompletedIds,
            )
        }
        val completed = leaves.count { it.completed }
        return TopicNode(
            topic = topic,
            completed = completed,
            total = items.size,
            state = stateFor(completed, items.size),
            leaves = leaves,
            firstCompletedAtMs = leaves.filter { it.completed }.mapNotNull { s.firstCompletedAt[it.discoveryId] }.minOrNull(),
        )
    }
}
