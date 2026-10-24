package com.example.curio.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.curio.domain.ActivitySnapshot
import com.example.curio.domain.ActivitySource
import com.example.curio.domain.CurrentThemeSource
import com.example.curio.domain.snapshotOf

class FakeActivitySource(initial: ActivitySnapshot = snapshotOf()) : ActivitySource {
    val snapshot = MutableStateFlow(initial)
    override fun observeSnapshot(): Flow<ActivitySnapshot> = snapshot
}

class FakeCurrentTheme(initial: String? = null) : CurrentThemeSource {
    val theme = MutableStateFlow(initial)
    override fun observe(): Flow<String?> = theme
}
