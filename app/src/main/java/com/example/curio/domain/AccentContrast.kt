package com.example.curio.domain

import kotlin.math.pow

object AccentContrast {
    const val TEXT_RATIO = 6.0
    const val MIN_TEXT_ALPHA = 0.9f

    fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF).toDouble() / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    fun contrast(first: Long, second: Long): Double {
        val a = luminance(first)
        val b = luminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    fun contrastWithWhite(argb: Long): Double = contrast(argb, 0xFFFFFFFFL)

    fun darken(argb: Long, amount: Double): Long {
        val t = amount.coerceIn(0.0, 1.0)
        fun channel(shift: Int): Long = Math.round(((argb shr shift) and 0xFF) * (1.0 - t)).coerceIn(0L, 255L)
        return 0xFF000000L or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    fun readable(argb: Long, minRatio: Double = TEXT_RATIO): Long {
        val opaque = 0xFF000000L or (argb and 0xFFFFFFL)
        if (contrastWithWhite(opaque) >= minRatio) return opaque
        var low = 0.0
        var high = 1.0
        repeat(30) {
            val mid = (low + high) / 2.0
            if (contrastWithWhite(darken(opaque, mid)) >= minRatio) high = mid else low = mid
        }
        return darken(opaque, high)
    }
}
