package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentContrastTest {

    private val white = 0xFFFFFFFFL
    private val black = 0xFF000000L

    private fun grid(): List<Long> = buildList {
        for (r in 0..255 step 17) for (g in 0..255 step 17) for (b in 0..255 step 17) {
            add(0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
        }
    }

    private fun channels(argb: Long) = listOf((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)

    @Test fun luminanceOfBlackAndWhite() {
        assertEquals(0.0, AccentContrast.luminance(black), 1e-9)
        assertEquals(1.0, AccentContrast.luminance(white), 1e-9)
    }

    @Test fun contrastOfBlackAndWhiteIsTwentyOne() {
        assertEquals(21.0, AccentContrast.contrast(black, white), 1e-9)
        assertEquals(21.0, AccentContrast.contrastWithWhite(black), 1e-9)
        assertEquals(1.0, AccentContrast.contrastWithWhite(white), 1e-9)
    }

    @Test fun contrastIsSymmetric() {
        assertEquals(
            AccentContrast.contrast(0xFFEA580CL, 0xFF1E293BL),
            AccentContrast.contrast(0xFF1E293BL, 0xFFEA580CL),
            1e-12,
        )
    }

    @Test fun knownContrastValues() {
        assertEquals(3.56, AccentContrast.contrastWithWhite(0xFFEA580CL), 0.02)
        assertEquals(2.56, AccentContrast.contrastWithWhite(0xFF94A3B8L), 0.02)
        assertEquals(2.15, AccentContrast.contrastWithWhite(0xFFF59E0BL), 0.02)
        assertEquals(5.02, AccentContrast.contrastWithWhite(0xFF15803DL), 0.02)
    }

    @Test fun colorsThatAlreadyReadWellAreKeptExactly() {
        assertEquals(0xFF1E293BL, AccentContrast.readable(0xFF1E293BL))
        assertEquals(0xFF991B1BL, AccentContrast.readable(0xFF991B1BL))
        assertEquals(black, AccentContrast.readable(black))
    }

    @Test fun everyColorBecomesReadableWithWhiteText() {
        grid().forEach { c ->
            val out = AccentContrast.readable(c)
            assertTrue("%08X -> %08X".format(c, out), AccentContrast.contrastWithWhite(out) >= AccentContrast.TEXT_RATIO)
            assertEquals(0xFFL, out shr 24)
        }
    }

    @Test fun darkeningNeverBrightensAChannel() {
        grid().forEach { c ->
            val out = AccentContrast.readable(c)
            channels(c).zip(channels(out)).forEach { (before, after) -> assertTrue(after <= before) }
        }
    }

    @Test fun theResultIsNotDarkerThanNecessary() {
        grid().filter { AccentContrast.contrastWithWhite(it) < AccentContrast.TEXT_RATIO && it != black }.forEach { c ->
            val out = AccentContrast.readable(c)
            if (out != black) {
                assertTrue("%08X -> %08X".format(c, out), AccentContrast.contrastWithWhite(out) < AccentContrast.TEXT_RATIO + 0.5)
            }
        }
    }

    @Test fun theSpaceThemeAccentsBecomeReadable() {
        listOf(0xFF6366F1L, 0xFFEA580CL, 0xFF94A3B8L, 0xFFF59E0BL, 0xFF38BDF8L, 0xFF10B981L, 0xFF3B82F6L).forEach {
            assertTrue(AccentContrast.contrastWithWhite(AccentContrast.readable(it)) >= 6.0)
        }
    }

    @Test fun orangeKeepsItsHue() {
        val out = channels(AccentContrast.readable(0xFFEA580CL))
        assertEquals(0xAC.toDouble(), out[0].toDouble(), 3.0)
        assertEquals(0x41.toDouble(), out[1].toDouble(), 3.0)
        assertEquals(0x09.toDouble(), out[2].toDouble(), 3.0)
    }

    @Test fun readableIsIdempotent() {
        grid().forEach { c ->
            val once = AccentContrast.readable(c)
            assertEquals(once, AccentContrast.readable(once))
        }
    }

    @Test fun theAlphaOfTheInputIsIgnored() {
        assertEquals(AccentContrast.readable(0xFFEA580CL), AccentContrast.readable(0x80EA580CL))
        assertEquals(0xFFL, AccentContrast.readable(0x00F59E0BL) shr 24)
    }

    @Test fun aTransparentDarkInputComesBackOpaque() {
        assertEquals(0xFF1E293BL, AccentContrast.readable(0x001E293BL))
        assertEquals(0xFF1E293BL, AccentContrast.readable(0x801E293BL))
    }

    @Test fun anImpossibleRatioGivesBlack() {
        assertEquals(black, AccentContrast.readable(white, 21.0))
        assertEquals(black, AccentContrast.readable(0xFFF59E0BL, 25.0))
    }

    @Test fun aLowerRatioDarkensLess() {
        val soft = AccentContrast.readable(0xFFF59E0BL, 4.5)
        val strong = AccentContrast.readable(0xFFF59E0BL, 7.0)
        assertTrue(AccentContrast.luminance(soft) > AccentContrast.luminance(strong))
        assertTrue(AccentContrast.contrastWithWhite(soft) >= 4.5)
    }

    @Test fun darkenEdges() {
        assertEquals(0xFFEA580CL, AccentContrast.darken(0xFFEA580CL, 0.0))
        assertEquals(black, AccentContrast.darken(0xFFEA580CL, 1.0))
        assertEquals(black, AccentContrast.darken(0xFFEA580CL, 3.0))
        assertEquals(0xFFEA580CL, AccentContrast.darken(0xFFEA580CL, -2.0))
        assertEquals(0xFF808080L, AccentContrast.darken(white, 0.5))
    }

    @Test fun constantsMatchTheDocumentedRules() {
        assertEquals(6.0, AccentContrast.TEXT_RATIO, 0.0)
        assertEquals(0.9f, AccentContrast.MIN_TEXT_ALPHA, 0f)
    }
}
