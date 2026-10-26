package com.example.curio.feature.tree

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.example.curio.domain.GrowthStage
import com.example.curio.domain.NextStep
import com.example.curio.domain.NodeState
import com.example.curio.domain.TestContent
import com.example.curio.domain.snapshotOf
import com.example.curio.testing.FakeActivitySource
import com.example.curio.testing.FakeCurrentTheme
import com.example.curio.testing.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class TreeScreenViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private lateinit var activity: FakeActivitySource
    private lateinit var theme: FakeCurrentTheme

    @Before
    fun setUp() {
        activity = FakeActivitySource()
        theme = FakeCurrentTheme()
    }

    private fun createVm(initial: String? = null) =
        TreeScreenViewModel(activity, theme, TestContent.index, initial)

    private suspend fun ready(vm: TreeScreenViewModel) =
        vm.state.first { !it.isLoading }

    private fun done(vararg ids: String) {
        activity.snapshot.value = snapshotOf(completed = ids.toSet())
    }

    @Test
    fun startsLoading() {
        val vm = createVm()
        assertTrue(vm.state.value.isLoading)
    }

    @Test
    fun showsOneTreePerAvailableTheme() = runTest {
        val vm = createVm()
        val state = ready(vm)
        assertEquals(listOf("animals", "space"), state.trees.map { it.theme.id })
    }

    @Test
    fun theInitialThemeIsSelected() = runTest {
        val vm = createVm("space")
        val state = ready(vm)
        assertEquals("space", state.selectedThemeId)
    }

    @Test
    fun anInvalidInitialThemeFallsBackToTheCurrentOne() = runTest {
        theme.theme.value = "space"
        val vm = createVm("oceans")
        val state = ready(vm)
        assertEquals("space", state.selectedThemeId)
    }

    @Test
    fun noInitialAndNoCurrentGivesTheFirst() = runTest {
        val vm = createVm()
        val state = ready(vm)
        assertEquals("animals", state.selectedThemeId)
    }

    @Test
    fun tappingATopicOpensItsSheet() = runTest {
        val vm = createVm()
        ready(vm)
        vm.onTopicTap("t1")
        val state = vm.state.first { it.sheet != null }
        assertTrue(state.sheet is NodeSheet.TopicSheet)
        val sheet = state.sheet as NodeSheet.TopicSheet
        assertEquals("t1", sheet.node.topic.id)
        assertEquals(NextStep.StartTopic(3), sheet.explanation.next)
    }

    @Test
    fun tappingAHiddenLeafOpensASheetWithoutATitle() = runTest {
        val vm = createVm()
        ready(vm)
        vm.onLeafTap("t1", "a1")
        val state = vm.state.first { it.sheet != null }
        val sheet = state.sheet as NodeSheet.LeafSheet
        assertNull(sheet.explanation.title)
        assertFalse(sheet.explanation.canOpenDetail)
    }

    @Test
    fun tappingADoneLeafOpensASheetWithItsTitle() = runTest {
        done("a1")
        val vm = createVm()
        ready(vm)
        vm.onLeafTap("t1", "a1")
        val state = vm.state.first { it.sheet != null }
        val sheet = state.sheet as NodeSheet.LeafSheet
        assertNotNull(sheet.explanation.title)
        assertTrue(sheet.explanation.canOpenDetail)
    }

    @Test
    fun tappingAnUnknownLeafOpensNothing() = runTest {
        val vm = createVm()
        ready(vm)
        vm.onLeafTap("t1", "zzz")
        assertNull(vm.state.value.sheet)
    }

    @Test
    fun tappingAnUnknownTopicOpensNothing() = runTest {
        val vm = createVm()
        ready(vm)
        vm.onTopicTap("zzz")
        assertNull(vm.state.value.sheet)
    }

    @Test
    fun theSheetUpdatesWhenProgressChanges() = runTest {
        val vm = createVm()
        ready(vm)
        vm.onTopicTap("t1")
        done("a1", "a2", "a3")
        val state = vm.state.first { (it.sheet as? NodeSheet.TopicSheet)?.explanation?.state == NodeState.GROWING }
        val sheet = state.sheet as NodeSheet.TopicSheet
        assertEquals(NodeState.GROWING, sheet.explanation.state)
    }

    @Test
    fun dismissingClosesTheSheet() = runTest {
        val vm = createVm()
        ready(vm)
        vm.onTopicTap("t1")
        vm.state.first { it.sheet != null }
        vm.onSheetDismiss()
        assertNull(vm.state.value.sheet)
    }

    @Test
    fun changingThemeClosesTheSheet() = runTest {
        val vm = createVm()
        ready(vm)
        vm.onTopicTap("t1")
        vm.state.first { it.sheet != null }
        vm.onThemeSelected("space")
        assertNull(vm.state.value.sheet)
        assertEquals("space", vm.state.value.selectedThemeId)
    }

    @Test
    fun aSheetOfAnotherThemeDoesNotShowAfterSwitching() = runTest {
        val vm = createVm()
        ready(vm)
        vm.onTopicTap("t1")
        vm.state.first { it.sheet != null }
        vm.onThemeSelected("space")
        vm.onThemeSelected("animals")
        assertNull(vm.state.value.sheet)
    }

    @Test
    fun openDetailOnADoneLeafEmitsAndCloses() = runTest {
        done("a1")
        val vm = createVm()
        ready(vm)
        val events = mutableListOf<TreeScreenEvent>()
        backgroundScope.launch { vm.events.toList(events) }

        vm.onLeafTap("t1", "a1")
        vm.state.first { it.sheet != null }
        vm.onOpenDetail("a1")
        yield()

        assertEquals(listOf(TreeScreenEvent.OpenDetail("a1")), events)
        assertNull(vm.state.value.sheet)
    }

    @Test
    fun openDetailOnAHiddenLeafDoesNothing() = runTest {
        val vm = createVm()
        ready(vm)
        val events = mutableListOf<TreeScreenEvent>()
        backgroundScope.launch { vm.events.toList(events) }

        vm.onLeafTap("t1", "a1")
        vm.state.first { it.sheet != null }
        vm.onOpenDetail("a1")
        yield()

        assertTrue(events.isEmpty())
        assertNotNull(vm.state.value.sheet)
    }

    @Test
    fun openDetailForAnotherCardDoesNothing() = runTest {
        done("a1", "a2")
        val vm = createVm()
        ready(vm)
        val events = mutableListOf<TreeScreenEvent>()
        backgroundScope.launch { vm.events.toList(events) }

        vm.onLeafTap("t1", "a1")
        vm.state.first { it.sheet != null }
        vm.onOpenDetail("a2")
        yield()

        assertTrue(events.isEmpty())
    }

    @Test
    fun openDetailWithoutALeafSheetDoesNothing() = runTest {
        val vm = createVm()
        ready(vm)
        val events = mutableListOf<TreeScreenEvent>()
        backgroundScope.launch { vm.events.toList(events) }

        vm.onTopicTap("t1")
        vm.state.first { it.sheet != null }
        vm.onOpenDetail("a1")
        yield()

        assertTrue(events.isEmpty())
    }

    @Test
    fun themeExplanationMatchesTheSelectedTree() = runTest {
        done("a1")
        val vm = createVm()
        val state = ready(vm)
        val e = state.themeExplanation
        assertNotNull(e)
        assertEquals(GrowthStage.SPROUT, e!!.stage)
        assertEquals(GrowthStage.SAPLING, e.nextStage)
        assertEquals(2, e.cardsToNextStage)
    }

    @Test
    fun themeExplanationFollowsTheSelectedTree() = runTest {
        val vm = createVm()
        ready(vm)
        vm.onThemeSelected("space")
        val state = vm.state.first { it.selectedThemeId == "space" }
        assertEquals(12, state.themeExplanation?.total)
    }

    @Test
    fun changingTheCurrentThemeResetsTheManualChoice() = runTest {
        theme.theme.value = "animals"
        val vm = createVm()
        ready(vm)
        vm.onThemeSelected("space")
        vm.state.first { it.selectedThemeId == "space" }
        theme.theme.value = "space"
        theme.theme.value = "animals"
        val state = vm.state.first { it.selectedThemeId == "animals" }
        assertEquals("animals", state.selectedThemeId)
    }
}
