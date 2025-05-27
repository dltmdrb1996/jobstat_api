package com.wildrew.jobstat.community_read.utils.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.system.measureTimeMillis

object TestUtils {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    fun logMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        log.debug("Memory Usage: Used=${usedMemory}MB, Total=${totalMemory}MB")
    }

    fun <T> measureTimeAndLog(
        operation: String,
        block: () -> T,
    ): T {
        val startTime = LocalDateTime.now()
        var result: T
        val executionTime =
            measureTimeMillis {
                result = block()
            }
        val endTime = LocalDateTime.now()

        log.debug(
            """
            Operation '$operation' completed:
            - Execution time: ${executionTime / 1000.0} seconds
            - Started at: $startTime
            - Ended at: $endTime
            - Duration: ${ChronoUnit.MILLIS.between(startTime, endTime)}ms
            """.trimIndent(),
        )

        return result
    }

    fun generateRandomString(length: Int): String {
        val chars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
}

data class TestMetrics(
    val operationName: String,
    val recordCount: Int,
    val executionTimeSeconds: Double,
    val recordsPerSecond: Double,
) {
    override fun toString(): String =
        """
        Test Metrics for '$operationName':
        - Total records processed: $recordCount
        - Execution time: $executionTimeSeconds seconds
        - Processing rate: $recordsPerSecond records/second
        """.trimIndent()
}
