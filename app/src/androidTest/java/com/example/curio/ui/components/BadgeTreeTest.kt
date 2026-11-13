package com.example.curio.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import com.example.curio.R
import com.example.curio.domain.AchievementConfig
import com.example.curio.domain.AchievementProgress
import com.example.curio.ui.theme.CurioTheme

class BadgeTreeTest {
    @get:Rule
    val rule = createComposeRule()

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    private fun progress(unlocked: Int): List<AchievementProgress> =
        AchievementConfig.defs.mapIndexed { index, def ->
            val on = def.mvp && index < unlocked
            AchievementProgress(def, if (on) def.target else 0, if (on) 100L + index else null)
        }

    @Test
    fun describesHowManyBadgesTheTreeHolds() {
        rule.setContent { CurioTheme { BadgeTree(progress(3)) } }
        rule.onNodeWithContentDescription(ctx.getString(R.string.badge_tree_cd, 3, 7)).assertExists()
        rule.onNodeWithText(ctx.getString(R.string.badge_tree_summary, 3, 7)).assertExists()
    }

    @Test
    fun anEmptyTreeInvitesTheFirstBadge() {
        rule.setContent { CurioTheme { BadgeTree(progress(0)) } }
        rule.onNodeWithContentDescription(ctx.getString(R.string.badge_tree_cd, 0, 7)).assertExists()
        rule.onNodeWithText(ctx.getString(R.string.badge_tree_empty)).assertExists()
    }

    @Test
    fun tappingABadgeOnTheTreeShowsItsName() {
        rule.setContent { CurioTheme { BadgeTree(progress(2)) } }
        val title = ctx.getString(R.string.badge_first_discovery_title)
        rule.onNodeWithContentDescription(title).performClick()
        rule.onNodeWithText(title).assertExists()
    }
}
