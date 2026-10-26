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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.curio.domain.ActivitySource
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.CurrentThemeSource
import com.example.curio.domain.LeafNode
import com.example.curio.domain.ThemeTree
import com.example.curio.domain.TreeBuilder
import com.example.curio.domain.TreeSelection

data class TreeUiState(
    val trees: List<ThemeTree> = emptyList(),
    val selectedThemeId: String? = null,
    val expandedTopicIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
) {
    val selected: ThemeTree? get() = trees.firstOrNull { it.theme.id == selectedThemeId }
    val isEmpty: Boolean get() = !isLoading && trees.isEmpty()
}

sealed interface TreeEvent {
    data class OpenDetail(val discoveryId: String) : TreeEvent
}

class TreeViewModel(
    activity: ActivitySource,
    private val currentTheme: CurrentThemeSource,
    private val content: ContentIndex,
) : ViewModel() {

    private val manualTheme = MutableStateFlow<String?>(null)
    private val expanded = MutableStateFlow<Set<String>>(emptySet())
    private val _events = Channel<TreeEvent>(Channel.BUFFERED)
    val events: Flow<TreeEvent> = _events.receiveAsFlow()

    val state: StateFlow<TreeUiState> = combine(
        activity.observeSnapshot().map { TreeBuilder.buildAll(content, it) },
        currentTheme.observe(),
        manualTheme,
        expanded,
    ) { trees, current, manual, open ->
        val selectedId = TreeSelection.resolve(trees, manual, current)
        TreeUiState(trees = trees, selectedThemeId = selectedId, expandedTopicIds = open, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TreeUiState())

    init {
        viewModelScope.launch {
            currentTheme.observe().distinctUntilChanged().drop(1).collect { manualTheme.value = null }
        }
    }

    fun onThemeSelected(themeId: String) { manualTheme.value = themeId }

    fun onTopicClick(topicId: String) {
        expanded.update { if (topicId in it) it - topicId else it + topicId }
    }

    fun onLeafClick(leaf: LeafNode) {
        if (leaf.completed) _events.trySend(TreeEvent.OpenDetail(leaf.discoveryId))
    }
}
