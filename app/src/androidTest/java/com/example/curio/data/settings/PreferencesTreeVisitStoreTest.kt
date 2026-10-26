package com.example.curio.data.settings

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesTreeVisitStoreTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val repo = SettingsRepository(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = PreferencesTreeVisitStore(repo, scope)

    @After
    fun tearDown() {
        context.deleteDataStore("curio_settings")
    }

    private fun android.content.Context.deleteDataStore(name: String) {
        getDatabasePath("$name.preferences_pb").delete()
        fileList().filter { it.contains(name) }.forEach { deleteFile(it) }
    }

    private suspend fun pollEquals(expected: Long?, action: suspend () -> Long?) {
        var actual: Long? = null
        repeat(40) {
            actual = action()
            if (actual == expected) return
            delay(50)
        }
        assertEquals(expected, actual)
    }

    @Test
    fun defaultsToNull() = runBlocking {
        assertNull(store.lastVisitedAt())
    }

    @Test
    fun markVisitedIsReadBack() = runBlocking {
        store.markVisited(1234L)
        pollEquals(1234L) { store.lastVisitedAt() }
    }

    @Test
    fun theLatestMarkWins() = runBlocking {
        store.markVisited(1L)
        store.markVisited(2L)
        pollEquals(2L) { store.lastVisitedAt() }
    }

    @Test
    fun clearRemovesTheValue() = runBlocking {
        store.markVisited(5L)
        pollEquals(5L) { store.lastVisitedAt() }
        store.clear()
        pollEquals(null) { store.lastVisitedAt() }
    }

    @Test
    fun theValueSurvivesARestart() = runBlocking {
        store.markVisited(77L)
        pollEquals(77L) { store.lastVisitedAt() }
        val store2 = PreferencesTreeVisitStore(SettingsRepository(context), scope)
        pollEquals(77L) { store2.lastVisitedAt() }
    }
}
