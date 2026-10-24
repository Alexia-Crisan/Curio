package com.example.curio.feature.achievements

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.curio.domain.AchievementSource
import com.example.curio.ui.components.UnlockBanner

private const val BANNER_MS = 3_500L
private const val EXIT_MS = 300L

@Composable
fun UnlockBannerHost(
    source: AchievementSource,
    suppress: Boolean,
    onOpenAchievements: () -> Unit,
    modifier: Modifier = Modifier,
    onShown: () -> Unit = {},
) {
    val pendingFlow = remember(source) { source.observeNextPending() }
    val pending by pendingFlow.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    val animate = remember { ValueAnimator.areAnimatorsEnabled() }

    val def = pending
    val visual = def?.let { AchievementVisuals.of(it.id) }

    LaunchedEffect(def?.id, suppress) {
        if (def == null) { visible = false; return@LaunchedEffect }
        if (suppress || visual == null) { source.markNotified(def.id); return@LaunchedEffect }
        visible = true
        onShown()
        delay(BANNER_MS)
        visible = false
        delay(EXIT_MS)
        source.markNotified(def.id)
    }

    Box(modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = visible && def != null && visual != null,
            enter = if (animate) slideInVertically { -it } + fadeIn() else EnterTransition.None,
            exit = if (animate) slideOutVertically { -it } + fadeOut() else ExitTransition.None,
        ) {
            if (def != null && visual != null) {
                UnlockBanner(
                    title = stringResource(visual.title),
                    iconRes = visual.icon,
                    onClick = { scope.launch { source.markNotified(def.id) }; onOpenAchievements() },
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}
