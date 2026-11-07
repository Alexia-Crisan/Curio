package com.example.curio.feature.tree

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.curio.R
import com.example.curio.domain.GrowthStage
import com.example.curio.domain.LeafExplanation
import com.example.curio.domain.LeafNode
import com.example.curio.domain.NodeState
import com.example.curio.domain.Theme
import com.example.curio.domain.ThemeTree
import com.example.curio.domain.Topic
import com.example.curio.domain.TopicNode
import com.example.curio.ui.theme.CurioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TreeScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun tappingLeafOpensSheetAndOpenDetailButtonCallsCallback() {
        var openedDetailId: String? = null
        var tappedLeaf: LeafNode? = null

        val topic = Topic("tp1", "t1", "Planets")
        val theme = Theme("t1", "Space", listOf(topic), available = true)
        val completedLeaf = LeafNode("d1", "Mars", completed = true, isLandmark = false, viaBonus = false)
        val branch = TopicNode(
            topic = topic,
            completed = 1,
            total = 6,
            state = NodeState.GROWING,
            leaves = listOf(completedLeaf),
            firstCompletedAtMs = null,
        )
        val tree = ThemeTree(
            theme = theme,
            stage = GrowthStage.SPROUT,
            completed = 1,
            total = 6,
            branches = listOf(branch),
        )

        val explanation = LeafExplanation(
            discoveryId = "d1",
            title = "Mars",
            completed = true,
            isLandmark = false,
            viaBonus = false,
        )
        val leafSheet = NodeSheet.LeafSheet(branch, completedLeaf, explanation)

        val state = TreeScreenUi(
            isLoading = false,
            trees = listOf(tree),
            selectedThemeId = "t1",
            sheet = leafSheet,
        )

        rule.setContent {
            CurioTheme {
                TreeScreen(
                    state = state,
                    onBack = {},
                    onThemeSelected = {},
                    onTopicTap = {},
                    onLeafTap = { _, leaf -> tappedLeaf = leaf },
                    onSheetDismiss = {},
                    onOpenDetail = { openedDetailId = it },
                )
            }
        }

        // Check tapping leaf callback works
        val leafCd = ctx.getString(R.string.tree_cd_leaf_done, "Mars")
        rule.onNodeWithContentDescription(leafCd).performClick()
        assertEquals(completedLeaf, tappedLeaf)

        // Check sheet open detail button is displayed and calls onOpenDetail
        val openDetailText = ctx.getString(R.string.tree_sheet_open_detail)
        rule.onNodeWithText(openDetailText)
            .assertExists()
            .performClick()

        assertEquals("d1", openedDetailId)
    }
}
