package com.wildrew.app

import com.wildrew.app.common.TimeZoneConfig.Companion.KOREA_TIME_ZONE_ID
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.*

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableConfigurationProperties(MongoProperties::class)
@EnableScheduling
@EnableDiscoveryClient
@EntityScan( // app 모듈 내의 모든 JPA Entity 패키지를 지정
    basePackages = [
        "com.wildrew.app.auth.user.entity",
        "com.wildrew.app.community.board.entity",
        "com.wildrew.app.auth.email.entity",
        "com.wildrew.app.community.comment.entity",
        "com.wildrew.jobstat.core.core_event.dlt",
        "com.wildrew.jobstat.core.core_event.outbox",
    ],
)
@EnableJpaRepositories( // app 모듈 내의 모든 JPA Repository 패키지를 지정
    basePackages = [
        "com.wildrew.app.auth.user.repository",
        "com.wildrew.app.auth.email.repository",
        "com.wildrew.app.community.board.repository",
        "com.wildrew.app.community.comment.repository",
        "com.wildrew.jobstat.core.core_event.dlt",
        "com.wildrew.jobstat.core.core_event.outbox",
    ],
)
class JobstatApplication

fun main(args: Array<String>) {
    // JVM 전체에 대한 기본 시간대 설정
    TimeZone.setDefault(TimeZone.getTimeZone(KOREA_TIME_ZONE_ID))

    // 동일한 효과의 JVM 시스템 속성 설정 (이중 보호)
    System.setProperty("user.timezone", KOREA_TIME_ZONE_ID)

    runApplication<JobstatApplication>(*args)
}
