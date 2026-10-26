package com.example.curio.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.curio.domain.Discovery
import com.example.curio.domain.SourcesBuilder
import com.example.curio.feature.achievements.AchievementsScreen
import com.example.curio.feature.achievements.AchievementsViewModel
import com.example.curio.feature.achievements.BadgeSummary
import com.example.curio.feature.achievements.UnlockBannerHost
import com.example.curio.feature.achievements.observeSummary
import com.example.curio.feature.feed.FeedScreen
import com.example.curio.feature.feed.FeedViewModel
import com.example.curio.feature.library.LibraryScreen
import com.example.curio.feature.onboarding.OnboardingScreen
import com.example.curio.feature.onboarding.OnboardingViewModel
import com.example.curio.feature.progress.ProgressScreen
import com.example.curio.feature.progress.TreeViewModel
import com.example.curio.feature.settings.SettingsScreen
import com.example.curio.feature.settings.SettingsViewModel
import com.example.curio.feature.settings.SourcesScreen
import com.example.curio.feature.tree.TreeScreen
import com.example.curio.feature.tree.TreeScreenEvent
import com.example.curio.feature.tree.TreeScreenViewModel

data class NavTab(val route: Route, val label: String, val iconText: String)

val navTabs = listOf(
    NavTab(Route.Feed, "Feed", "🔥"),
    NavTab(Route.Library, "Library", "📚"),
    NavTab(Route.Progress, "Progress", "📊"),
    NavTab(Route.Settings, "Settings", "⚙️"),
)

@Composable
fun CurioNavHost(
    container: AppContainer,
    feedViewModel: FeedViewModel,
    treeViewModel: TreeViewModel,
    achievementSource: AchievementSource,
    savedCards: List<Discovery>,
    onboardingDone: Boolean,
    onFinishOnboarding: () -> Unit,
    shakeEnabled: Boolean,
    onShakeToggle: (Boolean) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsToggle: (Boolean) -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val startDestination: Route = if (onboardingDone) Route.Feed else Route.Onboarding
    val onAchievements = navBackStackEntry?.destination?.hasRoute<Route.Achievements>() == true

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (currentRoute != null && !currentRoute.contains("Onboarding") && !currentRoute.contains("Sources") && !currentRoute.contains("Tree") && !onAchievements) {
                    NavigationBar(containerColor = Color(0xFF0F172A)) {
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
                                icon = { Text(tab.iconText) },
                                label = { Text(tab.label, color = if (selected) Color.White else Color.Gray) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color(0xFF1E293B)
                                )
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
                    FeedScreen(viewModel = feedViewModel)
                }

                composable<Route.Library> {
                    LibraryScreen(
                        savedCards = savedCards,
                        onCardClick = { id ->
                            navController.navigate(Route.Detail(id))
                        }
                    )
                }

                composable<Route.Progress> {
                    val treeUiState by treeViewModel.uiState.collectAsStateWithLifecycle()
                    val badgeSummaryFlow = remember(achievementSource) { achievementSource.observeSummary() }
                    val badgeSummary by badgeSummaryFlow.collectAsStateWithLifecycle(initialValue = BadgeSummary(0, 7, false))

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
                        }
                    )
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
                                    route.themeId
                                )
                            }
                        }
                    )
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
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
        )
    }
}
