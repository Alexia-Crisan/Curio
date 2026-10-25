package com.example.curio.domain

object TopicProgress {
    const val EXPLORED_AT = 3

    fun completedByTopic(completedIds: Set<String>, c: ContentIndex): Map<String, Int> {
        val counts = c.topics.associate { it.id to 0 }.toMutableMap()
        for (id in completedIds) {
            val topicId = c.discovery(id)?.topicId ?: continue
            if (topicId in counts) counts[topicId] = counts.getValue(topicId) + 1
        }
        return counts
    }

    fun exploredTopicsByTheme(completedIds: Set<String>, c: ContentIndex): Map<String, Int> {
        val byTopic = completedByTopic(completedIds, c)
        return c.themes.associate { th -> th.id to th.topics.count { (byTopic[it.id] ?: 0) >= EXPLORED_AT } }
    }

    fun completedByTheme(completedIds: Set<String>, c: ContentIndex): Map<String, Int> =
        c.themes.associate { th -> th.id to completedIds.count { c.discovery(it)?.themeId == th.id } }
}
