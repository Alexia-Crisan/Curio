package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistedNamesTest {

    @Test fun viewSourceNamesAndPrioritiesAreFixed() {
        assertEquals(listOf("FEED", "BONUS", "DETAIL"), ViewSource.entries.map { it.name })
        assertEquals(listOf(3, 2, 1), ViewSource.entries.map { it.priority })
    }

    @Test fun dailyModeNamesAreFixed() {
        assertEquals(listOf("GUIDED", "CHOOSE", "SURPRISE"), DailyMode.entries.map { it.name })
    }

    @Test fun achievementIdsOnlyEverGrowAtTheEnd() {
        val original = listOf(
            "FIRST_DISCOVERY", "FULL_DAY", "CURIOUS_MIND", "TOPIC_EXPLORER", "KNOWLEDGE_COLLECTOR",
            "DEEP_LEARNER", "HIDDEN_DISCOVERIES", "CONSISTENT_CURIOSITY", "THEME_EXPLORER",
        )
        assertEquals(original, AchievementId.entries.map { it.name }.take(original.size))
    }

    @Test fun theMvpBadgeSetIsFixed() {
        val mvp = AchievementConfig.defs.filter { it.mvp }.map { it.id.name }.toSet()
        assertEquals(
            setOf(
                "FIRST_DISCOVERY", "FULL_DAY", "CURIOUS_MIND", "TOPIC_EXPLORER",
                "KNOWLEDGE_COLLECTOR", "DEEP_LEARNER", "HIDDEN_DISCOVERIES",
            ),
            mvp,
        )
    }

    @Test fun everyBadgeHasADefinition() {
        assertEquals(AchievementId.entries.toSet(), AchievementConfig.defs.map { it.id }.toSet())
    }

    @Test fun badgeTargetsAreFixed() {
        val targets = AchievementConfig.defs.associate { it.id.name to it.target }
        assertEquals(
            mapOf(
                "FIRST_DISCOVERY" to 1, "FULL_DAY" to 1, "CURIOUS_MIND" to 25, "TOPIC_EXPLORER" to 3,
                "KNOWLEDGE_COLLECTOR" to 10, "DEEP_LEARNER" to 5, "HIDDEN_DISCOVERIES" to 3,
                "CONSISTENT_CURIOSITY" to 5, "THEME_EXPLORER" to 2,
            ),
            targets,
        )
    }

    @Test fun theContentTypeUsedAsTheTreeLandmarkExists() {
        assertTrue(DiscoveryType.entries.any { it == TreeConfig.LANDMARK_TYPE })
        assertEquals("KNOWLEDGE", TreeConfig.LANDMARK_TYPE.name)
    }

    @Test fun productConstantsAreFixed() {
        assertEquals(6, DAILY_FEED_SIZE)
        assertEquals(3_000L, ViewEventRecorder.COMPLETE_AT_MS)
        assertEquals(1_500L, ViewEventRecorder.SKIP_BELOW_MS)
        assertEquals(3, TopicProgress.EXPLORED_AT)
        assertEquals(3, ShakeBonusSelector.MAX_BONUS_PER_DAY)
        assertEquals(12, TreeConfig.SAPLING_FROM_PERCENT)
        assertEquals(33, TreeConfig.TREE_FROM_PERCENT)
        assertEquals(66, TreeConfig.MATURE_FROM_PERCENT)
        assertEquals(3_000L, DwellTracker.CHECKPOINT_MS)
        assertEquals(120_000L, DwellTracker.MAX_DELTA_MS)
        assertEquals(60, TreeTeaser.DEFAULT_HORIZON_DAYS)
    }

    @Test fun badgeConfigConstantsAreFixed() {
        assertEquals(25, AchievementConfig.CURIOUS_MIND_TARGET)
        assertEquals(3, AchievementConfig.TOPIC_EXPLORER_TARGET)
        assertEquals(10, AchievementConfig.KNOWLEDGE_COLLECTOR_TARGET)
        assertEquals(5, AchievementConfig.DEEP_LEARNER_TARGET)
        assertEquals(3, AchievementConfig.HIDDEN_DISCOVERIES_TARGET)
        assertEquals(5, AchievementConfig.CONSISTENT_CURIOSITY_TARGET)
        assertEquals(2, AchievementConfig.THEME_EXPLORER_TARGET)
        assertEquals(3, AchievementConfig.THEME_EXPLORED_AT)
    }

    @Test fun theFreshFirstSwitchIsOnByDefault() {
        assertTrue(FeedConfig.FRESH_FIRST)
    }
}
