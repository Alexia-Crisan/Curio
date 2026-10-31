package com.example.curio.feature.feed

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.curio.data.user.BonusRepository
import com.example.curio.data.user.CurioDatabase
import com.example.curio.data.user.DayPlanRepository
import com.example.curio.data.user.FreshnessRepository
import com.example.curio.data.user.ProgressRepository
import com.example.curio.data.user.UserActivityRepository
import com.example.curio.data.user.toEntity
import com.example.curio.domain.CurrentThemeSource
import com.example.curio.domain.DailyMode
import com.example.curio.domain.DayChoice
import com.example.curio.domain.FeedItemKind
import com.example.curio.domain.TestContent
import com.example.curio.domain.TouchQueue
import com.example.curio.domain.ViewSource
import com.example.curio.domain.ViewTouch
import com.example.curio.testing.FakeDailyModeSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PastDayFeedViewModelTest {
    private lateinit var db: CurioDatabase
    private lateinit var scope: CoroutineScope
    private val clock = Clock.fixed(Instant.parse("2026-10-26T09:00:00Z"), ZoneId.of("Europe/Bucharest"))
    private val TODAY = LocalDate.of(2026, 10, 26)
    private val PAST = TODAY.minusDays(1)
    private val index = TestContent.index

    private lateinit var dayPlans: DayPlanRepository
    private lateinit var bonus: BonusRepository
    private lateinit var userActivity: UserActivityRepository
    private lateinit var freshness: FreshnessRepository
    private val written = mutableListOf<ViewTouch>()
    private lateinit var queue: TouchQueue
    private lateinit var currentTheme: CurrentThemeSource
    private lateinit var dailyMode: FakeDailyModeSource

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).allowMainThreadQueries().build()
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        dayPlans = DayPlanRepository(db, clock)
        bonus = BonusRepository(db, index.discoveries, clock)
        userActivity = UserActivityRepository(db, index, clock)
        freshness = FreshnessRepository(db)
        written.clear()
        queue = TouchQueue(scope = scope) { written += it }
        currentTheme = CurrentThemeSource { flowOf("animals") }
        dailyMode = FakeDailyModeSource()
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    private suspend fun seedPast() {
        db.dayChoiceDao().upsert(DayChoice(PAST, "animals", "t2", DailyMode.GUIDED).toEntity())
    }

    private fun count(table: String): Long =
        db.openHelper.writableDatabase.compileStatement("SELECT COUNT(*) FROM $table").simpleQueryForLong()

    private suspend fun settle() {
        delay(300L)
    }

    private fun awaitState(vm: FeedViewModel): FeedUiState {
        var s = vm.state.value
        val deadline = System.currentTimeMillis() + 5000L
        while (s is FeedUiState.Loading && System.currentTimeMillis() < deadline) {
            Thread.sleep(50L)
            s = vm.state.value
        }
        return s
    }

    private fun vm(date: LocalDate?): FeedViewModel {
        var result: FeedViewModel? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result = FeedViewModel(date, index, dayPlans, bonus, userActivity, queue, currentTheme, dailyMode, clock, freshness)
        }
        return result!!
    }

    @Test
    fun aPastDayWithAChoiceIsReadOnly() = runBlocking {
        seedPast()
        val viewModel = vm(PAST)
        val state = awaitState(viewModel) as FeedUiState.Ready
        assertTrue(state.readOnly)
        assertEquals("t2", state.topicId)
        assertEquals(PAST, state.date)
        assertFalse(state.settingsLocked)
    }

    @Test
    fun aPastDayShowsTheSixCardsOfItsTopic() = runBlocking {
        seedPast()
        val viewModel = vm(PAST)
        val state = awaitState(viewModel) as FeedUiState.Ready
        assertEquals(listOf("b1", "b2", "b3", "b4", "b5", "b6"), state.items.map { it.discovery.id })
        assertTrue(state.items.all { it.kind == FeedItemKind.REGULAR })
    }

    @Test
    fun aPastDayWithoutAChoiceIsMissing() = runBlocking {
        val viewModel = vm(PAST)
        val state = awaitState(viewModel)
        assertTrue(state is FeedUiState.PastDayMissing)
        assertEquals(PAST, (state as FeedUiState.PastDayMissing).date)
    }

    @Test
    fun aPastDayDoesNotNeedACurrentTheme() = runBlocking {
        currentTheme = CurrentThemeSource { flowOf(null) }
        seedPast()
        val viewModel = vm(PAST)
        val state = awaitState(viewModel)
        assertTrue(state is FeedUiState.Ready)
    }

    @Test
    fun aPastDayDoesNotCreateAChoiceRow() = runBlocking {
        val viewModel = vm(PAST)
        awaitState(viewModel)
        assertEquals(0L, count("day_choice"))
    }

    @Test
    fun aPastDayWritesNoViewEvents() = runBlocking {
        seedPast()
        val viewModel = vm(PAST)
        awaitState(viewModel)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel.onPageSettled(0)
            viewModel.onScreenPaused()
            viewModel.onScreenResumed()
            viewModel.onDetailOpened("b1")
        }
        settle()
        assertTrue(written.isEmpty())
        assertEquals(0L, count("view_event"))
    }

    @Test
    fun aPastDayIgnoresTheShake() = runBlocking {
        seedPast()
        val viewModel = vm(PAST)
        val stateBefore = awaitState(viewModel) as FeedUiState.Ready
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel.onShake()
        }
        settle()
        assertEquals(0L, count("view_event"))
        val stateAfter = viewModel.state.value as FeedUiState.Ready
        assertEquals(6, stateAfter.items.size)
        assertEquals(stateBefore.items, stateAfter.items)
    }

    @Test
    fun aPastDayIgnoresATopicChoice() = runBlocking {
        seedPast()
        val viewModel = vm(PAST)
        awaitState(viewModel)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel.onTopicChosen("t3")
        }
        settle()
        assertEquals("t2", dayPlans.peek(PAST)?.topicId)
        assertEquals(1L, count("day_choice"))
    }

    @Test
    fun aPastDayLeavesTheProgressUntouched() = runBlocking {
        seedPast()
        val viewModel = vm(PAST)
        awaitState(viewModel)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel.onPageSettled(0)
            viewModel.onDetailOpened("b1")
        }
        settle()
        val monthProg = ProgressRepository(db, index.membership).observeMonth(YearMonth.of(2026, 10)).first()
        assertEquals(0, monthProg.daysCompleted)
    }

    @Test
    fun aSaveOnAPastDayWritesOnlyTheSavedRow() = runBlocking {
        seedPast()
        val viewModel = vm(PAST)
        awaitState(viewModel)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel.onSave("b1", true)
        }
        settle()
        assertEquals(1L, count("saved_discovery"))
        assertEquals(0L, count("view_event"))
    }

    @Test
    fun aPastDayWithAnEarlierHistoryIsFreshFirst() = runBlocking {
        seedPast()
        db.viewEventDao().record(ViewTouch("b1", PAST.minusDays(7), ViewSource.FEED, 1000L, markCompleted = true))
        db.viewEventDao().record(ViewTouch("b2", PAST.minusDays(7), ViewSource.FEED, 1000L, markCompleted = true))
        val viewModel = vm(PAST)
        val state = awaitState(viewModel) as FeedUiState.Ready
        assertEquals(listOf("b3", "b4", "b5", "b6", "b1", "b2"), state.items.map { it.discovery.id })
    }

    @Test
    fun aPastDayHasNoTeaser() = runBlocking {
        seedPast()
        val viewModel = vm(PAST)
        awaitState(viewModel)
        settle()
        assertNull(viewModel.teaser.value)
    }

    @Test
    fun aDateEqualToTodayBehavesLive() = runBlocking {
        val viewModel = vm(TODAY)
        val state = awaitState(viewModel) as FeedUiState.Ready
        assertFalse(state.readOnly)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel.onPageSettled(0)
        }
        settle()
        assertTrue(written.isNotEmpty())
    }

    @Test
    fun aNullDateBehavesLive() = runBlocking {
        val viewModel = vm(null)
        val state = awaitState(viewModel) as FeedUiState.Ready
        assertFalse(state.readOnly)
    }

    @Test
    fun aPastDayDoesNotLeaveAnythingBehindAfterBeingCleared() = runBlocking {
        seedPast()
        val store = ViewModelStore()
        val factory = viewModelFactory {
            initializer {
                FeedViewModel(PAST, index, dayPlans, bonus, userActivity, queue, currentTheme, dailyMode, clock, freshness)
            }
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            ViewModelProvider(store, factory)[FeedViewModel::class.java]
            store.clear()
        }
        settle()
        assertTrue(written.isEmpty())
    }
}
