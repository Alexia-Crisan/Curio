package com.example.curio.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.curio.data.user.BonusRepository
import com.example.curio.data.user.DayPlanRepository
import com.example.curio.data.user.UserActivityRepository
import com.example.curio.di.AppContainer
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.CurrentThemeSource
import com.example.curio.domain.DailyMode
import com.example.curio.domain.DailyModeSource
import com.example.curio.domain.DayChoice
import com.example.curio.domain.DayPlan
import com.example.curio.domain.Discovery
import com.example.curio.domain.DwellTracker
import com.example.curio.domain.EndCardTeaser
import com.example.curio.domain.FeedAssembler
import com.example.curio.domain.FeedConfig
import com.example.curio.domain.FeedItem
import com.example.curio.domain.FreshnessSource
import com.example.curio.domain.ShakeBonusSelector
import com.example.curio.domain.TeaserBuilder
import com.example.curio.domain.TouchQueue
import com.example.curio.domain.TreeBuilder
import com.example.curio.domain.ViewSource
import com.example.curio.domain.ViewTouch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(
    private val forDate: LocalDate?,
    private val content: ContentIndex,
    private val dayPlans: DayPlanRepository,
    private val bonus: BonusRepository,
    private val activity: UserActivityRepository,
    private val touches: TouchQueue,
    private val currentTheme: CurrentThemeSource,
    private val dailyMode: DailyModeSource,
    private val clock: Clock,
    private val freshness: FreshnessSource = FreshnessSource.None,
) : ViewModel() {

    private val isLive: Boolean = forDate == null || forDate == LocalDate.now(clock)

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<FeedEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<FeedEvent> = _events.asSharedFlow()

    val savedIds: StateFlow<Set<String>> = activity.observeSavedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val teaser: StateFlow<EndCardTeaser?> = combine(
        activity.observeSnapshot().map { TreeBuilder.buildAll(content, it) },
        state,
        dailyMode.observe(),
    ) { trees, s, mode ->
        val ready = s as? FeedUiState.Ready
        if (ready == null || ready.readOnly) null else TeaserBuilder.forFeed(trees, ready.themeId, mode, ready.date, ZoneId.systemDefault())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var lastTheme: String? = null
    private var lastMode: DailyMode = DailyMode.GUIDED
    private var tracker: DwellTracker? = null
    private var checkpointJob: Job? = null
    private var bonusCards: List<Discovery> = emptyList()
    private val revisitIds = mutableSetOf<String>()
    private var loadedDate: LocalDate? = null
    private var seenBefore: Set<String> = emptySet()

    init {
        if (isLive) {
            viewModelScope.launch {
                combine(currentTheme.observe().distinctUntilChanged(), dailyMode.observe().distinctUntilChanged()) { t, m -> t to m }
                    .collect { (theme, mode) ->
                        lastTheme = theme
                        lastMode = mode
                        loadLive()
                    }
            }
        } else {
            viewModelScope.launch { loadPast(forDate!!) }
        }
    }

    private suspend fun loadLive() {
        val themeId = lastTheme
        if (themeId == null) {
            endTracking(); _state.value = FeedUiState.NoTheme; return
        }
        val topicIds = content.topicsOf(themeId).map { it.id }
        applyPlan(themeId, dayPlans.ensureToday(themeId, topicIds, lastMode))
    }

    private suspend fun applyPlan(themeId: String, plan: DayPlan) {
        when (plan) {
            is DayPlan.Ready -> publishReady(plan.choice, plan.settingsLocked)
            is DayPlan.NeedsChoice -> { endTracking(); _state.value = FeedUiState.NeedsChoice(content.topicsOf(themeId)) }
            DayPlan.NoContent -> { endTracking(); _state.value = FeedUiState.NoContent }
        }
    }

    private suspend fun publishReady(choice: DayChoice, locked: Boolean) {
        val today = LocalDate.now(clock)
        val previous = _state.value as? FeedUiState.Ready
        if (loadedDate != today || previous?.topicId != choice.topicId) {
            endTracking()
            if (loadedDate != today) { revisitIds.clear() }
        }
        loadedDate = today
        bonusCards = bonus.todaysBonusCards()
        seenBefore = if (FeedConfig.FRESH_FIRST) freshness.seenBefore(today) else emptySet()
        _state.value = FeedUiState.Ready(
            date = today,
            themeId = choice.themeId,
            topicId = choice.topicId,
            items = FeedAssembler.assemble(choice.topicId, content.discoveries, bonusCards, revisitIds, seenBefore),
            readOnly = false,
            settingsLocked = locked,
        )
    }

    private suspend fun loadPast(date: LocalDate) {
        val choice = dayPlans.peek(date)
        _state.value = if (choice == null) {
            FeedUiState.PastDayMissing(date)
        } else {
            FeedUiState.Ready(
                date = date, themeId = choice.themeId, topicId = choice.topicId,
                items = FeedAssembler.assemble(
                    choice.topicId,
                    content.discoveries,
                    completedBefore = if (FeedConfig.FRESH_FIRST) freshness.seenBefore(date) else emptySet(),
                ),
                readOnly = true, settingsLocked = false,
            )
        }
    }

    fun onPageSettled(index: Int) {
        val s = _state.value as? FeedUiState.Ready ?: return
        if (s.readOnly || !isLive) return
        val item = s.items.getOrNull(index) ?: return
        val tr = tracker?.takeIf { trackerDate == s.date } ?: DwellTracker(s.date).also { tracker = it; trackerDate = s.date }
        touches.offerAll(tr.show(item.discovery.id, item.source, clock.millis()))
        startCheckpoints(tr)
    }
    private var trackerDate: LocalDate? = null

    fun onScreenPaused() {
        checkpointJob?.cancel()
        tracker?.let { touches.offerAll(it.pause(clock.millis())) }
    }

    fun onScreenResumed() {
        tracker?.let { it.resume(clock.millis()); startCheckpoints(it) }
        if (isLive) viewModelScope.launch { loadLive() }
    }

    fun onDetailOpened(discoveryId: String) {
        val s = _state.value as? FeedUiState.Ready ?: return
        if (s.readOnly) return
        val item = s.items.firstOrNull { it.discovery.id == discoveryId } ?: return
        touches.offer(ViewTouch(discoveryId, s.date, item.source, clock.millis(), markCompleted = true))
    }

    fun onSave(discoveryId: String, saved: Boolean) {
        val s = _state.value as? FeedUiState.Ready ?: return
        val item = s.items.firstOrNull { it.discovery.id == discoveryId }
        viewModelScope.launch {
            activity.setSaved(
                discoveryId = discoveryId,
                saved = saved,
                source = item?.source ?: ViewSource.FEED,
                date = s.date,
                recordActivity = !s.readOnly,
            )
        }
    }

    fun onShake() {
        val s = _state.value as? FeedUiState.Ready ?: return
        if (s.readOnly || !isLive) return
        viewModelScope.launch {
            when (val r = bonus.reveal(s.themeId, s.topicId)) {
                is ShakeBonusSelector.Result.Found -> {
                    if (r.isRevisit) revisitIds += r.discovery.id
                    bonusCards = bonus.todaysBonusCards()
                    val items = FeedAssembler.assemble(s.topicId, content.discoveries, bonusCards, revisitIds, seenBefore)
                    _state.value = s.copy(items = items)
                    val index = items.indexOfFirst { it.discovery.id == r.discovery.id }
                    if (index >= 0) _events.emit(FeedEvent.ScrollTo(index))
                }
                ShakeBonusSelector.Result.CapReached -> _events.emit(FeedEvent.BonusCapReached)
                ShakeBonusSelector.Result.NothingLeft -> _events.emit(FeedEvent.BonusNothingLeft)
            }
        }
    }

    fun onTopicChosen(topicId: String) {
        val themeId = lastTheme ?: return
        if (!isLive) return
        viewModelScope.launch {
            endTracking()
            val topicIds = content.topicsOf(themeId).map { it.id }
            applyPlan(themeId, dayPlans.chooseTopic(themeId, topicIds, topicId))
        }
    }

    private fun startCheckpoints(tr: DwellTracker) {
        checkpointJob?.cancel()
        checkpointJob = viewModelScope.launch {
            while (true) {
                delay(DwellTracker.CHECKPOINT_MS)
                touches.offerAll(tr.checkpoint(clock.millis()))
            }
        }
    }

    private fun endTracking() {
        checkpointJob?.cancel()
        tracker?.let { touches.offerAll(it.leave(clock.millis())) }
        tracker = null
        trackerDate = null
    }

    override fun onCleared() {
        endTracking()
    }

    companion object {
        fun factory(container: AppContainer, forDate: LocalDate? = null): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FeedViewModel(
                    forDate = forDate,
                    content = container.contentIndex,
                    dayPlans = container.dayPlans,
                    bonus = container.bonus,
                    activity = container.userActivity,
                    touches = container.touchQueue,
                    currentTheme = container.currentTheme,
                    dailyMode = container.dailyMode,
                    clock = container.javaClock,
                    freshness = container.freshness,
                )
            }
        }
    }
}
