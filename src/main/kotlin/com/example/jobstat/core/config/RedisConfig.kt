package com.example.jobstat.core.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
class RedisConfig(
    @Value("\${spring.data.redis.host}") private val host: String,
    @Value("\${spring.data.redis.port}") private val port: Int,
    @Value("\${spring.data.redis.database}") private val dataIndex: Int,
) {
    private val rankingTtl = java.time.Duration.ofDays(32)
    private val defaultTtl = java.time.Duration.ofMinutes(30)

    @Bean
    fun createLettuceConnectionFactory(): RedisConnectionFactory {
        val redisStandaloneConfiguration = RedisStandaloneConfiguration()
        redisStandaloneConfiguration.hostName = host
        redisStandaloneConfiguration.port = port
        redisStandaloneConfiguration.database = dataIndex
        return LettuceConnectionFactory(redisStandaloneConfiguration)
    }

    @Bean
    fun stringRedisTemplate(): StringRedisTemplate = StringRedisTemplate(createLettuceConnectionFactory())

}
