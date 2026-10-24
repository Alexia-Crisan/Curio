package com.example.curio.domain

/** Persisted by NAME (step 6). Never rename a value; add new ones at the end. */
enum class AchievementId {
    FIRST_DISCOVERY, FULL_DAY, CURIOUS_MIND, TOPIC_EXPLORER, KNOWLEDGE_COLLECTOR,
    DEEP_LEARNER, HIDDEN_DISCOVERIES, CONSISTENT_CURIOSITY, THEME_EXPLORER,
}

enum class AchievementKind { BINARY, INCREMENTAL }

data class AchievementDef(val id: AchievementId, val target: Int, val kind: AchievementKind, val mvp: Boolean)

data class AchievementProgress(val def: AchievementDef, val current: Int, val unlockedAt: Long?) {
    val isUnlocked: Boolean get() = unlockedAt != null
    val fraction: Float get() = (current.toFloat() / def.target).coerceIn(0f, 1f)
}

data class Evaluation(
    val progress: List<AchievementProgress>,
    val newlyEarned: List<AchievementDef>,      // earned now but not yet in the unlocked map: the tracker persists these
)

object AchievementConfig {
    const val CURIOUS_MIND_TARGET = 25
    const val TOPIC_EXPLORER_TARGET = 3          // explored topics in ONE theme
    const val KNOWLEDGE_COLLECTOR_TARGET = 10
    const val DEEP_LEARNER_TARGET = 5
    const val HIDDEN_DISCOVERIES_TARGET = 3
    const val CONSISTENT_CURIOSITY_TARGET = 5    // distinct days, not consecutive
    const val THEME_EXPLORER_TARGET = 2          // themes
    const val THEME_EXPLORED_AT = 3              // completed discoveries for a theme to count

    private val I = AchievementKind.INCREMENTAL
    private val B = AchievementKind.BINARY

    val defs: List<AchievementDef> = listOf(
        AchievementDef(AchievementId.FIRST_DISCOVERY, 1, B, mvp = true),
        AchievementDef(AchievementId.FULL_DAY, 1, B, mvp = true),
        AchievementDef(AchievementId.CURIOUS_MIND, CURIOUS_MIND_TARGET, I, mvp = true),
        AchievementDef(AchievementId.TOPIC_EXPLORER, TOPIC_EXPLORER_TARGET, I, mvp = true),
        AchievementDef(AchievementId.KNOWLEDGE_COLLECTOR, KNOWLEDGE_COLLECTOR_TARGET, I, mvp = true),
        AchievementDef(AchievementId.DEEP_LEARNER, DEEP_LEARNER_TARGET, I, mvp = true),
        AchievementDef(AchievementId.HIDDEN_DISCOVERIES, HIDDEN_DISCOVERIES_TARGET, I, mvp = true),
        AchievementDef(AchievementId.CONSISTENT_CURIOSITY, CONSISTENT_CURIOSITY_TARGET, I, mvp = false),   // Should-have
        AchievementDef(AchievementId.THEME_EXPLORER, THEME_EXPLORER_TARGET, I, mvp = false),               // Should-have
    )
}
