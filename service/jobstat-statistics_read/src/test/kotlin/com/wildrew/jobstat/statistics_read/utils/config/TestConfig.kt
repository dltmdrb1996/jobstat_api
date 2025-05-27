package com.wildrew.jobstat.statistics_read.utils.config

import com.wildrew.jobstat.statistics_read.core.StatsBulkCacheManager
import com.wildrew.jobstat.statistics_read.fake.FakeStatsBulkCacheManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestConfig {
    @Bean
    @Primary
    fun statsBulkCacheManager(): StatsBulkCacheManager = FakeStatsBulkCacheManager()
}
