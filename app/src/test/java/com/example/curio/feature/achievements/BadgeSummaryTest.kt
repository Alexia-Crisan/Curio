package com.example.curio.feature.achievements

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.curio.domain.AchievementId
import com.example.curio.testing.FakeAchievementSource
import com.example.curio.testing.badge

class BadgeSummaryTest {
    private val fake = FakeAchievementSource()

    @Test fun countsUnlockedAndTotal() = runBlocking {
        fake.progress.value = listOf(
            badge(AchievementId.FIRST_DISCOVERY, 1, unlockedAt = 100),
            badge(AchievementId.CURIOUS_MIND, 3),
            badge(AchievementId.DEEP_LEARNER, 0),
        )
        val s = fake.observeSummary().first()
        assertEquals(1, s.unlocked)
        assertEquals(3, s.total)
    }

    @Test fun hasNewFollowsTheUnseenCount() = runBlocking {
        fake.unseen.value = 1
        assertTrue(fake.observeSummary().first().hasNew)

        fake.unseen.value = 0
        assertFalse(fake.observeSummary().first().hasNew)
    }

    @Test fun badgesWithoutVisualsAreIgnored() = runBlocking {
        fake.progress.value = listOf(
            badge(AchievementId.FIRST_DISCOVERY, 0),
            badge(AchievementId.THEME_EXPLORER, 2, unlockedAt = 100),
        )
        val s = fake.observeSummary().first()
        assertEquals(1, s.total)
        assertEquals(0, s.unlocked)
    }

    @Test fun updatesWhenProgressChanges() = runBlocking {
        fake.progress.value = listOf(badge(AchievementId.FIRST_DISCOVERY, 0))
        assertEquals(0, fake.observeSummary().first().unlocked)

        fake.progress.value = listOf(badge(AchievementId.FIRST_DISCOVERY, 1, unlockedAt = 100))
        assertEquals(1, fake.observeSummary().first { it.unlocked == 1 }.unlocked)
    }
}
