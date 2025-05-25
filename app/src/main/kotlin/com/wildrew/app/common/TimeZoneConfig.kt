package com.wildrew.app.common

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class TimeZoneConfig {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    companion object {
        const val KOREA_TIME_ZONE_ID = "Asia/Seoul"
    }

    @PostConstruct
    fun init() {
        TimeZone.setDefault(TimeZone.getTimeZone(KOREA_TIME_ZONE_ID))
        log.debug("애플리케이션 기본 시간대가 {}로 설정되었습니다.", TimeZone.getDefault().id)
    }
}
