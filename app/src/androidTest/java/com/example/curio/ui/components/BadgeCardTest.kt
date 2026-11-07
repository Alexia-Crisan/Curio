package com.example.curio.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.curio.R
import com.example.curio.domain.AchievementId
import com.example.curio.feature.achievements.AchievementUi
import com.example.curio.ui.theme.CurioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BadgeCardTest {
    @get:Rule
    val rule = createComposeRule()

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun lockedBadgeContentDescriptionAndClick() {
        var clicked = false
        val lockedBadge = AchievementUi(
            id = AchievementId.FIRST_DISCOVERY,
            titleRes = R.string.badge_first_discovery_title,
            descriptionRes = R.string.badge_first_discovery_desc,
            iconRes = R.drawable.ic_badge_first_discovery,
            descriptionUsesTarget = false,
            current = 0,
            target = 1,
            fraction = 0f,
            isUnlocked = false,
            unlockedOn = null,
        )
        val title = ctx.getString(R.string.badge_first_discovery_title)
        val expectedDescription = ctx.getString(R.string.badge_cd_locked, title, 0, 1)

        rule.setContent {
            CurioTheme {
                BadgeCard(badge = lockedBadge, onClick = { clicked = true })
            }
        }

        rule.onNodeWithContentDescription(expectedDescription)
            .assertExists()
            .performClick()

        assertTrue(clicked)
    }

    @Test
    fun unlockedBadgeContentDescription() {
        val date = "01.11.2026"
        val unlockedBadge = AchievementUi(
            id = AchievementId.FIRST_DISCOVERY,
            titleRes = R.string.badge_first_discovery_title,
            descriptionRes = R.string.badge_first_discovery_desc,
            iconRes = R.drawable.ic_badge_first_discovery,
            descriptionUsesTarget = false,
            current = 1,
            target = 1,
            fraction = 1f,
            isUnlocked = true,
            unlockedOn = date,
        )
        val title = ctx.getString(R.string.badge_first_discovery_title)
        val expectedDescription = ctx.getString(R.string.badge_cd_unlocked, title, date)

        rule.setContent {
            CurioTheme {
                BadgeCard(badge = unlockedBadge, onClick = {})
            }
        }

        rule.onNodeWithContentDescription(expectedDescription).assertExists()
    }
}
