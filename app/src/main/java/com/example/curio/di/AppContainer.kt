package com.example.curio.di

import android.content.Context
import com.example.curio.data.RealClock
import com.example.curio.data.fake.FakeDiscoveryRepository
import com.example.curio.data.fake.FakeInteractionRepository
import com.example.curio.data.settings.PreferencesSettingsStore
import com.example.curio.data.settings.PreferencesTreeVisitStore
import com.example.curio.data.settings.SettingsRepository
import com.example.curio.data.user.AchievementRepository
import com.example.curio.data.user.AchievementTracker
import com.example.curio.data.user.BonusRepository
import com.example.curio.data.user.CurioDatabase
import com.example.curio.data.user.DayPlanRepository
import com.example.curio.data.user.ProgressRepository
import com.example.curio.data.user.UserActivityRepository
import com.example.curio.domain.AchievementSource
import com.example.curio.domain.Clock
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.CurrentThemeSource
import com.example.curio.domain.DailyModeSource
import com.example.curio.domain.DiscoveryRepository
import com.example.curio.domain.InteractionRepository
import com.example.curio.domain.ProgressResetter
import com.example.curio.domain.SettingsStore
import com.example.curio.domain.TestContent
import com.example.curio.domain.Theme
import com.example.curio.domain.TouchQueue
import com.example.curio.domain.TreeVisitStore
import com.example.curio.hardware.ShakeSensor
import com.example.curio.hardware.TiltSensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val javaClock: java.time.Clock = java.time.Clock.systemDefaultZone()
    val clock: Clock = RealClock()

    val discoveryRepository: DiscoveryRepository = FakeDiscoveryRepository()
    val interactionRepository: InteractionRepository = FakeInteractionRepository()
    val settingsRepository: SettingsRepository = SettingsRepository(context)
    val shakeSensor: ShakeSensor = ShakeSensor(context)
    val tiltSensor: TiltSensor = TiltSensor(context)

    val database: CurioDatabase = CurioDatabase.create(context)
    val contentIndex: ContentIndex = TestContent.index

    val dayPlans: DayPlanRepository = DayPlanRepository(database, javaClock)
    val progress: ProgressRepository = ProgressRepository(database, contentIndex.membership)
    val bonus: BonusRepository = BonusRepository(database, contentIndex.discoveries, javaClock)
    val userActivity: UserActivityRepository = UserActivityRepository(database, contentIndex, javaClock)
    val achievements: AchievementRepository = AchievementRepository(database, userActivity, contentIndex, javaClock)
    val achievementSource: AchievementSource get() = achievements
    val achievementTracker: AchievementTracker = AchievementTracker(appScope, userActivity, achievements)

    val settingsStore: SettingsStore = PreferencesSettingsStore(settingsRepository)
    val treeVisits: TreeVisitStore = PreferencesTreeVisitStore(settingsRepository, appScope)
    val progressResetter = ProgressResetter {
        userActivity.resetAll()
        treeVisits.clear()
    }
    val currentTheme: CurrentThemeSource = CurrentThemeSource { settingsStore.observe().map { it.themeId }.distinctUntilChanged() }
    val dailyMode: DailyModeSource = DailyModeSource { settingsStore.observe().map { it.mode }.distinctUntilChanged() }
    val availableThemes: List<Theme> get() = contentIndex.themes

    val touchQueue: TouchQueue = TouchQueue(appScope) { touch -> database.viewEventDao().record(touch) }
}
