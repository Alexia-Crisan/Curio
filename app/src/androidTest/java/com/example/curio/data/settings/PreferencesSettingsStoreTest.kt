package com.example.curio.data.settings

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.example.curio.domain.DailyMode

@RunWith(AndroidJUnit4::class)
class PreferencesSettingsStoreTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val repo = SettingsRepository(context)
    private val store = PreferencesSettingsStore(repo)

    @After
    fun tearDown() {
        context.deleteDataStore("curio_settings")
    }

    private fun android.content.Context.deleteDataStore(name: String) {
        getDatabasePath("$name.preferences_pb").delete()
        fileList().filter { it.contains(name) }.forEach { deleteFile(it) }
    }

    @Test
    fun defaultsAreNoThemeAndGuided() = runBlocking {
        val settings = store.observe().first()
        assertNull(settings.themeId)
        assertEquals(DailyMode.GUIDED, settings.mode)
    }

    @Test
    fun setThemeIsReadBack() = runBlocking {
        store.setTheme("space")
        val settings = store.observe().first()
        assertEquals("space", settings.themeId)
    }

    @Test
    fun setModeIsReadBack() = runBlocking {
        store.setMode(DailyMode.SURPRISE)
        val settings = store.observe().first()
        assertEquals(DailyMode.SURPRISE, settings.mode)
    }

    @Test
    fun completeOnboardingWritesAllThreeValuesTogether() = runBlocking {
        store.completeOnboarding("animals", DailyMode.CHOOSE)
        val settings = store.observe().first()
        assertEquals("animals", settings.themeId)
        assertEquals(DailyMode.CHOOSE, settings.mode)
        assertTrue(repo.onboardingDone.first())
    }

    @Test
    fun valuesSurviveARestart() = runBlocking {
        store.setTheme("space")
        store.setMode(DailyMode.CHOOSE)
        val store2 = PreferencesSettingsStore(SettingsRepository(context))
        val settings = store2.observe().first()
        assertEquals("space", settings.themeId)
        assertEquals(DailyMode.CHOOSE, settings.mode)
    }
}
