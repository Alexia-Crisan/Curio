package com.example.curio.data.user

import com.example.curio.domain.ViewEvent
import com.example.curio.domain.ViewSource
import java.time.LocalDate

fun ViewEventEntity.toDomain() = ViewEvent(
    discoveryId, LocalDate.parse(date), firstViewedAt, dwellMs, completed, skipped,
    ViewSource.valueOf(source), detailDwellMs,
)

fun ViewEvent.toEntity() = ViewEventEntity(
    discoveryId, date.toString(), firstViewedAt, dwellMs, completed, skipped, source.name, detailDwellMs,
)
