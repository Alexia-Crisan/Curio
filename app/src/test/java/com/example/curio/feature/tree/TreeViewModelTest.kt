package com.example.curio.feature.tree

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.LeafNode
import com.example.curio.domain.NodeState
import com.example.curio.domain.TestContent
import com.example.curio.domain.idsOf
import com.example.curio.domain.snapshotOf
import com.example.curio.testing.FakeActivitySource
import com.example.curio.testing.FakeCurrentTheme
import com.example.curio.testing.MainDispatcherRule

class TreeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val c = TestContent.index
    private val activity = FakeActivitySource()
    private val theme = FakeCurrentTheme()

    private fun vm(index: ContentIndex = c) = TreeViewModel(activity, theme, index)
    private suspend fun TreeViewModel.ready() = state.first { !it.isLoading }
    private fun TreeUiState.t1() = selected!!.branches.first { it.topic.id == "t1" }

    @Test fun startsLoadingThenShowsOneTreePerAvailableTheme() = runTest {
        val vm = vm()
        assertTrue(vm.state.value.isLoading)
        val s = vm.ready()
        assertEquals(listOf("animals", "space"), s.trees.map { it.theme.id })
        assertFalse(s.isEmpty)
    }

    @Test fun selectsTheUsersCurrentThemeByDefault() = runTest {
        theme.theme.value = "space"
        assertEquals("space", vm().ready().selected?.theme?.id)
    }

    @Test fun anUnavailableCurrentThemeFallsBackToTheFirstTree() = runTest {
        theme.theme.value = "oceans"
        assertEquals("animals", vm().ready().selected?.theme?.id)
    }

    @Test fun noCurrentThemeFallsBackToTheFirstTree() = runTest {
        assertEquals("animals", vm().ready().selected?.theme?.id)
    }

    @Test fun tappingAThemeChipOverridesTheCurrentTheme() = runTest {
        theme.theme.value = "animals"
        val vm = vm(); vm.ready()
        vm.onThemeSelected("space")
        assertEquals("space", vm.state.first { it.selectedThemeId == "space" }.selected?.theme?.id)
    }

    @Test fun changingTheCurrentThemeResetsTheChipChoice() = runTest {
        val sea = TestContent.theme("sea", "z1")
        val index = ContentIndex(c.themes + sea, c.discoveries + (1..6).map { TestContent.disc("z$it", "sea", "z1") })
        theme.theme.value = "animals"
        val vm = vm(index); vm.ready()
        vm.onThemeSelected("space")
        vm.state.first { it.selectedThemeId == "space" }
        theme.theme.value = "sea"
        assertEquals("sea", vm.state.first { it.selectedThemeId == "sea" }.selectedThemeId)
    }

    @Test fun selectingAnUnknownThemeIsIgnored() = runTest {
        theme.theme.value = "space"
        val vm = vm(); vm.ready()
        vm.onThemeSelected("oceans")
        assertEquals("space", vm.state.value.selected?.theme?.id)
    }

    @Test fun tappingATopicExpandsAndCollapsesIt() = runTest {
        val vm = vm(); vm.ready()
        vm.onTopicClick("t1")
        assertTrue("t1" in vm.state.first { "t1" in it.expandedTopicIds }.expandedTopicIds)
        vm.onTopicClick("t1")
        assertFalse("t1" in vm.state.first { "t1" !in it.expandedTopicIds }.expandedTopicIds)
    }

    @Test fun expansionSurvivesSnapshotChanges() = runTest {
        val vm = vm(); vm.ready()
        vm.onTopicClick("t1")
        activity.snapshot.value = snapshotOf(completed = setOf("a1"))
        val s = vm.state.first { it.selected != null && it.t1().completed == 1 }
        assertTrue("t1" in s.expandedTopicIds)
    }

    @Test fun tappingACompletedLeafOpensItsDetail() = runTest {
        val vm = vm()
        vm.onLeafClick(LeafNode("a1", "a1", completed = true, isLandmark = false, viaBonus = false))
        assertEquals(TreeEvent.OpenDetail("a1"), vm.events.first())
    }

    @Test fun tappingAnUndiscoveredLeafDoesNothing() = runTest {
        val vm = vm()
        vm.onLeafClick(LeafNode("a2", null, completed = false, isLandmark = false, viaBonus = false))
        assertNull(withTimeoutOrNull(1_000) { vm.events.first() })
    }

    @Test fun theTreeUpdatesWhenTheSnapshotChanges() = runTest {
        val vm = vm()
        assertEquals(NodeState.LOCKED, vm.ready().t1().state)
        activity.snapshot.value = snapshotOf(completed = idsOf("a", 1..3).toSet())
        val s = vm.state.first { it.selected != null && it.t1().state == NodeState.GROWING }
        assertEquals(3, s.t1().completed)
    }

    @Test fun noAvailableThemesMeansAnEmptyState() = runTest {
        val onlyUnavailable = ContentIndex(listOf(TestContent.theme("oceans", available = false)), emptyList())
        val s = vm(onlyUnavailable).ready()
        assertTrue(s.isEmpty)
        assertNull(s.selected)
    }
}
