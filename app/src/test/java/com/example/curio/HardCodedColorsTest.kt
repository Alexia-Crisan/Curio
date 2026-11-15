package com.example.curio

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HardCodedColorsTest {

    private val roots = listOf(
        "src/main/java/com/example/curio/feature",
        "src/main/java/com/example/curio/ui/components",
    )

    private val gradientScreens = setOf("FeedCards.kt", "FeedScreen.kt", "TiltCard.kt", "LibraryScreen.kt")

    private val legacyFiles = setOf("feature/progress/TreeCard.kt", "ui/components/BadgeTree.kt")

    private val anyFixedColor = Regex("""Color\.(White|Black|Gray|LightGray|DarkGray|Red|Green|Blue|Yellow|Cyan|Magenta)\b|Color\(\s*0[xX]""")

    private val neutralGray = Regex("""Color\.(Gray|LightGray|DarkGray)\b""")

    private val translucentWhiteText = Regex("""Color\.White\.copy\(\s*alpha\s*=\s*0?\.([0-8][0-9]*)f""")

    private fun sources(): List<File> =
        roots.flatMap { root -> File(root).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }

    private fun relative(file: File): String = file.path.replace('\\', '/').substringAfter("com/example/curio/")

    private fun hits(files: List<File>, regex: Regex): List<String> =
        files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val code = line.substringBefore("//")
                if (regex.containsMatchIn(code)) "${relative(file)}:${index + 1}: ${line.trim()}" else null
            }
        }

    private fun screens() = sources().filter { it.name !in gradientScreens && relative(it) !in legacyFiles }

    private fun gradients() = sources().filter { it.name in gradientScreens }

    @Test fun sourceRootsExist() {
        assertTrue(sources().size > 20)
        assertTrue(gradients().size == gradientScreens.size)
    }

    @Test fun screensUseThemeColorsOnly() {
        val found = hits(screens(), anyFixedColor)
        assertTrue("Fixed colours outside the feed screens:\n" + found.joinToString("\n"), found.isEmpty())
    }

    @Test fun noScreenUsesGreyNamedColors() {
        val found = hits(sources().filter { relative(it) !in legacyFiles }, neutralGray)
        assertTrue("Color.Gray and friends:\n" + found.joinToString("\n"), found.isEmpty())
    }

    @Test fun whiteTextOnGradientsIsNeverMoreTransparentThanNinetyPercent() {
        val found = hits(gradients(), translucentWhiteText).filter { !it.contains("background(") }
        assertTrue("Translucent white text:\n" + found.joinToString("\n"), found.isEmpty())
    }

    @Test fun gradientScreensDrawTheirBackgroundFromAReadableAccent() {
        val cards = gradients().first { it.name == "FeedCards.kt" }.readText()
        assertTrue(cards.contains("AccentContrast.readable"))
        assertTrue(!cards.contains("Color(d.accentColor)"))
    }

    @Test fun libraryTabsAndEmptyStateUseThemeColors() {
        val library = sources().first { it.name == "LibraryScreen.kt" }.readText()
        assertTrue(!library.contains("Color.White.copy(alpha = 0.1f)"))
        assertTrue(!library.contains("Color.Gray"))
        assertTrue(library.contains("colorScheme.onPrimary"))
        assertTrue(library.contains("colorScheme.onSurfaceVariant"))
    }

    @Test fun libraryCardsUseAReadableAccent() {
        val library = sources().first { it.name == "LibraryScreen.kt" }.readText()
        assertTrue(library.contains("AccentContrast.readable"))
        assertTrue(!library.contains("Color(card.accentColor)"))
    }
}
