package com.example.curio.feature.tree

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.example.curio.domain.AchievementConfig
import com.example.curio.domain.AchievementDef
import com.example.curio.domain.AchievementProgress
import com.example.curio.domain.BadgeTreeLayout
import com.example.curio.domain.TestContent
import com.example.curio.testing.FakeActivitySource
import com.example.curio.testing.FakeCurrentTheme
import com.example.curio.testing.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class TreeScreenBadgesTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val activity = FakeActivitySource()
    private val theme = FakeCurrentTheme()
    private val badges = MutableStateFlow<List<AchievementProgress>>(emptyList())

    private fun progress(def: AchievementDef, unlockedAt: Long?) =
        AchievementProgress(def, if (unlockedAt != null) def.target else 0, unlockedAt)

    @Test
    fun badgesAreEmptyWhenNoSourceIsGiven() = runTest {
        val vm = TreeScreenViewModel(activity, theme, TestContent.index, null)
        val state = vm.state.first { !it.isLoading }
        assertTrue(state.badges.isEmpty())
    }

    @Test
    fun badgesFollowTheSource() = runTest {
        val vm = TreeScreenViewModel(activity, theme, TestContent.index, null, badgeProgress = badges)
        assertEquals(0, vm.state.first { !it.isLoading }.badges.size)
        val defs = AchievementConfig.defs.filter { it.mvp }
        badges.value = defs.take(2).mapIndexed { index, def -> progress(def, 100L + index) }
        val state = vm.state.first { it.badges.size == 2 }
        assertEquals(2, BadgeTreeLayout.levelOf(state.badges))
    }
}
