package com.example.curio.domain

object AchievementEvaluator {

    /**
     * @param unlocked achievementId -> unlockedAt (epoch ms), read from the achievement_unlock table in step 6.
     * Unlocking is permanent: an unlocked badge reports target/target whatever the data says now.
     */
    fun evaluate(
        s: ActivitySnapshot,
        c: ContentIndex,
        defs: List<AchievementDef> = AchievementConfig.defs,
        unlocked: Map<AchievementId, Long> = emptyMap(),
    ): Evaluation {
        val explored = TopicProgress.exploredTopicsByTheme(s.completedIds, c)
        val byTheme = TopicProgress.completedByTheme(s.completedIds, c)
        val raw = defs.associate { it.id to rawValue(it.id, s, explored, byTheme) }

        val progress = defs.map { def ->
            val at = unlocked[def.id]
            if (at != null) AchievementProgress(def, def.target, at)
            else AchievementProgress(def, minOf(raw.getValue(def.id), def.target), null)   // never show 40 / 25
        }
        val newlyEarned = defs.filter { it.id !in unlocked && raw.getValue(it.id) >= it.target }
        return Evaluation(progress, newlyEarned)
    }

    private fun rawValue(id: AchievementId, s: ActivitySnapshot, explored: Map<String, Int>, byTheme: Map<String, Int>): Int =
        when (id) {
            AchievementId.FIRST_DISCOVERY -> minOf(1, s.completedIds.size)
            AchievementId.FULL_DAY -> if (s.completedDays.isNotEmpty()) 1 else 0
            AchievementId.CURIOUS_MIND -> s.completedIds.size
            AchievementId.TOPIC_EXPLORER -> explored.values.maxOrNull() ?: 0        // best single theme, never summed
            AchievementId.KNOWLEDGE_COLLECTOR -> s.savedIds.size
            AchievementId.DEEP_LEARNER -> s.deepReadIds.size
            AchievementId.HIDDEN_DISCOVERIES -> s.bonusCompletedIds.size
            AchievementId.CONSISTENT_CURIOSITY -> s.completedFeedDays.size
            AchievementId.THEME_EXPLORER -> byTheme.values.count { it >= AchievementConfig.THEME_EXPLORED_AT }
        }
}
