package com.wildrew.app.common

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT3M")
class SchedulerConfig {
    @Value("\${shedlock.redis.key.env-prefix:default}")
    private lateinit var shedlockEnvPrefix: String

    @Bean
    fun lockProvider(connectionFactory: RedisConnectionFactory): LockProvider = RedisLockProvider(connectionFactory, shedlockEnvPrefix)
}
