package com.example.jobstat.utils.config

import com.example.jobstat.core.cache.StatsBulkCacheManager
import com.example.jobstat.statistics_read.fake.FakeStatsBulkCacheManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestConfig {
    @Bean
    @Primary
    fun statsBulkCacheManager(): StatsBulkCacheManager = FakeStatsBulkCacheManager()
}
