package com.example.curio.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.curio.domain.Discovery
import com.example.curio.feature.feed.FeedScreen
import com.example.curio.feature.feed.FeedViewModel
import com.example.curio.feature.library.LibraryScreen
import com.example.curio.feature.onboarding.OnboardingScreen
import com.example.curio.feature.progress.ProgressScreen
import com.example.curio.feature.progress.TreeViewModel
import com.example.curio.feature.settings.SettingsScreen

data class NavTab(val route: Route, val label: String, val iconText: String)

val navTabs = listOf(
    NavTab(Route.Feed, "Feed", "🔥"),
    NavTab(Route.Library, "Library", "📚"),
    NavTab(Route.Progress, "Progress", "📊"),
    NavTab(Route.Settings, "Settings", "⚙️"),
)

@Composable
fun CurioNavHost(
    feedViewModel: FeedViewModel,
    treeViewModel: TreeViewModel,
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

    Scaffold(
        bottomBar = {
            if (currentRoute != null && !currentRoute.contains("Onboarding")) {
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
                OnboardingScreen(
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
                ProgressScreen(
                    treeUiState = treeUiState,
                    onSelectTheme = treeViewModel::selectTheme,
                    onToggleTopicExpanded = treeViewModel::toggleTopicExpanded,
                    onLeafClick = { id ->
                        navController.navigate(Route.Detail(id))
                    }
                )
            }

            composable<Route.Settings> {
                SettingsScreen(
                    shakeEnabled = shakeEnabled,
                    onShakeToggle = onShakeToggle,
                    hapticsEnabled = hapticsEnabled,
                    onHapticsToggle = onHapticsToggle,
                )
            }
        }
    }
}
