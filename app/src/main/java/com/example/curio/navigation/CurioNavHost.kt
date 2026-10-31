package com.example.curio.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.curio.di.AppContainer
import com.example.curio.domain.AchievementSource
import com.example.curio.domain.PastDayPolicy
import com.example.curio.domain.PastDayTarget
import com.example.curio.domain.SourcesBuilder
import com.example.curio.feature.achievements.AchievementsScreen
import com.example.curio.feature.achievements.AchievementsViewModel
import com.example.curio.feature.achievements.BadgeSummary
import com.example.curio.feature.achievements.UnlockBannerHost
import com.example.curio.feature.achievements.observeSummary
import com.example.curio.feature.feed.FeedScreen
import com.example.curio.feature.feed.FeedViewModel
import com.example.curio.feature.feed.PastDayScreen
import com.example.curio.feature.library.LibraryScreen
import com.example.curio.feature.library.LibraryViewModel
import com.example.curio.feature.onboarding.OnboardingScreen
import com.example.curio.feature.onboarding.OnboardingViewModel
import com.example.curio.feature.progress.ProgressScreen
import com.example.curio.feature.progress.ProgressViewModel
import com.example.curio.feature.progress.TreeViewModel
import com.example.curio.feature.settings.SettingsScreen
import com.example.curio.feature.settings.SettingsViewModel
import com.example.curio.feature.settings.SourcesScreen
import com.example.curio.feature.tree.TreeScreen
import com.example.curio.feature.tree.TreeScreenEvent
import com.example.curio.feature.tree.TreeScreenViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

data class NavTab(val route: Route, val label: String, val icon: ImageVector)

val navTabs = listOf(
    NavTab(Route.Feed, "Feed", Icons.Default.Home),
    NavTab(Route.Library, "Library", Icons.Default.Favorite),
    NavTab(Route.Progress, "Progress", Icons.Default.List),
    NavTab(Route.Settings, "Settings", Icons.Default.Settings),
)

@Composable
fun CurioNavHost(
    container: AppContainer,
    feedViewModel: FeedViewModel,
    treeViewModel: TreeViewModel,
    achievementSource: AchievementSource,
    onboardingDone: Boolean,
    onFinishOnboarding: () -> Unit,
    shakeEnabled: Boolean,
    onShakeToggle: (Boolean) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsToggle: (Boolean) -> Unit,
    tiltEnabled: Boolean = true,
    onTiltToggle: (Boolean) -> Unit = {},
    darkMode: String = "SYSTEM",
    onDarkModeChange: (String) -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val startDestination: Route = if (onboardingDone) Route.Feed else Route.Onboarding
    val onAchievements = navBackStackEntry?.destination?.hasRoute<Route.Achievements>() == true
    val onFeedDay = navBackStackEntry?.destination?.hasRoute<Route.FeedDay>() == true

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (currentRoute != null && !currentRoute.contains("Onboarding") && !currentRoute.contains("Sources") && !currentRoute.contains("Tree") && !onAchievements && !onFeedDay) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        navTabs.forEach { tab ->
                            val selected = currentRoute.contains(tab.label)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable<Route.Onboarding> {
                    val onboardingVm: OnboardingViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer { OnboardingViewModel(container.availableThemes, container.settingsStore) }
                        }
                    )
                    OnboardingScreen(
                        viewModel = onboardingVm,
                        onFinished = {
                            onFinishOnboarding()
                            navController.navigate(Route.Feed) {
                                popUpTo(Route.Onboarding) { inclusive = true }
                            }
                        }
                    )
                }

                composable<Route.Feed> {
                    val shakes = remember { container.shakeSensor.shakes() }
                    FeedScreen(
                        viewModel = feedViewModel,
                        shakes = shakes,
                        shakeEnabled = shakeEnabled,
                        onShakeDetected = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    )
                }

                composable<Route.Library> {
                    val vm: LibraryViewModel = viewModel(factory = viewModelFactory {
                        initializer { LibraryViewModel(container.userActivity, container.contentIndex) }
                    })
                    val saved by vm.saved.collectAsStateWithLifecycle()
                    val history by vm.history.collectAsStateWithLifecycle()
                    LibraryScreen(
                        savedCards = saved,
                        history = history,
                        onCardClick = { id -> navController.navigate(Route.Detail(id)) },
                    )
                }

                composable<Route.Progress> {
                    val progressVm: ProgressViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer { ProgressViewModel(container.progress, container.userActivity, container.javaClock) }
                        }
                    )
                    val monthProgress by progressVm.monthProgress.collectAsStateWithLifecycle()
                    val savedCount by progressVm.savedCount.collectAsStateWithLifecycle()

                    val treeUiState by treeViewModel.uiState.collectAsStateWithLifecycle()
                    val badgeSummaryFlow = remember(achievementSource) { achievementSource.observeSummary() }
                    val badgeSummary by badgeSummaryFlow.collectAsStateWithLifecycle(initialValue = BadgeSummary(0, 7, false))
                    val scope = rememberCoroutineScope()

                    ProgressScreen(
                        treeUiState = treeUiState,
                        badgeSummary = badgeSummary,
                        onOpenAchievements = {
                            navController.navigate(Route.Achievements) { launchSingleTop = true }
                        },
                        onSelectTheme = treeViewModel::selectTheme,
                        onToggleTopicExpanded = treeViewModel::toggleTopicExpanded,
                        onLeafClick = { id ->
                            navController.navigate(Route.Detail(id))
                        },
                        onOpenTree = {
                            navController.navigate(Route.Tree(treeUiState.selectedThemeId)) { launchSingleTop = true }
                        },
                        month = monthProgress,
                        savedCount = savedCount,
                        today = progressVm.today(),
                        onPreviousMonth = progressVm::previousMonth,
                        onNextMonth = progressVm::nextMonth,
                        onDayClick = { date ->
                            scope.launch {
                                val today = LocalDate.now(container.javaClock)
                                val hasChoice = container.dayPlans.peek(date) != null
                                when (val target = PastDayPolicy.targetFor(date, today, hasChoice)) {
                                    PastDayTarget.Live -> navController.navigate(Route.Feed) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    is PastDayTarget.Past -> navController.navigate(Route.FeedDay(target.date.toString())) { launchSingleTop = true }
                                    PastDayTarget.None -> Unit
                                }
                            }
                        },
                    )
                }

                composable<Route.FeedDay> { entry ->
                    val route = entry.toRoute<Route.FeedDay>()
                    val date = remember(route.date) { runCatching { LocalDate.parse(route.date) }.getOrNull() }
                    if (date == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        val vm: FeedViewModel = viewModel(factory = FeedViewModel.factory(container, date))
                        PastDayScreen(date = date, onBack = { navController.popBackStack() }) {
                            val shakes = remember { container.shakeSensor.shakes() }
                            FeedScreen(
                                viewModel = vm,
                                shakes = shakes,
                                shakeEnabled = shakeEnabled,
                                onShakeDetected = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            )
                        }
                    }
                }

                composable<Route.Achievements> {
                    val achVm: AchievementsViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer { AchievementsViewModel(achievementSource) }
                        }
                    )
                    AchievementsScreen(
                        viewModel = achVm,
                        onBack = { navController.popBackStack() },
                        onGoToFeed = {
                            navController.navigate(Route.Feed) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                composable<Route.Settings> {
                    val settingsVm: SettingsViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer { SettingsViewModel(container.availableThemes, container.settingsStore, container.progressResetter) }
                        }
                    )
                    SettingsScreen(
                        viewModel = settingsVm,
                        onOpenSources = { navController.navigate(Route.Sources) { launchSingleTop = true } },
                        shakeEnabled = shakeEnabled,
                        onShakeToggle = onShakeToggle,
                        hapticsEnabled = hapticsEnabled,
                        onHapticsToggle = onHapticsToggle,
                        tiltEnabled = tiltEnabled,
                        onTiltToggle = onTiltToggle,
                        darkMode = darkMode,
                        onDarkModeChange = onDarkModeChange,
                    )
                }

                composable<Route.Sources> {
                    val entries = remember { SourcesBuilder.build(container.contentIndex.discoveries) }
                    SourcesScreen(
                        entries = entries,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<Route.Tree> { entry ->
                    val route = entry.toRoute<Route.Tree>()
                    val vm: TreeScreenViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                TreeScreenViewModel(
                                    container.userActivity,
                                    container.currentTheme,
                                    container.contentIndex,
                                    route.themeId,
                                    container.treeVisits,
                                    container.clock,
                                )
                            }
                        }
                    )
                    DisposableEffect(vm) { onDispose { vm.onLeave() } }
                    val ui by vm.state.collectAsStateWithLifecycle()
                    LaunchedEffect(vm) {
                        vm.events.collect { event ->
                            when (event) {
                                is TreeScreenEvent.OpenDetail -> navController.navigate(Route.Detail(event.discoveryId))
                            }
                        }
                    }
                    TreeScreen(
                        state = ui,
                        onBack = { navController.popBackStack() },
                        onThemeSelected = vm::onThemeSelected,
                        onTopicTap = vm::onTopicTap,
                        onLeafTap = { topicId, leaf -> vm.onLeafTap(topicId, leaf.discoveryId) },
                        onSheetDismiss = vm::onSheetDismiss,
                        onOpenDetail = vm::onOpenDetail,
                    )
                }
            }
        }

        UnlockBannerHost(
            source = achievementSource,
            suppress = onAchievements,
            onOpenAchievements = { navController.navigate(Route.Achievements) { launchSingleTop = true } },
            onShown = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
        )
    }
}
