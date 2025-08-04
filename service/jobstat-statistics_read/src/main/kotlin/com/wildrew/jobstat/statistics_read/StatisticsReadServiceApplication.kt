package com.wildrew.jobstat.statistics_read

import com.wildrew.jobstat.statistics_read.common.TimeZoneConfig.Companion.KOREA_TIME_ZONE_ID
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.*

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableConfigurationProperties(MongoProperties::class)
class StatisticsReadServiceApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone(KOREA_TIME_ZONE_ID))
    System.setProperty("user.timezone", KOREA_TIME_ZONE_ID)

    runApplication<StatisticsReadServiceApplication>(*args)
}
