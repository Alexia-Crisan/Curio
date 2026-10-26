package com.example.curio.testing

import com.example.curio.domain.TreeVisitStore

class FakeTreeVisitStore(var last: Long? = null) : TreeVisitStore {
    val marks = mutableListOf<Long>()
    var clears = 0

    override suspend fun lastVisitedAt(): Long? = last

    override fun markVisited(nowMs: Long) {
        marks += nowMs
        last = nowMs
    }

    override fun clear() {
        clears++
        last = null
    }
}
