package com.example.curio.testing

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class MutableClock(var nowMs: Long, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(nowMs, zone)
    override fun instant(): Instant = Instant.ofEpochMilli(nowMs)
    override fun millis(): Long = nowMs
}
