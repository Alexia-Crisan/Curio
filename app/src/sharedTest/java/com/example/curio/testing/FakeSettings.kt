package com.example.curio.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.curio.domain.DailyMode
import com.example.curio.domain.ProgressResetter
import com.example.curio.domain.SettingsStore
import com.example.curio.domain.UserSettings

class FakeSettingsStore(initial: UserSettings = UserSettings(null, DailyMode.GUIDED)) : SettingsStore {
    val settings = MutableStateFlow(initial)
    val calls = mutableListOf<String>()
    var failOnComplete = false

    override fun observe(): Flow<UserSettings> = settings

    override suspend fun setTheme(themeId: String) {
        calls += "theme:$themeId"
        settings.value = settings.value.copy(themeId = themeId)
    }

    override suspend fun setMode(mode: DailyMode) {
        calls += "mode:$mode"
        settings.value = settings.value.copy(mode = mode)
    }

    override suspend fun completeOnboarding(themeId: String, mode: DailyMode) {
        calls += "onboarding:$themeId:$mode"
        if (failOnComplete) throw IllegalStateException("write failed")
        settings.value = UserSettings(themeId, mode)
    }
}

class FakeResetter : ProgressResetter {
    var count = 0
    var fail = false

    override suspend fun resetAll() {
        count++
        if (fail) throw IllegalStateException("reset failed")
    }
}
