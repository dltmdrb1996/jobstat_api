package com.wildrew.jobstat.notification

import com.wildrew.jobstat.auth.common.TimeZoneConfig.Companion.KOREA_TIME_ZONE_ID
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.*

@SpringBootApplication
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@EnableScheduling
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
class NotificationServiceApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone(KOREA_TIME_ZONE_ID))
    System.setProperty("user.timezone", KOREA_TIME_ZONE_ID)

    runApplication<NotificationServiceApplication>(*args)
}
