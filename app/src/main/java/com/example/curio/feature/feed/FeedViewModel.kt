package com.example.curio.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curio.domain.Clock
import com.example.curio.domain.Discovery
import com.example.curio.domain.DiscoveryRepository
import com.example.curio.domain.InteractionRepository
import com.example.curio.domain.InteractionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Ready(val cards: List<Discovery>) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

class FeedViewModel(
    private val discoveries: DiscoveryRepository,
    private val interactions: InteractionRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    val savedIds: StateFlow<Set<String>> = interactions.observeSavedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init { load() }

    fun load() {
        _state.value = FeedUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                FeedUiState.Ready(discoveries.getDailyFeed(clock.today()))
            } catch (e: Exception) {
                FeedUiState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun onSaveClicked(id: String) {
        viewModelScope.launch {
            val type = if (id in savedIds.value) InteractionType.UNSAVED else InteractionType.SAVED
            interactions.record(id, type)
        }
    }

    fun onQuizAnswered(id: String) {
        viewModelScope.launch { interactions.record(id, InteractionType.COMPLETED) }
    }

    class Factory(
        private val discoveries: DiscoveryRepository,
        private val interactions: InteractionRepository,
        private val clock: Clock,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FeedViewModel(discoveries, interactions, clock) as T
    }
}
