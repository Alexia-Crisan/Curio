package com.example.curio.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curio.data.user.UserActivityRepository
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.ThemeTree
import com.example.curio.domain.TreeBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class TreeUiState(
    val trees: List<ThemeTree> = emptyList(),
    val selectedThemeId: String = "animals",
    val expandedTopicIds: Set<String> = emptySet(),
) {
    val currentTree: ThemeTree? get() = trees.firstOrNull { it.theme.id == selectedThemeId } ?: trees.firstOrNull()
}

class TreeViewModel(
    userActivityRepository: UserActivityRepository,
    private val contentIndex: ContentIndex,
    initialThemeId: String = "animals",
) : ViewModel() {

    private val _selectedThemeId = MutableStateFlow(initialThemeId)
    private val _expandedTopicIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<TreeUiState> = combine(
        userActivityRepository.observeSnapshot(),
        _selectedThemeId,
        _expandedTopicIds,
    ) { snapshot, selectedThemeId, expandedIds ->
        val trees = TreeBuilder.buildAll(contentIndex, snapshot)
        TreeUiState(
            trees = trees,
            selectedThemeId = selectedThemeId,
            expandedTopicIds = expandedIds,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TreeUiState())

    fun selectTheme(themeId: String) {
        _selectedThemeId.value = themeId
    }

    fun toggleTopicExpanded(topicId: String) {
        _expandedTopicIds.update { set ->
            if (topicId in set) set - topicId else set + topicId
        }
    }

    class Factory(
        private val userActivityRepository: UserActivityRepository,
        private val contentIndex: ContentIndex,
        private val initialThemeId: String = "animals",
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TreeViewModel(userActivityRepository, contentIndex, initialThemeId) as T
    }
}
