package com.example.curio.ui.components

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.curio.R
import com.example.curio.domain.GrowthStage
import com.example.curio.domain.LeafNode
import com.example.curio.domain.NodeState
import com.example.curio.domain.Theme
import com.example.curio.domain.ThemeTree
import com.example.curio.domain.Topic
import com.example.curio.domain.TopicNode
import com.example.curio.feature.progress.TreeUiState
import com.example.curio.ui.theme.CurioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TreeCardTest {
    @get:Rule
    val rule = createComposeRule()

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun completedLeafCallsOnLeafClickAndLockedLeafDoesNothing() {
        var clickedLeaf: LeafNode? = null

        val topic = Topic("tp1", "t1", "Planets")
        val theme = Theme("t1", "Space", listOf(topic), available = true)
        val completedLeaf = LeafNode("d1", "Mars", completed = true, isLandmark = false, viaBonus = false)
        val lockedLeaf = LeafNode("d2", null, completed = false, isLandmark = false, viaBonus = false)

        val branch = TopicNode(
            topic = topic,
            completed = 1,
            total = 6,
            state = NodeState.GROWING,
            leaves = listOf(completedLeaf, lockedLeaf),
            firstCompletedAtMs = null,
        )
        val tree = ThemeTree(
            theme = theme,
            stage = GrowthStage.SPROUT,
            completed = 1,
            total = 6,
            branches = listOf(branch),
        )

        val state = TreeUiState(
            trees = listOf(tree),
            selectedThemeId = "t1",
            expandedTopicIds = setOf("tp1"),
        )

        rule.setContent {
            CurioTheme {
                TreeCard(
                    state = state,
                    onThemeSelected = {},
                    onTopicClick = {},
                    onLeafClick = { clickedLeaf = it },
                )
            }
        }

        // Completed leaf has title "Mars" and contentDescription tree_cd_leaf_done
        val completedCd = ctx.getString(R.string.tree_cd_leaf_done, "Mars")
        rule.onNodeWithContentDescription(completedCd).performClick()
        assertEquals(completedLeaf, clickedLeaf)

        // Locked leaf shows no title "Jupiter"
        rule.onNodeWithText("Jupiter").assertDoesNotExist()

        // Locked leaf has contentDescription tree_cd_leaf_locked and is disabled
        clickedLeaf = null
        val lockedCd = ctx.getString(R.string.tree_cd_leaf_locked)
        rule.onNodeWithContentDescription(lockedCd)
            .assertIsNotEnabled()
            .performClick()

        assertNull(clickedLeaf)
    }
}
