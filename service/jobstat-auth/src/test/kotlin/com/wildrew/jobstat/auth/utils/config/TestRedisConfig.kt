package com.wildrew.jobstat.auth.utils.config

import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@TestConfiguration
@Testcontainers // Testcontainers 활성화
class TestRedisConfig {
    companion object {
        private val log = LoggerFactory.getLogger(TestRedisConfig::class.java)
        private const val REDIS_IMAGE = "redis:7-alpine" // 사용할 Redis 이미지 지정 (버전 변경 가능)
        const val REDIS_PORT = 6379

        @Container // Testcontainers가 관리할 컨테이너임을 명시
        val redisContainer =
            GenericContainer(
                DockerImageName.parse(REDIS_IMAGE),
            ).withExposedPorts(REDIS_PORT) // Redis 기본 포트 노출
                .withReuse(true) // 컨테이너 재사용 설정 (선택 사항)

        // 클래스 로딩 시점에 컨테이너 시작
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
