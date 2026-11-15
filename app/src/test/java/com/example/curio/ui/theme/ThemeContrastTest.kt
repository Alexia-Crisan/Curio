package com.example.curio.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.curio.domain.AccentContrast
import java.io.File

class ThemeContrastTest {

    private val source = File("src/main/java/com/example/curio/ui/theme/Color.kt").readText()

    private fun palette(name: String): Map<String, Long> {
        val start = source.indexOf("val $name =")
        assertTrue("missing $name in Color.kt", start >= 0)
        val end = source.indexOf("\n)", start)
        assertTrue("unterminated $name in Color.kt", end > start)
        return Regex("""(\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)""")
            .findAll(source.substring(start, end))
            .associate { it.groupValues[1] to it.groupValues[2].toLong(16) }
    }

    private val surfaces = listOf(
        "background", "surface", "surfaceContainerLowest", "surfaceContainerLow",
        "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest",
    )

    private val roles = listOf("primary", "secondary", "tertiary", "error")

    private fun textPairs(): List<Pair<String, String>> = buildList {
        roles.forEach { add("on" + it.replaceFirstChar(Char::uppercase) to it) }
        roles.forEach { add("on" + it.replaceFirstChar(Char::uppercase) + "Container" to it + "Container") }
        add("onBackground" to "background")
        surfaces.forEach { add("onSurface" to it) }
        surfaces.forEach { add("onSurfaceVariant" to it) }
        add("onSurface" to "surfaceVariant")
        add("onSurfaceVariant" to "surfaceVariant")
        add("inverseOnSurface" to "inverseSurface")
        add("primary" to "background")
        add("primary" to "surfaceContainerLowest")
        add("error" to "background")
    }

    private fun nonTextPairs(): List<Pair<String, String>> = listOf(
        "outline" to "background",
        "outline" to "surfaceContainerLowest",
        "primary" to "surfaceContainerLowest",
    )

    private fun failures(colors: Map<String, Long>, pairs: List<Pair<String, String>>, minimum: Double): List<String> =
        pairs.mapNotNull { (foreground, background) ->
            val fg = colors[foreground]
            val bg = colors[background]
            when {
                fg == null || bg == null -> "$foreground on $background: token missing"
                AccentContrast.contrast(fg, bg) < minimum ->
                    "$foreground on $background: %.2f < $minimum".format(AccentContrast.contrast(fg, bg))
                else -> null
            }
        }

    private fun check(name: String) {
        val colors = palette(name)
        assertTrue("$name has too few colours", colors.size >= 35)
        val bad = failures(colors, textPairs(), 4.5) + failures(colors, nonTextPairs(), 3.0)
        assertTrue("$name:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test fun lightSchemeTextAndControlsAreReadable() = check("LightColors")

    @Test fun darkSchemeTextAndControlsAreReadable() = check("DarkColors")

    private fun status(name: String) {
        val colors = palette(name)
        val bad = listOf("success", "warning", "info", "reward").flatMap { role ->
            val on = "on" + role.replaceFirstChar(Char::uppercase)
            val container = role + "Container"
            val onContainer = "on" + role.replaceFirstChar(Char::uppercase) + "Container"
            failures(colors, listOf(on to role, onContainer to container), 4.5)
        }
        assertTrue("$name:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test fun lightStatusColorsAreReadable() = status("LightCurioColors")

    @Test fun darkStatusColorsAreReadable() = status("DarkCurioColors")

    private fun windowBackground(folder: String): Long? {
        val file = File("src/main/res/$folder/colors.xml")
        if (!file.exists()) return null
        val match = Regex("""<color name="window_background">#([0-9A-Fa-f]{6})</color>""").find(file.readText()) ?: return null
        return 0xFF000000L or match.groupValues[1].toLong(16)
    }

    @Test fun windowBackgroundMatchesThePageColorInBothThemes() {
        assertEquals(palette("LightColors").getValue("background"), windowBackground("values"))
        assertEquals(palette("DarkColors").getValue("background"), windowBackground("values-night"))
    }

    @Test fun lightAndDarkSchemesDefineTheSameTokens() {
        assertEquals(palette("LightColors").keys, palette("DarkColors").keys)
        assertEquals(palette("LightCurioColors").keys, palette("DarkCurioColors").keys)
    }

    @Test fun lightPageIsLightAndDarkPageIsDark() {
        assertTrue(AccentContrast.luminance(palette("LightColors").getValue("background")) > 0.8)
        assertTrue(AccentContrast.luminance(palette("DarkColors").getValue("background")) < 0.05)
    }

    @Test fun textOnThePageIsNeverTheSameShadeAsThePage() {
        listOf("LightColors", "DarkColors").forEach { name ->
            val colors = palette(name)
            assertTrue(AccentContrast.contrast(colors.getValue("onBackground"), colors.getValue("background")) >= 7.0)
        }
    }
}
