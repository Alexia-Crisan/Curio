package com.example.curio.domain

interface TreeVisitStore {
    suspend fun lastVisitedAt(): Long?
    fun markVisited(nowMs: Long)
    fun clear()

    companion object {
        val None: TreeVisitStore = object : TreeVisitStore {
            override suspend fun lastVisitedAt(): Long? = null
            override fun markVisited(nowMs: Long) = Unit
            override fun clear() = Unit
        }
    }
}
