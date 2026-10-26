package com.example.curio.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.curio.domain.DailyMode
import com.example.curio.domain.SettingsStore
import com.example.curio.domain.Theme

data class OnboardingUi(
    val themes: List<Theme>,
    val selectedThemeId: String?,
    val mode: DailyMode,
    val saving: Boolean,
) {
    val canStart: Boolean get() = selectedThemeId != null && !saving
}

sealed interface OnboardingEvent {
    data object Finished : OnboardingEvent
}

class OnboardingViewModel(
    private val themes: List<Theme>,
    private val store: SettingsStore,
) : ViewModel() {

    private val _ui = MutableStateFlow(OnboardingUi(themes, null, DailyMode.GUIDED, false))
    val ui: StateFlow<OnboardingUi> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>(replay = 4)
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    fun selectTheme(id: String) {
        if (themes.none { it.id == id && it.available }) return
        _ui.update { it.copy(selectedThemeId = id) }
    }

    fun selectMode(mode: DailyMode) {
        _ui.update { it.copy(mode = mode) }
    }

    fun start() {
        val current = _ui.value
        val themeId = current.selectedThemeId ?: return
        if (current.saving) return
        _ui.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                store.completeOnboarding(themeId, current.mode)
                _events.emit(OnboardingEvent.Finished)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(saving = false) }
            }
        }
    }
}
