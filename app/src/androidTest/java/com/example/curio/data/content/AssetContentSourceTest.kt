package com.example.curio.data.content

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.curio.domain.ContentLoader
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetContentSourceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theShippedAssetsAreReadableFromThePackage() {
        val result = AssetContentSource(context).load()
        assertTrue("No theme file found in assets/content", result.index.themes.isNotEmpty())
        val blocking = result.issues.filter { it.themeId == null || it.themeId !in result.draftThemeIds }
        assertTrue("Content problems:\n" + blocking.joinToString("\n"), blocking.isEmpty())
    }

    @Test
    fun everyReferencedMediaFileOpensFromThePackage() {
        val result = AssetContentSource(context).load()
        result.index.discoveries.forEach { d ->
            val url = d.mediaUrl ?: return@forEach
            val path = url
                .removePrefix(ContentLoader.IMAGE_ROOT.removeSuffix("content/media/"))
                .removePrefix(ContentLoader.VIDEO_ROOT.removeSuffix("content/media/"))
            context.assets.open(path).close()
        }
    }
}
