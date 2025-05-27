package com.wildrew.jobstat.community_read

import com.wildrew.jobstat.community_read.common.TimeZoneConfig.Companion.KOREA_TIME_ZONE_ID
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.*

@SpringBootApplication
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@EnableScheduling
@EnableFeignClients
@EntityScan(
    basePackages = [
        "com.wildrew.jobstat.core.core_event.dlt",
        "com.wildrew.jobstat.core.core_event.outbox",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "com.wildrew.jobstat.core.core_event.dlt",
        "com.wildrew.jobstat.core.core_event.outbox",
    ],
)
class CommunityReadServiceApplication

fun main(args: Array<String>) {
    // JVM 전체에 대한 기본 시간대 설정
    TimeZone.setDefault(TimeZone.getTimeZone(KOREA_TIME_ZONE_ID))
    System.setProperty("user.timezone", KOREA_TIME_ZONE_ID)

    runApplication<CommunityReadServiceApplication>(*args)
}
