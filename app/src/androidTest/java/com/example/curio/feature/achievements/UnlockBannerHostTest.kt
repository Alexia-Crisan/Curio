package com.example.curio.feature.achievements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.curio.R
import com.example.curio.domain.AchievementConfig
import com.example.curio.domain.AchievementId
import com.example.curio.testing.FakeAchievementSource
import com.example.curio.ui.theme.CurioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnlockBannerHostTest {
    @get:Rule
    val rule = createComposeRule()

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun bannerShownAndMarkNotifiedCalledWhenPending() {
        val fakeSource = FakeAchievementSource()
        val firstBadgeDef = AchievementConfig.defs.first { it.id == AchievementId.FIRST_DISCOVERY }
        fakeSource.pending.value = firstBadgeDef

        rule.setContent {
            CurioTheme {
                UnlockBannerHost(
                    source = fakeSource,
                    suppress = false,
                    onOpenAchievements = {},
                )
            }
        }

        val expectedTitle = ctx.getString(R.string.badge_first_discovery_title)
        rule.onNodeWithText(expectedTitle).assertExists()

        rule.waitUntil(timeoutMillis = 5000) {
            fakeSource.notified.contains(AchievementId.FIRST_DISCOVERY)
        }
        assertTrue(fakeSource.notified.contains(AchievementId.FIRST_DISCOVERY))
    }

    @Test
    fun nothingShownWhenSuppressed() {
        val fakeSource = FakeAchievementSource()
        val firstBadgeDef = AchievementConfig.defs.first { it.id == AchievementId.FIRST_DISCOVERY }
        fakeSource.pending.value = firstBadgeDef

        rule.setContent {
            CurioTheme {
                UnlockBannerHost(
                    source = fakeSource,
                    suppress = true,
                    onOpenAchievements = {},
                )
            }
        }

        val expectedTitle = ctx.getString(R.string.badge_first_discovery_title)
        rule.onNodeWithText(expectedTitle).assertDoesNotExist()

        rule.waitUntil(timeoutMillis = 5000) {
            fakeSource.notified.contains(AchievementId.FIRST_DISCOVERY)
        }
        assertTrue(fakeSource.notified.contains(AchievementId.FIRST_DISCOVERY))
    }
}
