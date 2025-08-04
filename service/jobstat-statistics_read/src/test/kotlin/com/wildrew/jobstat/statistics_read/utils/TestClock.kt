package com.wildrew.jobstat.statistics_read.utils

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class TestClock(
    private var instant: Instant = Instant.now(),
) : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = instant

    fun setInstant(newInstant: Instant) {
        this.instant = newInstant
    }

    fun advance(duration: Duration) {
        this.instant = this.instant.plus(duration)
    }

    fun advanceSeconds(seconds: Long) {
        advance(Duration.ofSeconds(seconds))
    }

    fun advanceMinutes(minutes: Long) {
        advance(Duration.ofMinutes(minutes))
    }

    fun advanceHours(hours: Long) {
        advance(Duration.ofHours(hours))
    }

    fun advanceDays(days: Long) {
        advance(Duration.ofDays(days))
    }
}
