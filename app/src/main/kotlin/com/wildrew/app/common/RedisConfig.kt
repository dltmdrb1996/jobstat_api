//package com.wildrew.app.common
//
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.data.redis.connection.RedisConnectionFactory
//import org.springframework.data.redis.connection.RedisPassword
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
//import org.springframework.data.redis.core.StringRedisTemplate
//
//@Configuration
//class RedisConfig(
//    @Value("\${spring.data.redis.host}") private val host: String,
//    @Value("\${spring.data.redis.port}") private val port: Int,
//    @Value("\${spring.data.redis.database}") private val dataIndex: Int,
//    @Value("\${spring.data.redis.password}") private val password: String,
//) {
//    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
//
//    @Bean
//    fun createLettuceConnectionFactory(): RedisConnectionFactory {
//        log.info("RedisConfig: host=$host, port=$port, database=$dataIndex")
//        val redisStandaloneConfiguration = RedisStandaloneConfiguration()
//        redisStandaloneConfiguration.hostName = host
//        redisStandaloneConfiguration.port = port
//        redisStandaloneConfiguration.database = dataIndex
//        redisStandaloneConfiguration.password = RedisPassword.of(password)
//        redisStandaloneConfiguration.username = "default"
//        return LettuceConnectionFactory(redisStandaloneConfiguration)
//    }
//
//    @Bean
//    fun stringRedisTemplate(): StringRedisTemplate = StringRedisTemplate(createLettuceConnectionFactory())
//}
