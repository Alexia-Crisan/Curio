package com.example.curio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.curio.R
import com.example.curio.domain.AchievementId
import com.example.curio.domain.AchievementProgress
import com.example.curio.domain.BadgeTreeLayout
import com.example.curio.domain.FoliageShade
import com.example.curio.domain.TreePoint
import com.example.curio.feature.achievements.AchievementVisuals
import com.example.curio.ui.theme.CurioTheme
import com.example.curio.ui.theme.Spacing

private data class TreePalette(
    val wood: Color,
    val deep: Color,
    val mid: Color,
    val light: Color,
    val ground: Color,
    val ghost: Color,
)

@Composable
fun BadgeTreeCard(progress: List<AchievementProgress>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val bubble = scheme.surface.copy(alpha = 0.35f)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
        ),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(bubble, radius = size.width * 0.28f, center = Offset(size.width * 0.95f, size.height * 0.02f))
                    drawCircle(bubble, radius = size.width * 0.14f, center = Offset(size.width * 0.02f, size.height * 0.40f))
                    drawCircle(bubble, radius = size.width * 0.10f, center = Offset(size.width * 0.80f, size.height * 0.88f))
                }
                .padding(vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BadgeTree(progress, Modifier.fillMaxWidth(0.82f))
        }
    }
}

@Composable
fun BadgeTree(progress: List<AchievementProgress>, modifier: Modifier = Modifier) {
    val layout = remember(progress) { BadgeTreeLayout.of(progress) }
    val animation = remember { Animatable(0f) }
    LaunchedEffect(layout.level) {
        animation.animateTo(layout.level.toFloat(), tween(durationMillis = 1200))
    }
    val animatedLevel = animation.value
    var selected by remember { mutableStateOf<AchievementId?>(null) }
    val scheme = MaterialTheme.colorScheme
    val palette = TreePalette(
        wood = scheme.tertiary,
        deep = lerp(scheme.primary, Color.Black, 0.25f),
        mid = scheme.primary,
        light = lerp(scheme.primary, Color.White, 0.3f),
        ground = lerp(scheme.primaryContainer, scheme.primary, 0.25f),
        ghost = scheme.onPrimaryContainer.copy(alpha = 0.4f),
    )
    val total = BadgeTreeLayout.MAX_LEVEL
    val description = stringResource(R.string.badge_tree_cd, layout.level, total)
    val picked = selected?.let { id -> layout.slots.firstOrNull { it.badge == id }?.badge }
    val caption = when {
        picked != null -> AchievementVisuals.of(picked)?.let { stringResource(it.title) }.orEmpty()
        layout.level == 0 -> stringResource(R.string.badge_tree_empty)
        else -> stringResource(R.string.badge_tree_summary, layout.level, total)
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(BadgeTreeLayout.WIDTH / BadgeTreeLayout.HEIGHT)
                .semantics { contentDescription = description },
        ) {
            val unit = maxWidth / BadgeTreeLayout.WIDTH
            Canvas(Modifier.fillMaxSize()) {
                drawBadgeTree(layout, animatedLevel, palette)
            }
            layout.slots.forEach { slot ->
                val badge = slot.badge ?: return@forEach
                val visual = AchievementVisuals.of(badge) ?: return@forEach
                val grown = BadgeTreeLayout.growth(animatedLevel, slot.index + 1)
                if (grown > 0f) {
                    val title = stringResource(visual.title)
                    Box(
                        Modifier
                            .offset(unit * (slot.center.x - slot.radius), unit * (slot.center.y - slot.radius))
                            .size(unit * (slot.radius * 2f))
                            .graphicsLayer {
                                scaleX = grown
                                scaleY = grown
                                alpha = grown
                            }
                            .clip(CircleShape)
                            .background(CurioTheme.colors.rewardContainer)
                            .border(2.dp, scheme.surfaceContainerLowest, CircleShape)
                            .clickable(role = Role.Button) { selected = if (selected == badge) null else badge }
                            .semantics { contentDescription = title },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(visual.icon),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.6f),
                            tint = CurioTheme.colors.onRewardContainer,
                        )
                    }
                }
            }
        }
        Text(
            caption,
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.sm),
        )
    }
}

private fun DrawScope.drawBadgeTree(layout: BadgeTreeLayout, level: Float, palette: TreePalette) {
    val k = size.width / BadgeTreeLayout.WIDTH
    fun at(p: TreePoint) = Offset(p.x * k, p.y * k)

    val g = layout.ground
    drawOval(
        color = palette.ground,
        topLeft = Offset((g.center.x - g.radiusX) * k, (g.center.y - g.radiusY) * k),
        size = Size(g.radiusX * 2f * k, g.radiusY * 2f * k),
    )

    layout.slots.forEach { slot ->
        val fade = 1f - BadgeTreeLayout.growth(level, slot.index + 1)
        if (fade > 0f) {
            drawCircle(
                color = palette.ghost.copy(alpha = palette.ghost.alpha * fade),
                radius = slot.radius * k,
                center = at(slot.center),
                style = Stroke(
                    width = 1.6f * k,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * k, 3f * k)),
                ),
            )
        }
    }

    val thickness = BadgeTreeLayout.thickness(level)
    layout.trunk.forEach { limb ->
        val grown = BadgeTreeLayout.growth(level, limb.appearAt)
        if (grown > 0f) {
            val end = Offset(
                limb.from.x + (limb.to.x - limb.from.x) * grown,
                limb.from.y + (limb.to.y - limb.from.y) * grown,
            )
            val control = Offset(
                limb.from.x + (limb.control.x - limb.from.x) * grown,
                limb.from.y + (limb.control.y - limb.from.y) * grown,
            )
            val path = Path().apply {
                moveTo(limb.from.x * k, limb.from.y * k)
                quadraticBezierTo(control.x * k, control.y * k, end.x * k, end.y * k)
            }
            drawPath(
                path = path,
                color = palette.wood,
                style = Stroke(width = limb.width * thickness * k, cap = StrokeCap.Round),
            )
        }
    }

    FoliageShade.entries.forEach { shade ->
        val color = when (shade) {
            FoliageShade.DEEP -> palette.deep
            FoliageShade.MID -> palette.mid
            FoliageShade.LIGHT -> palette.light.copy(alpha = 0.7f)
        }
        layout.foliage.filter { it.shade == shade }.forEach { foliage ->
            val scale = BadgeTreeLayout.scaleOf(foliage, level)
            if (scale > 0f) {
                drawCircle(color = color, radius = foliage.radius * k * scale, center = at(foliage.center))
            }
        }
    }
}
