package com.example.curio.domain

object TopicProgress {
    /** A topic counts as "explored" once this many of its discoveries are completed. */
    const val EXPLORED_AT = 3

    /** topicId -> number of completed discoveries. Every declared topic is listed, even with 0. */
    fun completedByTopic(completedIds: Set<String>, c: ContentIndex): Map<String, Int> {
        val counts = c.topics.associate { it.id to 0 }.toMutableMap()
        for (id in completedIds) {
            val topicId = c.discovery(id)?.topicId ?: continue          // unknown ids are ignored
            if (topicId in counts) counts[topicId] = counts.getValue(topicId) + 1
        }
        return counts
    }

    /** themeId -> number of explored topics (>= EXPLORED_AT completed). */
    fun exploredTopicsByTheme(completedIds: Set<String>, c: ContentIndex): Map<String, Int> {
        val byTopic = completedByTopic(completedIds, c)
        return c.themes.associate { th -> th.id to th.topics.count { (byTopic[it.id] ?: 0) >= EXPLORED_AT } }
    }

    /** themeId -> number of completed discoveries in that theme. */
    fun completedByTheme(completedIds: Set<String>, c: ContentIndex): Map<String, Int> =
        c.themes.associate { th -> th.id to completedIds.count { c.discovery(it)?.themeId == th.id } }
}
