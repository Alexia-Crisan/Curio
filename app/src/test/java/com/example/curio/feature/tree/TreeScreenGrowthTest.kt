package com.example.curio.feature.tree

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.example.curio.domain.Clock
import com.example.curio.domain.TestContent
import com.example.curio.domain.snapshotOf
import com.example.curio.testing.FakeActivitySource
import com.example.curio.testing.FakeCurrentTheme
import com.example.curio.testing.FakeTreeVisitStore
import com.example.curio.testing.MainDispatcherRule
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TreeScreenGrowthTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private lateinit var activity: FakeActivitySource
    private lateinit var theme: FakeCurrentTheme
    private lateinit var store: FakeTreeVisitStore

    private val fixedClock = object : Clock {
        override fun today(): LocalDate = LocalDate.of(2026, 10, 26)
        override fun now(): Instant = Instant.ofEpochMilli(5_000L)
    }

    @Before
    fun setUp() {
        activity = FakeActivitySource()
        theme = FakeCurrentTheme()
        store = FakeTreeVisitStore()
    }

    private fun createVm(last: Long? = null) =
        TreeScreenViewModel(activity, theme, TestContent.index, null, FakeTreeVisitStore(last).also { store = it }, fixedClock)

    private suspend fun ready(vm: TreeScreenViewModel) =
        vm.state.first { !it.isLoading }

    @Test
    fun noStoredVisitGivesNoGrowth() = runTest {
        activity.snapshot.value = snapshotOf(completed = setOf("a1"), firstAt = mapOf("a1" to 100L))
        val vm = createVm(null)
        val state = ready(vm)
        assertEquals(0, state.growth?.newLeafCount ?: 0)
    }

    @Test
    fun growthSinceTheStoredVisit() = runTest {
        activity.snapshot.value = snapshotOf(completed = setOf("a1", "a2"), firstAt = mapOf("a1" to 100L, "a2" to 300L))
        val vm = createVm(200L)
        val state = ready(vm)
        assertNotNull(state.growth)
        assertEquals(setOf("a2"), state.growth!!.newLeafIds)
    }

    @Test
    fun growthFollowsTheSelectedTheme() = runTest {
        activity.snapshot.value = snapshotOf(completed = setOf("a1", "s1"), firstAt = mapOf("a1" to 300L, "s1" to 300L))
        val vm = createVm(200L)
        ready(vm)
        vm.onThemeSelected("space")
        val state = vm.state.first { it.selectedThemeId == "space" }
        assertEquals(setOf("s1"), state.growth?.newLeafIds)
    }

    @Test
    fun aLeafCompletedWhileOpenCountsAsNew() = runTest {
        activity.snapshot.value = snapshotOf(completed = setOf("a1"), firstAt = mapOf("a1" to 100L))
        val vm = createVm(200L)
        ready(vm)
        activity.snapshot.value = snapshotOf(completed = setOf("a1", "a3"), firstAt = mapOf("a1" to 100L, "a3" to 400L))
        val state = vm.state.first { it.growth?.newLeafIds?.contains("a3") == true }
        val growth = state.growth
        assertNotNull(growth)
        assertTrue(growth!!.newLeafIds.contains("a3"))
        assertFalse(growth.newLeafIds.contains("a1"))
    }

    @Test
    fun leavingMarksTheVisitWithTheClockTime() = runTest {
        val vm = createVm(null)
        vm.onLeave()
        assertEquals(listOf(5000L), store.marks)
    }

    @Test
    fun leavingTwiceMarksTwice() = runTest {
        val vm = createVm(null)
        vm.onLeave()
        vm.onLeave()
        assertEquals(listOf(5000L, 5000L), store.marks)
    }

    @Test
    fun theDefaultStoreNeverFailsOnLeave() {
        val vm = TreeScreenViewModel(activity, theme, TestContent.index, null)
        vm.onLeave()
    }
}
