package com.example.curio.feature.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.example.curio.domain.DailyMode
import com.example.curio.domain.TestContent
import com.example.curio.domain.UserSettings
import com.example.curio.testing.FakeResetter
import com.example.curio.testing.FakeSettingsStore
import com.example.curio.testing.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private lateinit var store: FakeSettingsStore
    private lateinit var resetter: FakeResetter
    private lateinit var vm: SettingsViewModel

    @Before
    fun setUp() {
        store = FakeSettingsStore(UserSettings("animals", DailyMode.GUIDED))
        resetter = FakeResetter()
        vm = SettingsViewModel(TestContent.index.themes, store, resetter)
    }

    @Test
    fun uiReflectsTheStore() = runTest {
        val ui = vm.ui.value
        assertEquals("animals", ui.themeId)
        assertEquals(DailyMode.GUIDED, ui.mode)
        assertFalse(ui.resetDialogVisible)
        assertFalse(ui.resetting)
    }

    @Test
    fun uiFollowsStoreChanges() = runTest {
        store.settings.value = UserSettings("space", DailyMode.SURPRISE)
        val ui = vm.ui.value
        assertEquals("space", ui.themeId)
        assertEquals(DailyMode.SURPRISE, ui.mode)
    }

    @Test
    fun selectingAnAvailableThemeWritesIt() = runTest {
        vm.onThemeSelected("space")
        assertEquals(listOf("theme:space"), store.calls)
    }

    @Test
    fun selectingTheCurrentThemeWritesNothing() = runTest {
        vm.onThemeSelected("animals")
        assertTrue(store.calls.isEmpty())
    }

    @Test
    fun selectingAnUnavailableThemeWritesNothing() = runTest {
        vm.onThemeSelected("oceans")
        assertTrue(store.calls.isEmpty())
    }

    @Test
    fun selectingAModeWritesIt() = runTest {
        vm.onModeSelected(DailyMode.CHOOSE)
        assertEquals(listOf("mode:CHOOSE"), store.calls)
    }

    @Test
    fun selectingTheCurrentModeWritesNothing() = runTest {
        vm.onModeSelected(DailyMode.GUIDED)
        assertTrue(store.calls.isEmpty())
    }

    @Test
    fun requestingAResetOnlyShowsTheDialog() = runTest {
        vm.onResetRequested()
        assertTrue(vm.ui.value.resetDialogVisible)
        assertEquals(0, resetter.count)
    }

    @Test
    fun cancellingTheDialogHidesItWithoutResetting() = runTest {
        vm.onResetRequested()
        vm.onResetCancelled()
        assertFalse(vm.ui.value.resetDialogVisible)
        assertEquals(0, resetter.count)
    }

    @Test
    fun confirmingResetsOnceAndReports() = runTest {
        val events = mutableListOf<SettingsEvent>()
        backgroundScope.launch { vm.events.toList(events) }

        vm.onResetRequested()
        vm.onResetConfirmed()
        yield()

        assertEquals(1, resetter.count)
        assertFalse(vm.ui.value.resetDialogVisible)
        assertFalse(vm.ui.value.resetting)
        assertEquals(listOf(SettingsEvent.ResetDone), events)
    }

    @Test
    fun confirmingWithoutTheDialogDoesNothing() = runTest {
        vm.onResetConfirmed()
        assertEquals(0, resetter.count)
    }

    @Test
    fun confirmingTwiceResetsOnce() = runTest {
        vm.onResetRequested()
        vm.onResetConfirmed()
        vm.onResetConfirmed()
        assertEquals(1, resetter.count)
    }

    @Test
    fun aFailedResetReportsFailureAndRecovers() = runTest {
        val events = mutableListOf<SettingsEvent>()
        backgroundScope.launch { vm.events.toList(events) }

        resetter.fail = true
        vm.onResetRequested()
        vm.onResetConfirmed()
        yield()

        assertEquals(listOf(SettingsEvent.ResetFailed), events)
        assertFalse(vm.ui.value.resetting)
        assertFalse(vm.ui.value.resetDialogVisible)
    }
}
