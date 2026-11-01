package com.example.curio.data.user

import kotlinx.coroutines.flow.Flow
import com.example.curio.domain.DetailSaves
import com.example.curio.domain.ViewSource
import java.time.LocalDate

class UserActivityDetailSaves(private val activity: UserActivityRepository) : DetailSaves {
    override fun observeSaved(): Flow<Set<String>> = activity.observeSavedIds()

    override suspend fun setSaved(discoveryId: String, saved: Boolean, date: LocalDate) =
        activity.setSaved(
            discoveryId = discoveryId,
            saved = saved,
            source = ViewSource.DETAIL,
            date = date,
            recordActivity = true,
        )
}
