package com.example.curio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.curio.feature.feed.FeedViewModel
import com.example.curio.feature.progress.TreeViewModel
import com.example.curio.navigation.CurioNavHost
import com.example.curio.ui.theme.CurioTheme
import kotlinx.coroutines.launch

private object NoHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CurioApp).container

        setContent {
            val darkMode by container.settingsRepository.darkMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")
            val dark = when (darkMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            CurioTheme(darkTheme = dark) {
                val scope = rememberCoroutineScope()
                val feedVm: FeedViewModel = viewModel(
                    factory = FeedViewModel.factory(container)
                )

                val treeVm: TreeViewModel = viewModel(
                    factory = TreeViewModel.Factory(
                        userActivityRepository = container.userActivity,
                        contentIndex = container.contentIndex,
                    )
                )

                val onboardingDone by container.settingsRepository.onboardingDone.collectAsStateWithLifecycle(initialValue = false)
                val shakeEnabled by container.settingsRepository.shakeEnabled.collectAsStateWithLifecycle(initialValue = true)
                val hapticsEnabled by container.settingsRepository.hapticsEnabled.collectAsStateWithLifecycle(initialValue = true)
                val tiltEnabled by container.settingsRepository.tiltEnabled.collectAsStateWithLifecycle(initialValue = true)

                val systemHaptics = LocalHapticFeedback.current
                CompositionLocalProvider(
                    LocalHapticFeedback provides if (hapticsEnabled) systemHaptics else NoHaptics
                ) {
                    CurioNavHost(
                        container = container,
                        feedViewModel = feedVm,
                        treeViewModel = treeVm,
                        achievementSource = container.achievementSource,
                        onboardingDone = onboardingDone,
                        onFinishOnboarding = {
                            scope.launch { container.settingsRepository.setOnboardingDone(true) }
                        },
                        shakeEnabled = shakeEnabled,
                        onShakeToggle = { enabled ->
                            scope.launch { container.settingsRepository.setShakeEnabled(enabled) }
                        },
                        hapticsEnabled = hapticsEnabled,
                        onHapticsToggle = { enabled ->
                            scope.launch { container.settingsRepository.setHapticsEnabled(enabled) }
                        },
                        tiltEnabled = tiltEnabled,
                        onTiltToggle = { enabled ->
                            scope.launch { container.settingsRepository.setTiltEnabled(enabled) }
                        },
                        darkMode = darkMode,
                        onDarkModeChange = { mode ->
                            scope.launch { container.settingsRepository.setDarkMode(mode) }
                        },
                    )
                }
            }
        }
    }
}
