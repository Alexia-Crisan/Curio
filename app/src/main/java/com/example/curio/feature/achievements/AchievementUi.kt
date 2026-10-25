package com.example.curio.feature.achievements

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.curio.R
import com.example.curio.domain.AchievementId
import com.example.curio.domain.AchievementProgress

data class AchievementVisual(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
    val descriptionUsesTarget: Boolean,
)

object AchievementVisuals {
    private val visuals = mapOf(
        AchievementId.FIRST_DISCOVERY to AchievementVisual(R.string.badge_first_discovery_title, R.string.badge_first_discovery_desc, R.drawable.ic_badge_first_discovery, false),
        AchievementId.FULL_DAY to AchievementVisual(R.string.badge_full_day_title, R.string.badge_full_day_desc, R.drawable.ic_badge_full_day, false),
        AchievementId.CURIOUS_MIND to AchievementVisual(R.string.badge_curious_mind_title, R.string.badge_curious_mind_desc, R.drawable.ic_badge_curious_mind, true),
        AchievementId.TOPIC_EXPLORER to AchievementVisual(R.string.badge_topic_explorer_title, R.string.badge_topic_explorer_desc, R.drawable.ic_badge_topic_explorer, true),
        AchievementId.KNOWLEDGE_COLLECTOR to AchievementVisual(R.string.badge_knowledge_collector_title, R.string.badge_knowledge_collector_desc, R.drawable.ic_badge_knowledge_collector, true),
        AchievementId.DEEP_LEARNER to AchievementVisual(R.string.badge_deep_learner_title, R.string.badge_deep_learner_desc, R.drawable.ic_badge_deep_learner, true),
        AchievementId.HIDDEN_DISCOVERIES to AchievementVisual(R.string.badge_hidden_discoveries_title, R.string.badge_hidden_discoveries_desc, R.drawable.ic_badge_hidden_discoveries, true),
    )

    fun of(id: AchievementId): AchievementVisual? = visuals[id]
}

data class AchievementUi(
    val id: AchievementId,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val descriptionUsesTarget: Boolean,
    val current: Int,
    val target: Int,
    val fraction: Float,
    val isUnlocked: Boolean,
    val unlockedOn: String?,
)

@Composable
fun AchievementUi.description(): String =
    if (descriptionUsesTarget) stringResource(descriptionRes, target) else stringResource(descriptionRes)

fun AchievementProgress.toUi(formatDate: (Long) -> String): AchievementUi? {
    val v = AchievementVisuals.of(def.id) ?: return null
    return AchievementUi(
        id = def.id, titleRes = v.title, descriptionRes = v.description, iconRes = v.icon,
        descriptionUsesTarget = v.descriptionUsesTarget,
        current = current, target = def.target, fraction = fraction,
        isUnlocked = isUnlocked, unlockedOn = unlockedAt?.let(formatDate),
    )
}
