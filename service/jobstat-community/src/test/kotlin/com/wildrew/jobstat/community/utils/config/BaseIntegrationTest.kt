package com.wildrew.jobstat.community.utils.config

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import kotlin.system.measureTimeMillis

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Import(
    value = [
        TestMysqlConfig::class,
        TestRedisConfig::class,
        DockerTestConfig::class,
        TestEventConfig::class,
    ],
)
@TestPropertySource(
    properties = [
        "spring.cloud.config.enabled=false",
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.enabled=false",
        "batch.chunk-size=2000",
        "batch.max-threads=10",
        "jobstat.core.token.jwt.secret=test-jwt-secret-key-that-is-long-enough-for-hmac-sha256",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true",
        "spring.data.redis.username=",
        "spring.data.redis.password=",
        "ddns.domain=http://localhost:8080",
        "spring.mail.username=test@gamil.com",
        "spring.mail.password=test-password",
        "jobstat.core.security.admin.username=admin",
        "jobstat.core.security.admin.password=admin",
        "spring.cloud.bootstrap.enabled=false",
        "spring.cloud.config.enabled=false",
        "app.server.url=http://localhost:8081",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
    ],
)
abstract class BaseIntegrationTest {
    protected fun logTestStart(testName: String) {
        TestUtils.logMemoryUsage()
        log.debug("=== Starting test: $testName ===")
    }

    protected fun logTestEnd(testName: String) {
        TestUtils.logMemoryUsage()
        log.debug("=== Completed test: $testName ===")
    }

    companion object {
        private val log: Logger by lazy { LoggerFactory.getLogger(BaseIntegrationTest::class.java) }

        @JvmStatic
        @DynamicPropertySource
        fun redisProperties(registry: DynamicPropertyRegistry) {
            val container = TestRedisConfig.redisContainer
            if (!container.isRunning) {
                log.warn("Redis container is not running for property source setup!")
                return
            }
            val host = container.host
            val port = container.getMappedPort(TestRedisConfig.REDIS_PORT)
            registry.add("spring.data.redis.host") { host }
            registry.add("spring.data.redis.port") { port.toString() } // 포트는 문자열로 전달
            log.debug("Dynamically set Redis Host: {}, Port: {}", host, port)
        }
    }
}

abstract class BatchOperationTestSupport : BaseIntegrationTest() {
    protected val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    protected val executionTimes = mutableMapOf<String, Double>()
    var testStartTime: Long = 0

    @BeforeEach
    fun setup() {
        testStartTime = System.currentTimeMillis()
        TestUtils.logMemoryUsage()
    }

    @AfterEach
    fun tearDown() {
        val testDuration = (System.currentTimeMillis() - testStartTime) / 1000.0
        log.debug("Test completed in $testDuration seconds")
        TestUtils.logMemoryUsage()
    }

    protected abstract fun cleanupTestData()

    protected fun executeWithTiming(
        operationName: String,
        recordCount: Int = 0,
        operation: () -> Unit,
    ): TestMetrics {
        val timeInMillis = measureTimeMillis { operation() }
        val timeInSeconds = timeInMillis / 1000.0
        executionTimes[operationName] = timeInSeconds

        val recordsPerSecond = if (recordCount > 0 && timeInSeconds > 0) recordCount / timeInSeconds else 0.0
        return TestMetrics(operationName, recordCount, timeInSeconds, recordsPerSecond)
    }

    protected fun printExecutionSummary() {
        log.debug("=== Execution Summary ===")
        executionTimes.forEach { (operation, time) ->
            log.debug("$operation: $time seconds")
        }
        TestUtils.logMemoryUsage()
    }

    @Transactional
    protected fun <R> executeInTransaction(operation: () -> R): R = operation()

    protected fun retryOperation(
        maxAttempts: Int = 3,
        delayMs: Long = 1000,
        operation: () -> Unit,
    ) {
        var attempt = 1
        var lastException: Exception? = null

        while (attempt <= maxAttempts) {
            try {
                operation()
                return
            } catch (e: Exception) {
                lastException = e
                log.warn("Attempt $attempt failed: ${e.message}")
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(delayMs)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw RuntimeException("Retry interrupted", ie)
                    }
                }
                attempt++
            }
        }
        throw RuntimeException("Operation failed after $maxAttempts attempts", lastException)
    }
}
