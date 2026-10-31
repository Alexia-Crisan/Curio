package com.example.curio.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.curio.domain.DailyMode
import com.example.curio.domain.DailyModeSource

class FakeDailyModeSource(initial: DailyMode = DailyMode.GUIDED) : DailyModeSource {
    val mode = MutableStateFlow(initial)
    override fun observe(): Flow<DailyMode> = mode
}
