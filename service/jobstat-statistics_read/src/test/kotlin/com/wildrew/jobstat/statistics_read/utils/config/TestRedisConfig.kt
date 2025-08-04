package com.wildrew.jobstat.statistics_read.utils.config

import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@TestConfiguration
@Testcontainers
class TestRedisConfig {
    companion object {
        private val log = LoggerFactory.getLogger(TestRedisConfig::class.java)
        private const val REDIS_IMAGE = "redis:7-alpine"
        const val REDIS_PORT = 6379

        @Container
        val redisContainer =
            GenericContainer(
                DockerImageName.parse(REDIS_IMAGE),
            ).withExposedPorts(REDIS_PORT)
                .withReuse(true)

        init {
            try {
                if (!redisContainer.isRunning) {
                    redisContainer.start()
                    log.debug("==== Test Redis Container Started ====")
                    log.debug("Redis Host: {}", redisContainer.host)
                    log.debug("Redis Port: {}", redisContainer.getMappedPort(REDIS_PORT))
                    log.debug("====================================")
                } else {
                    log.debug("==== Test Redis Container Already Running ====")
                }
            } catch (e: Exception) {
                log.error("Failed to start Redis container", e)
                throw IllegalStateException("Could not start Redis Testcontainer", e)
            }
        }
    }
}
