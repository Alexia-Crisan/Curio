package com.example.curio.feature.feed

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.example.curio.domain.ContentType
import com.example.curio.domain.Discovery

@Composable
fun DiscoveryCard(
    d: Discovery,
    saved: Boolean,
    onSave: () -> Unit,
    onQuizAnswered: () -> Unit,
) {
    CardScaffold(d, saved, onSave) {
        when (d.type) {
            ContentType.FACT -> FactContent(d)
            ContentType.ARTICLE -> ArticleContent(d)
            ContentType.QUIZ -> QuizContent(d, onQuizAnswered)
            ContentType.IMAGE -> ImageContent(d)
            ContentType.VIDEO -> VideoContent(d)
            else -> FactContent(d)
        }
    }
}

@Composable
private fun CardScaffold(
    d: Discovery,
    saved: Boolean,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val accent = Color(d.accentColor)
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(accent, lerp(accent, Color.Black, 0.55f))))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 72.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.Center,
            content = content,
        )
        Text(
            text = "↗  ${d.source.name}",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(24.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.16f))
                .clickable { uriHandler.openUri(d.source.url) }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp)
        ) { SaveButton(saved, onSave) }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.75f),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp,
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ColumnScope.FactContent(d: Discovery) {
    Label("DID YOU KNOW")
    Text(d.title, color = Color.White, fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
    d.body?.let {
        Spacer(Modifier.height(20.dp))
        Text(it, color = Color.White.copy(alpha = 0.9f), fontSize = 18.sp, lineHeight = 26.sp)
    }
}

@Composable
private fun ColumnScope.ArticleContent(d: Discovery) {
    Label("MINI ARTICLE")
    Text(d.title, color = Color.White, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    Box(
        Modifier
            .width(48.dp)
            .height(3.dp)
            .background(Color.White.copy(alpha = 0.6f))
    )
    d.body?.let {
        Spacer(Modifier.height(16.dp))
        Text(it, color = Color.White.copy(alpha = 0.92f), fontSize = 17.sp, lineHeight = 26.sp)
    }
}

@Composable
private fun ColumnScope.QuizContent(d: Discovery, onAnswered: () -> Unit) {
    val quiz = d.quiz ?: return
    var selected by rememberSaveable(d.id) { mutableStateOf<Int?>(null) }

    Label("TEST YOURSELF")
    Text(quiz.question, color = Color.White, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(24.dp))

    quiz.options.forEachIndexed { i, option ->
        val answered = selected != null
        val target = when {
            !answered -> Color.White.copy(alpha = 0.18f)
            i == quiz.correctIndex -> Color(0xFF22C55E)
            i == selected -> Color(0xFFEF4444)
            else -> Color.White.copy(alpha = 0.08f)
        }
        val bg by animateColorAsState(target, label = "option")
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .clickable(enabled = !answered) {
                    selected = i
                    onAnswered()
                }
                .padding(16.dp)
        ) { Text(option, color = Color.White, fontSize = 17.sp) }
    }

    AnimatedVisibility(visible = selected != null) {
        Text(
            quiz.explanation,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun ColumnScope.ImageContent(d: Discovery) {
    Label("IMAGE DISCOVERY")
    d.mediaUrl?.let { url ->
        AsyncImage(
            model = url,
            contentDescription = d.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(16.dp))
    }
    Text(d.title, color = Color.White, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
    d.body?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, lineHeight = 22.sp)
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ColumnScope.VideoContent(d: Discovery) {
    val context = LocalContext.current
    val exoPlayer = remember(d.mediaUrl) {
        ExoPlayer.Builder(context).build().apply {
            d.mediaUrl?.let { url ->
                setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ONE
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Label("VIDEO DISCOVERY")
    Box(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(d.title, color = Color.White, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
    d.body?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun SaveButton(saved: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(saved) {
        if (saved) {
            scale.animateTo(1.4f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            scale.animateTo(1f)
        }
    }
    Text(
        text = if (saved) "♥" else "♡",
        fontSize = 36.sp,
        color = if (saved) Color(0xFFFF5A7A) else Color.White,
        modifier = Modifier
            .scale(scale.value)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(12.dp),
    )
}
