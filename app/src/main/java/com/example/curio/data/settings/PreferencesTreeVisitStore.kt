package com.example.curio.data.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.example.curio.domain.TreeVisitStore

class PreferencesTreeVisitStore(
    private val prefs: SettingsRepository,
    private val scope: CoroutineScope,
) : TreeVisitStore {

    override suspend fun lastVisitedAt(): Long? = prefs.treeLastVisitedAt.firstOrNull()

    override fun markVisited(nowMs: Long) {
        scope.launch { prefs.setTreeLastVisitedAt(nowMs) }
    }

    override fun clear() {
        scope.launch { prefs.clearTreeLastVisitedAt() }
    }
}
