package com.example.curio.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.curio.di.AppContainer
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.DetailReadingTracker
import com.example.curio.domain.DetailSaves
import com.example.curio.domain.Discovery
import com.example.curio.domain.TouchQueue
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    private val discoveryId: String,
    content: ContentIndex,
    private val saves: DetailSaves,
    private val touches: TouchQueue,
    private val clock: Clock,
) : ViewModel() {

    private val _discovery = MutableStateFlow(content.discovery(discoveryId))
    val discovery: StateFlow<Discovery?> = _discovery.asStateFlow()

    val isSaved: StateFlow<Boolean> = saves.observeSaved()
        .map { discoveryId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val tracker = DetailReadingTracker(discoveryId, LocalDate.now(clock))
    private var checkpointJob: Job? = null

    fun onVisible() {
        if (_discovery.value == null) return
        val now = clock.millis()
        touches.offerAll(tracker.open(now))
        tracker.visible(now)
        if (checkpointJob?.isActive != true) {
            checkpointJob = viewModelScope.launch {
                while (true) {
                    delay(DetailReadingTracker.CHECKPOINT_MS)
                    touches.offerAll(tracker.checkpoint(clock.millis()))
                }
            }
        }
    }

    fun onHidden() {
        checkpointJob?.cancel()
        checkpointJob = null
        touches.offerAll(tracker.hidden(clock.millis()))
    }

    fun toggleSave() {
        viewModelScope.launch {
            val saved = discoveryId in saves.observeSaved().first()
            saves.setSaved(discoveryId, !saved, LocalDate.now(clock))
        }
    }

    override fun onCleared() {
        onHidden()
    }

    companion object {
        fun factory(container: AppContainer, discoveryId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DetailViewModel(
                    discoveryId = discoveryId,
                    content = container.contentIndex,
                    saves = container.detailSaves,
                    touches = container.touchQueue,
                    clock = container.javaClock,
                )
            }
        }
    }
}
