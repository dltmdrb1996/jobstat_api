package com.example.jobstat.core.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@EnableScheduling
@Configuration
class MonitoringConfig(
    private val environment: Environment,
) {
    companion object {
        private const val CPU_THRESHOLD = 80.0
    }

    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val isProd = environment.activeProfiles.contains("prod")

    @Value("\${logging.level.root:INFO}")
    private lateinit var defaultLogLevel: String

    @Scheduled(fixedRate = 30000) // 30초마다 체크
    fun monitorSystemResources() {
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        if (osBean is com.sun.management.OperatingSystemMXBean) {
            val cpuLoad = osBean.cpuLoad * 100

            if (cpuLoad > CPU_THRESHOLD) {
                // 로그 레벨 변경
                (LoggerFactory.getILoggerFactory() as LoggerContext).apply {
                    getLogger(Logger.ROOT_LOGGER_NAME).level = Level.DEBUG
                }

                // 시스템 정보 수집
                val systemInfo = collectSystemInfo(osBean)

                // Sentry에 이벤트 전송
                Sentry.withScope { scope ->
                    scope.level = SentryLevel.WARNING
                    scope.setExtra("cpu_usage", cpuLoad.toString())
                    scope.setExtra("system_info", systemInfo.toString())

                    val event =
                        SentryEvent().apply {
                            message =
                                Message().apply {
                                    message = "High CPU Usage Alert: %.2f%%".format(cpuLoad)
                                }
                        }

                    if (isProd) Sentry.captureEvent(event)
                }

                log.error("High CPU Usage: $cpuLoad% - System Info: $systemInfo")

                // 30분 후 로그 레벨 복구
                scheduleLogLevelReset()
            }
        }
    }

    private fun collectSystemInfo(osBean: com.sun.management.OperatingSystemMXBean): Map<String, Any> =
        buildMap {
            put("total_memory", osBean.totalMemorySize)
            put("free_memory", osBean.freeMemorySize)
            put("process_cpu_load", osBean.processCpuLoad)

            val threadBean = ManagementFactory.getThreadMXBean()
            put("thread_count", threadBean.threadCount)

            // 활성 스레드 정보
            val threadInfo = threadBean.dumpAllThreads(true, true)
            put("thread_dump", threadInfo.map { it.threadName to it.threadState }.toMap())
        }

    private fun scheduleLogLevelReset() {
        Executors.newSingleThreadScheduledExecutor().apply {
            schedule({
                (LoggerFactory.getILoggerFactory() as LoggerContext).apply {
                    getLogger(Logger.ROOT_LOGGER_NAME).level = Level.valueOf(defaultLogLevel)
                }
            }, 30, TimeUnit.MINUTES)
            shutdown()
        }
    }
}
