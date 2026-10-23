package com.example.curio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.curio.feature.feed.FeedViewModel
import com.example.curio.feature.progress.TreeViewModel
import com.example.curio.navigation.CurioNavHost
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CurioApp).container

        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                val feedVm: FeedViewModel = viewModel(
                    factory = FeedViewModel.Factory(
                        container.discoveryRepository,
                        container.interactionRepository,
                        container.clock,
                    )
                )

                val treeVm: TreeViewModel = viewModel(
                    factory = TreeViewModel.Factory(
                        userActivityRepository = container.userActivityRepository,
                        contentIndex = container.contentIndex,
                    )
                )

                val onboardingDone by container.settingsRepository.onboardingDone.collectAsStateWithLifecycle(initialValue = false)
                val shakeEnabled by container.settingsRepository.shakeEnabled.collectAsStateWithLifecycle(initialValue = true)
                val hapticsEnabled by container.settingsRepository.hapticsEnabled.collectAsStateWithLifecycle(initialValue = true)

                CurioNavHost(
                    feedViewModel = feedVm,
                    treeViewModel = treeVm,
                    savedCards = emptyList(),
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
                )
            }
        }
    }
}
