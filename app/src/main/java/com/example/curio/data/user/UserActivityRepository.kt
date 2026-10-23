package com.example.curio.data.user

import com.example.curio.domain.ActivitySnapshot
import com.example.curio.domain.ActivitySnapshotBuilder
import com.example.curio.domain.ContentIndex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class UserActivityRepository(
    private val viewEventDao: ViewEventDao,
    private val savedIdsFlow: Flow<Set<String>>,
    private val contentIndex: ContentIndex,
) {
    fun observeSnapshot(): Flow<ActivitySnapshot> {
        return combine(
            viewEventDao.observeAll(),
            savedIdsFlow,
        ) { eventEntities, savedIds ->
            val events = eventEntities.map { it.toDomain() }
            ActivitySnapshotBuilder.build(
                events = events,
                savedIds = savedIds,
                days = emptyList(),
                index = contentIndex,
            )
        }
    }
}
