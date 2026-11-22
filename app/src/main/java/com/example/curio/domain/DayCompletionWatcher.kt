package com.example.curio.domain

/**
 * True exactly once: when today goes from not complete to complete while the feed is open.
 * The first value is only remembered, so opening the app on an already complete day does not fire.
 */
class DayCompletionWatcher {
    private var last: Boolean? = null

    fun update(completeNow: Boolean): Boolean {
        val fire = last == false && completeNow
        last = completeNow
        return fire
    }
}
