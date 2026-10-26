package com.example.curio.feature.onboarding

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.example.curio.domain.DailyMode
import com.example.curio.domain.TestContent
import com.example.curio.testing.FakeSettingsStore
import com.example.curio.testing.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private lateinit var store: FakeSettingsStore
    private lateinit var vm: OnboardingViewModel

    @Before
    fun setUp() {
        store = FakeSettingsStore()
        vm = OnboardingViewModel(TestContent.index.themes, store)
    }

    @Test
    fun startsWithNothingSelected() = runTest {
        val ui = vm.ui.value
        assertNull(ui.selectedThemeId)
        assertEquals(DailyMode.GUIDED, ui.mode)
        assertFalse(ui.canStart)
        assertEquals(3, ui.themes.size)
    }

    @Test
    fun selectingAnAvailableThemeEnablesStart() = runTest {
        vm.selectTheme("animals")
        val ui = vm.ui.value
        assertEquals("animals", ui.selectedThemeId)
        assertTrue(ui.canStart)
    }

    @Test
    fun selectingAnUnavailableThemeIsIgnored() = runTest {
        vm.selectTheme("oceans")
        assertNull(vm.ui.value.selectedThemeId)
        assertFalse(vm.ui.value.canStart)
    }

    @Test
    fun selectingAnUnknownThemeIsIgnored() = runTest {
        vm.selectTheme("nope")
        assertNull(vm.ui.value.selectedThemeId)
    }

    @Test
    fun selectingAnotherThemeReplacesTheFirst() = runTest {
        vm.selectTheme("animals")
        vm.selectTheme("space")
        assertEquals("space", vm.ui.value.selectedThemeId)
    }

    @Test
    fun selectModeUpdatesTheState() = runTest {
        vm.selectMode(DailyMode.SURPRISE)
        assertEquals(DailyMode.SURPRISE, vm.ui.value.mode)
    }

    @Test
    fun startWithoutAThemeDoesNothing() = runTest {
        vm.start()
        assertTrue(store.calls.isEmpty())
    }

    @Test
    fun startSavesThemeAndModeOnceAndFinishes() = runTest {
        val events = mutableListOf<OnboardingEvent>()
        backgroundScope.launch { vm.events.toList(events) }

        vm.selectTheme("animals")
        vm.selectMode(DailyMode.CHOOSE)
        vm.start()
        yield()

        assertEquals(listOf("onboarding:animals:CHOOSE"), store.calls)
        assertEquals(listOf(OnboardingEvent.Finished), events)
    }

    @Test
    fun startTwiceSavesOnce() = runTest {
        val events = mutableListOf<OnboardingEvent>()
        backgroundScope.launch { vm.events.toList(events) }

        vm.selectTheme("animals")
        vm.start()
        vm.start()
        yield()
        assertEquals(1, store.calls.size)
    }

    @Test
    fun aFailedSaveAllowsARetry() = runTest {
        val events = mutableListOf<OnboardingEvent>()
        backgroundScope.launch { vm.events.toList(events) }

        store.failOnComplete = true
        vm.selectTheme("animals")
        vm.start()
        assertFalse(vm.ui.value.saving)
        assertTrue(vm.ui.value.canStart)

        store.failOnComplete = false
        vm.start()
        yield()
        assertEquals(listOf(OnboardingEvent.Finished), events)
    }
}
