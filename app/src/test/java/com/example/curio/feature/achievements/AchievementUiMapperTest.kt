package com.example.curio.feature.achievements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.curio.domain.AchievementConfig
import com.example.curio.domain.AchievementId
import com.example.curio.testing.badge

class AchievementUiMapperTest {

    @Test fun lockedBadgeKeepsProgressAndHasNoDate() {
        val ui = badge(AchievementId.KNOWLEDGE_COLLECTOR, 4).toUi { "date" }!!
        assertEquals(4, ui.current)
        assertEquals(10, ui.target)
        assertFalse(ui.isUnlocked)
        assertNull(ui.unlockedOn)
    }

    @Test fun unlockedBadgeShowsFullProgressAndTheFormattedDate() {
        val ui = badge(AchievementId.CURIOUS_MIND, 25, unlockedAt = 777).toUi { "on-$it" }!!
        assertTrue(ui.isUnlocked)
        assertEquals("on-777", ui.unlockedOn)
        assertEquals(1f, ui.fraction, 0.001f)
    }

    @Test fun fractionIsCurrentOverTarget() {
        val ui = badge(AchievementId.KNOWLEDGE_COLLECTOR, 4).toUi { "" }!!
        assertEquals(0.4f, ui.fraction, 0.001f)
    }

    @Test fun badgesWithoutVisualsAreSkipped() {
        val ui = badge(AchievementId.CONSISTENT_CURIOSITY, 2).toUi { "" }
        assertNull(ui)
    }

    @Test fun everyMvpBadgeHasVisuals() {
        val mvpDefs = AchievementConfig.defs.filter { it.mvp }
        val missing = mvpDefs.filter { AchievementVisuals.of(it.id) == null }.map { it.id }
        assertTrue("Missing visuals for MVP badges: $missing", missing.isEmpty())
    }
}
