package com.example.jobstat.utils

import java.time.*

/**
 * 테스트에서 "현재 시각"을 임의로 제어하기 위해 사용하는 Clock 구현.
 * - 기본적으로 instant는 'Instant.now()'로 시작
 * - advanceXxx() 메서드 등을 통해 시간을 원하는 만큼 이동 가능
 * - setInstant()로 특정 시점(Instant)을 직접 설정 가능
 */
class TestClock(
    private var instant: Instant = Instant.now(), // 초기 값 (기본=현재 시간)
) : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = this

    /**
     * Clock이 반환할 "현재 시각"을 정의.
     */
    override fun instant(): Instant = instant

    /**
     * 특정 Instant로 시간을 강제 설정.
     */
    fun setInstant(newInstant: Instant) {
        this.instant = newInstant
    }

    /**
     * 주어진 Duration 만큼 시간을 앞으로 이동.
     */
    fun advance(duration: Duration) {
        this.instant = this.instant.plus(duration)
    }

    /**
     * 아래는 자주 쓰는 단위를 위한 편의 메서드들
     */
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
