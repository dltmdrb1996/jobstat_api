package com.example.jobstat.core.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import java.util.TimeZone

/**
 * 전체 애플리케이션에 한국 시간대(KST)를 적용하는 설정 클래스
 */
@Configuration
class TimeZoneConfig {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    companion object {
        const val KOREA_TIME_ZONE_ID = "Asia/Seoul"
    }

    /**
     * 애플리케이션 시작 시 기본 시간대를 한국 시간(KST)으로 설정
     */
    @PostConstruct
    fun init() {
        TimeZone.setDefault(TimeZone.getTimeZone(KOREA_TIME_ZONE_ID))
        log.info("애플리케이션 기본 시간대가 {}로 설정되었습니다.", TimeZone.getDefault().id)
    }
}
