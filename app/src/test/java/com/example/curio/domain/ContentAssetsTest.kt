package com.example.curio.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentAssetsTest {
    private val contentDir: File = listOf(
        File("src/main/assets/content"),
        File("app/src/main/assets/content"),
    ).first { it.isDirectory }

    private val jsonFiles: List<File> =
        contentDir.listFiles { f -> f.isFile && f.extension == "json" }.orEmpty().sortedBy { it.name }

    private fun contentFile(f: File) = ContentFile(f.name, f.readText(Charsets.UTF_8))

    private val result: ContentLoadResult by lazy { ContentLoader.load(jsonFiles.map(::contentFile)) }

    @Test
    fun everyThemeThatIsNotADraftLoadsWithoutIssues() {
        val blocking = result.issues.filter { it.themeId == null || it.themeId !in result.draftThemeIds }
        assertTrue("Content problems:\n" + blocking.joinToString("\n"), blocking.isEmpty())
    }

    @Test
    fun everyMediaFileThatIsReferencedExists() {
        val missing = result.index.discoveries.mapNotNull { d ->
            val url = d.mediaUrl ?: return@mapNotNull null
            val relative = url.removePrefix(ContentLoader.IMAGE_ROOT).removePrefix(ContentLoader.VIDEO_ROOT)
            if (File(contentDir, "media/$relative").isFile) null else "${d.id} -> media/$relative"
        }
        assertTrue("Missing media files:\n" + missing.joinToString("\n"), missing.isEmpty())
    }

    @Test
    fun eachThemeFileIsNamedAfterItsThemeId() {
        val wrong = jsonFiles.mapNotNull { f ->
            val themes = ContentLoader.load(listOf(contentFile(f))).index.themes
            val id = themes.singleOrNull()?.id
            if (id == f.nameWithoutExtension) null else "${f.name} defines ${id ?: "no readable theme"}"
        }
        assertTrue("File names must match the theme id:\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }

    @Test
    fun everyAvailableThemeHasOnlyCompleteTopics() {
        result.index.themes.filter { it.available }.forEach { theme ->
            theme.topics.forEach { topic ->
                assertEquals("${topic.id} must have $DAILY_FEED_SIZE discoveries", DAILY_FEED_SIZE, result.index.discoveriesOf(topic.id).size)
            }
        }
    }

    @Test
    fun atLeastOneThemeIsAvailable() {
        assertTrue(result.index.themes.any { it.available })
    }
}
