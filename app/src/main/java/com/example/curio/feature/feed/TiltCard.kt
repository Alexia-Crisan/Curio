package com.example.curio.feature.feed

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.curio.R
import com.example.curio.domain.Discovery
import com.example.curio.hardware.TiltState
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

const val TILT_REVEAL_DEGREES = 15f
private const val MAX_TILT_DEGREES = 25f

@Composable
fun TiltCard(
    d: Discovery,
    isCurrent: Boolean,
    useGyroscope: Boolean,
    tilt: Flow<TiltState>,
    content: @Composable (Discovery) -> Unit,
) {
    var gyro by remember { mutableStateOf(TiltState()) }
    var dragRoll by remember(d.id) { mutableFloatStateOf(0f) }
    var revealed by rememberSaveable(d.id) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(isCurrent, useGyroscope) {
        gyro = TiltState()
        if (isCurrent && useGyroscope) tilt.collect { gyro = it }
    }

    val roll = if (useGyroscope) gyro.rollDegrees else dragRoll
    val pitch = if (useGyroscope) gyro.pitchDegrees else 0f
    val beyond = abs(roll) > TILT_REVEAL_DEGREES || abs(pitch) > TILT_REVEAL_DEGREES

    LaunchedEffect(beyond) {
        if (beyond && !revealed) {
            revealed = true
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val drag = if (useGyroscope) Modifier else Modifier.pointerInput(d.id) {
        detectHorizontalDragGestures(
            onDragEnd = { dragRoll = 0f },
            onDragCancel = { dragRoll = 0f },
        ) { change, dx ->
            change.consume()
            dragRoll = (dragRoll + dx / 12f).coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)
        }
    }

    Box(Modifier.fillMaxSize().then(drag)) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                rotationY = roll * 0.6f
                rotationX = -pitch * 0.6f
                cameraDistance = 16f * density
            }
        ) {
            content(if (revealed) d else d.copy(body = null))
        }

        if (revealed) {
            Text(
                text = stringResource(R.string.tilt_revealed),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 32.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        } else {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 140.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(if (useGyroscope) R.string.tilt_hint_gyro else R.string.tilt_hint_drag),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { revealed = true }) {
                    Text(stringResource(R.string.tilt_reveal_button), color = Color.White)
                }
            }
        }
    }
}
