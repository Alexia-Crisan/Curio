package com.example.curio.feature.tree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.curio.data.RealClock
import com.example.curio.domain.ActivitySource
import com.example.curio.domain.Clock
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.CurrentThemeSource
import com.example.curio.domain.LeafExplanation
import com.example.curio.domain.LeafNode
import com.example.curio.domain.ThemeExplanation
import com.example.curio.domain.ThemeTree
import com.example.curio.domain.TopicExplanation
import com.example.curio.domain.TopicNode
import com.example.curio.domain.TreeBuilder
import com.example.curio.domain.TreeExplainer
import com.example.curio.domain.TreeGrowth
import com.example.curio.domain.TreeGrowthSummary
import com.example.curio.domain.TreeSelection
import com.example.curio.domain.TreeVisitStore

private data class Baseline(val ready: Boolean, val sinceMs: Long?)

sealed interface NodeKey {
    data class OfTopic(val topicId: String) : NodeKey
    data class OfLeaf(val topicId: String, val discoveryId: String) : NodeKey
}

sealed interface NodeSheet {
    data class TopicSheet(val node: TopicNode, val explanation: TopicExplanation) : NodeSheet
    data class LeafSheet(val topic: TopicNode, val leaf: LeafNode, val explanation: LeafExplanation) : NodeSheet
}

data class TreeScreenUi(
    val trees: List<ThemeTree> = emptyList(),
    val selectedThemeId: String? = null,
    val themeExplanation: ThemeExplanation? = null,
    val sheet: NodeSheet? = null,
    val isLoading: Boolean = true,
    val growth: TreeGrowthSummary? = null,
) {
    val selected: ThemeTree? get() = trees.firstOrNull { it.theme.id == selectedThemeId }
    val isEmpty: Boolean get() = !isLoading && trees.isEmpty()
}

sealed interface TreeScreenEvent {
    data class OpenDetail(val discoveryId: String) : TreeScreenEvent
}

class TreeScreenViewModel(
    activity: ActivitySource,
    private val currentTheme: CurrentThemeSource,
    private val content: ContentIndex,
    initialThemeId: String?,
    private val visits: TreeVisitStore = TreeVisitStore.None,
    private val clock: Clock = RealClock(),
) : ViewModel() {

    private val manualTheme = MutableStateFlow(initialThemeId)
    private val openNode = MutableStateFlow<NodeKey?>(null)
    private val baseline = MutableStateFlow(Baseline(false, null))
    private val _events = Channel<TreeScreenEvent>(Channel.BUFFERED)
    val events: Flow<TreeScreenEvent> = _events.receiveAsFlow()

    val state: StateFlow<TreeScreenUi> = combine(
        activity.observeSnapshot().map { TreeBuilder.buildAll(content, it) },
        currentTheme.observe(),
        manualTheme,
        openNode,
        baseline,
    ) { trees, current, manual, key, base ->
        val selectedId = TreeSelection.resolve(trees, manual, current)
        val tree = trees.firstOrNull { it.theme.id == selectedId }
        TreeScreenUi(
            trees = trees,
            selectedThemeId = selectedId,
            themeExplanation = tree?.let { TreeExplainer.explainTheme(it) },
            sheet = if (tree != null && key != null) resolveSheet(tree, key) else null,
            isLoading = false,
            growth = if (tree != null && base.ready) TreeGrowth.since(tree, base.sinceMs) else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TreeScreenUi())

    init {
        viewModelScope.launch {
            baseline.value = Baseline(true, visits.lastVisitedAt())
        }
        viewModelScope.launch {
            currentTheme.observe().distinctUntilChanged().drop(1).collect { manualTheme.value = null }
        }
    }

    fun onThemeSelected(themeId: String) {
        manualTheme.value = themeId
        openNode.value = null
    }

    fun onTopicTap(topicId: String) {
        openNode.value = NodeKey.OfTopic(topicId)
    }

    fun onLeafTap(topicId: String, discoveryId: String) {
        openNode.value = NodeKey.OfLeaf(topicId, discoveryId)
    }

    fun onSheetDismiss() {
        openNode.value = null
    }

    fun onOpenDetail(discoveryId: String) {
        val sheet = state.value.sheet as? NodeSheet.LeafSheet ?: return
        if (sheet.leaf.discoveryId != discoveryId || !sheet.explanation.canOpenDetail) return
        _events.trySend(TreeScreenEvent.OpenDetail(discoveryId))
        openNode.value = null
    }

    fun onLeave() {
        visits.markVisited(clock.now().toEpochMilli())
    }

    private fun resolveSheet(tree: ThemeTree, key: NodeKey): NodeSheet? = when (key) {
        is NodeKey.OfTopic -> tree.branches.firstOrNull { it.topic.id == key.topicId }
            ?.let { NodeSheet.TopicSheet(it, TreeExplainer.explainTopic(it)) }
        is NodeKey.OfLeaf -> tree.branches.firstOrNull { it.topic.id == key.topicId }?.let { branch ->
            branch.leaves.firstOrNull { it.discoveryId == key.discoveryId }
                ?.let { NodeSheet.LeafSheet(branch, it, TreeExplainer.explainLeaf(it)) }
        }
    }
}
