package com.example.curio.feature.achievements

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import com.example.curio.domain.AchievementId
import com.example.curio.testing.FakeAchievementSource
import com.example.curio.testing.MainDispatcherRule
import com.example.curio.testing.badge

class AchievementsViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val fake = FakeAchievementSource()
    private fun vm() = AchievementsViewModel(fake) { "date-$it" }

    private val twoBadges = listOf(
        badge(AchievementId.FIRST_DISCOVERY, 1, unlockedAt = 5),
        badge(AchievementId.CURIOUS_MIND, 10),
    )

    @Test fun oneCardPerActiveBadgeInDefinitionOrder() = runBlocking {
        fake.progress.value = twoBadges
        val state = vm().state.first { it.items.isNotEmpty() }
        assertEquals(listOf(AchievementId.FIRST_DISCOVERY, AchievementId.CURIOUS_MIND), state.items.map { it.id })
        assertEquals(1, state.unlockedCount)
        assertEquals(2, state.total)
    }

    @Test fun unlockedBadgeShowsTheFormattedDate() = runBlocking {
        fake.progress.value = twoBadges
        val state = vm().state.first { it.items.isNotEmpty() }
        assertEquals("date-5", state.items[0].unlockedOn)
    }

    @Test fun lockedBadgeHasNoDate() = runBlocking {
        fake.progress.value = twoBadges
        val state = vm().state.first { it.items.isNotEmpty() }
        assertNull(state.items[1].unlockedOn)
    }

    @Test fun clickingABadgeOpensItsSheetAndDismissClosesIt() = runBlocking {
        fake.progress.value = twoBadges
        val v = vm()
        v.onBadgeClick(AchievementId.CURIOUS_MIND)
        assertEquals(AchievementId.CURIOUS_MIND, v.state.first { it.selected != null }.selected?.id)

        v.onDismissSheet()
        assertNull(v.state.first { it.selected == null }.selected)
    }

    @Test fun selectingABadgeThatIsNotShownSelectsNothing() = runBlocking {
        fake.progress.value = twoBadges
        val v = vm()
        v.onBadgeClick(AchievementId.THEME_EXPLORER)
        assertNull(v.state.value.selected)
    }

    @Test fun unseenUnlocksAreMarkedSeenWhenTheScreenOpens() = runBlocking {
        fake.unseen.value = 2
        vm()
        assertEquals(1, fake.seenCalls)
        assertEquals(0, fake.unseen.value)
    }

    @Test fun anUnlockWhileTheScreenIsOpenIsMarkedSeenToo() = runBlocking {
        vm()
        fake.unseen.value = 1
        assertEquals(1, fake.seenCalls)
    }

    @Test fun badgesWithoutVisualsAreNotShown() = runBlocking {
        fake.progress.value = listOf(
            badge(AchievementId.FIRST_DISCOVERY, 1),
            badge(AchievementId.CONSISTENT_CURIOSITY, 2),
        )
        val state = vm().state.first { it.items.isNotEmpty() }
        assertEquals(listOf(AchievementId.FIRST_DISCOVERY), state.items.map { it.id })
    }

    @Test fun theStateFollowsProgressChanges() = runBlocking {
        fake.progress.value = listOf(badge(AchievementId.CURIOUS_MIND, 10))
        val v = vm()
        assertEquals(10, v.state.first { it.items.isNotEmpty() }.items[0].current)

        fake.progress.value = listOf(badge(AchievementId.CURIOUS_MIND, 20))
        assertEquals(20, v.state.first { it.items.first().current == 20 }.items[0].current)
    }
}
