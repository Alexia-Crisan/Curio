package com.example.curio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.curio.domain.DailyMode
import com.example.curio.domain.ProgressResetter
import com.example.curio.domain.SettingsStore
import com.example.curio.domain.Theme

data class SettingsUi(
    val themes: List<Theme>,
    val themeId: String?,
    val mode: DailyMode,
    val resetDialogVisible: Boolean,
    val resetting: Boolean,
)

sealed interface SettingsEvent {
    data object ResetDone : SettingsEvent
    data object ResetFailed : SettingsEvent
}

class SettingsViewModel(
    private val themes: List<Theme>,
    private val store: SettingsStore,
    private val resetter: ProgressResetter,
) : ViewModel() {

    private val dialogVisible = MutableStateFlow(false)
    private val resetting = MutableStateFlow(false)

    private val _events = MutableSharedFlow<SettingsEvent>(replay = 4)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    val ui: StateFlow<SettingsUi> =
        combine(store.observe(), dialogVisible, resetting) { settings, dialog, busy ->
            SettingsUi(themes, settings.themeId, settings.mode, dialog, busy)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            SettingsUi(themes, null, DailyMode.GUIDED, false, false),
        )

    fun onThemeSelected(id: String) {
        if (themes.none { it.id == id && it.available }) return
        if (ui.value.themeId == id) return
        viewModelScope.launch { store.setTheme(id) }
    }

    fun onModeSelected(mode: DailyMode) {
        if (ui.value.mode == mode) return
        viewModelScope.launch { store.setMode(mode) }
    }

    fun onResetRequested() {
        dialogVisible.value = true
    }

    fun onResetCancelled() {
        dialogVisible.value = false
    }

    fun onResetConfirmed() {
        if (!dialogVisible.value || resetting.value) return
        dialogVisible.value = false
        resetting.value = true
        viewModelScope.launch {
            try {
                resetter.resetAll()
                _events.emit(SettingsEvent.ResetDone)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ResetFailed)
            } finally {
                resetting.value = false
            }
        }
    }
}
