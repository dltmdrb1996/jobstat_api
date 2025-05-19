package com.example.jobstat

import com.example.jobstat.core.core_config.TimeZoneConfig.Companion.KOREA_TIME_ZONE_ID
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.TimeZone

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableJpaAuditing
@EnableConfigurationProperties(MongoProperties::class)
@EnableScheduling
class JobstatApplication

/**
 * 애플리케이션 진입점
 * 시스템 시간대를 한국 시간(KST)으로 설정하고 애플리케이션 시작
 */
fun main(args: Array<String>) {
    // JVM 전체에 대한 기본 시간대 설정
    TimeZone.setDefault(TimeZone.getTimeZone(KOREA_TIME_ZONE_ID))

    // 동일한 효과의 JVM 시스템 속성 설정 (이중 보호)
    System.setProperty("user.timezone", KOREA_TIME_ZONE_ID)

    runApplication<JobstatApplication>(*args)
}
