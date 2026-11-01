package com.example.curio.feature.detail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.example.curio.domain.TestContent
import com.example.curio.domain.TouchQueue
import com.example.curio.domain.ViewSource
import com.example.curio.domain.ViewTouch
import com.example.curio.testing.FakeDetailSaves
import com.example.curio.testing.MainDispatcherRule
import com.example.curio.testing.MutableClock
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @get:Rule val main = MainDispatcherRule(dispatcher)

    private val base = Instant.parse("2026-10-26T09:00:00Z").toEpochMilli()
    private val today = LocalDate.of(2026, 10, 26)
    private val clock = MutableClock(base)
    private val recorded = mutableListOf<ViewTouch>()
    private val queue = TouchQueue(CoroutineScope(UnconfinedTestDispatcher())) { touch -> recorded += touch }
    private val saves = FakeDetailSaves()

    private fun vm(id: String = "a6") = DetailViewModel(id, TestContent.index, saves, queue, clock)
    private fun dwell() = recorded.sumOf { it.detailDwellDeltaMs }

    private fun advance(ms: Long) {
        clock.nowMs += ms
        dispatcher.scheduler.advanceTimeBy(ms)
        dispatcher.scheduler.runCurrent()
    }

    @Test fun theFirstVisibleCompletesTheCardAndLaterOnesDoNot() {
        val vm = vm()
        vm.onVisible(); vm.onHidden(); vm.onVisible(); vm.onHidden()
        assertEquals(1, recorded.count { it.markCompleted })
        assertTrue(recorded.all { it.source == ViewSource.DETAIL && it.date == today })
    }

    @Test fun hiddenWritesTheVisibleTime() {
        val vm = vm()
        vm.onVisible()
        clock.nowMs += 5_000
        vm.onHidden()
        assertEquals(5_000L, dwell())
    }

    @Test fun checkpointsEveryThreeSecondsWhileVisible() {
        val vm = vm()
        vm.onVisible()
        dispatcher.scheduler.runCurrent()
        advance(3_000)
        assertEquals(3_000L, dwell())
        advance(3_000)
        assertEquals(6_000L, dwell())
        clock.nowMs += 500
        vm.onHidden()
        assertEquals(6_500L, dwell())
    }

    @Test fun noCheckpointsAfterHidden() {
        val vm = vm()
        vm.onVisible()
        dispatcher.scheduler.runCurrent()
        vm.onHidden()
        recorded.clear()
        advance(9_000)
        assertTrue(recorded.isEmpty())
    }

    @Test fun anUnknownDiscoveryWritesNothing() {
        val vm = vm("not-in-the-dataset")
        assertNull(vm.discovery.value)
        vm.onVisible()
        clock.nowMs += 5_000
        vm.onHidden()
        assertTrue(recorded.isEmpty())
    }

    @Test fun toggleSaveFlipsTheSavedStateUsingTodaysDate() {
        val vm = vm()
        vm.toggleSave()
        dispatcher.scheduler.runCurrent()
        vm.toggleSave()
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf(Triple("a6", true, today), Triple("a6", false, today)), saves.calls)
    }
}
