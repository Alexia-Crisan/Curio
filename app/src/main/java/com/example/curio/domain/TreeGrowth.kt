package com.example.curio.domain

import java.time.LocalDate
import java.time.ZoneId

data class TreeGrowthSummary(
    val newLeafIds: Set<String>,
    val grownTopicIds: Set<String>,
    val stageBefore: GrowthStage,
    val stageAfter: GrowthStage,
) {
    val newLeafCount: Int get() = newLeafIds.size
    val stageChanged: Boolean get() = stageBefore != stageAfter
}

object TreeGrowth {

    fun since(tree: ThemeTree, sinceMs: Long?): TreeGrowthSummary {
        if (sinceMs == null) return summarize(tree) { false }
        return summarize(tree) { it > sinceMs }
    }

    fun on(tree: ThemeTree, day: LocalDate, zone: ZoneId): TreeGrowthSummary {
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return summarize(tree) { it in start until end }
    }

    private fun summarize(tree: ThemeTree, isNew: (Long) -> Boolean): TreeGrowthSummary {
        val newLeaves = tree.branches.flatMap { branch ->
            branch.leaves
                .filter { leaf -> leaf.completed && leaf.completedAtMs != null && isNew(leaf.completedAtMs) }
                .map { leaf -> branch.topic.id to leaf.discoveryId }
        }
        val before = tree.completed - newLeaves.size
        return TreeGrowthSummary(
            newLeafIds = newLeaves.map { it.second }.toSet(),
            grownTopicIds = newLeaves.map { it.first }.toSet(),
            stageBefore = TreeBuilder.stageFor(before, tree.total),
            stageAfter = TreeBuilder.stageFor(tree.completed, tree.total),
        )
    }
}

data class NextVisit(val date: LocalDate, val topicId: String)

data class EndCardTeaser(
    val leavesToday: Int,
    val stageChangedToday: Boolean,
    val stageNow: GrowthStage,
    val next: NextVisit?,
) {
    val hasContent: Boolean get() = leavesToday > 0 || next != null
}

object TreeTeaser {
    const val DEFAULT_HORIZON_DAYS = 60

    fun nextVisit(
        tree: ThemeTree,
        mode: DailyMode,
        after: LocalDate,
        horizonDays: Int = DEFAULT_HORIZON_DAYS,
    ): NextVisit? {
        val topicIds = tree.branches.map { it.topic.id }
        val unfinished = tree.branches.filter { it.state != NodeState.COMPLETE && it.total > 0 }.map { it.topic.id }.toSet()
        if (unfinished.isEmpty()) return null
        for (offset in 1..horizonDays) {
            val date = after.plusDays(offset.toLong())
            val topicId = TopicSelector.select(mode, date, tree.theme.id, topicIds, null) ?: return null
            if (topicId in unfinished) return NextVisit(date, topicId)
        }
        return null
    }
}

object TeaserBuilder {
    fun forFeed(
        trees: List<ThemeTree>,
        themeId: String,
        mode: DailyMode,
        date: LocalDate,
        zone: ZoneId,
    ): EndCardTeaser? {
        val tree = trees.firstOrNull { it.theme.id == themeId } ?: return null
        val today = TreeGrowth.on(tree, date, zone)
        return EndCardTeaser(
            leavesToday = today.newLeafCount,
            stageChangedToday = today.stageChanged,
            stageNow = tree.stage,
            next = TreeTeaser.nextVisit(tree, mode, date),
        )
    }
}
