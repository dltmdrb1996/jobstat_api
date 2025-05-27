package com.wildrew.jobstat.core.core_monitoring

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@AutoConfiguration
@ConditionalOnProperty(name = ["jobstat.core.monitoring.enabled"], havingValue = "true", matchIfMissing = false)
@ConditionalOnClass(Sentry::class)
class CoreMonitoringAutoConfiguration(
    private val environment: Environment,
    @Value("\${jobstat.core.monitoring.cpu-threshold:80.0}") private val cpuThreshold: Double,
    @Value("\${logging.level.root:INFO}") private val defaultRootLogLevel: String,
    @Value("\${jobstat.core.monitoring.sentry.enabled:true}") private val sentryEnabled: Boolean,
    @Value("\${jobstat.core.monitoring.dynamic-log-level.enabled:false}") private val dynamicLogLevelEnabled: Boolean,
) {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val isProd by lazy { environment.activeProfiles.contains("prod") }

    @Scheduled(fixedRateString = "\${jobstat.core.monitoring.schedule.fixed-rate:30000}")
    fun monitorSystemResources() {
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        if (osBean is com.sun.management.OperatingSystemMXBean) {
            val cpuLoad = osBean.cpuLoad * 100.0

            if (cpuLoad > cpuThreshold) {
                if (dynamicLogLevelEnabled) {
                    (LoggerFactory.getILoggerFactory() as? LoggerContext)?.getLogger(Logger.ROOT_LOGGER_NAME)?.level = Level.DEBUG
                    log.warn("Temporarily changed root log level to DEBUG due to high CPU usage.")
                }

                val systemInfo = collectSystemInfo(osBean)

                if (sentryEnabled && isProd && Sentry.isEnabled()) {
                    Sentry.withScope { scope ->
                        scope.level = SentryLevel.WARNING
                        scope.setExtra("cpu_usage", String.format("%.2f%%", cpuLoad))
                        scope.setExtra("system_info", systemInfo.toString())

                        val event =
                            SentryEvent().apply {
                                this.message =
                                    Message().apply {
                                        this.message = "High CPU Usage Alert: %.2f%%".format(cpuLoad)
                                    }
                                this.level = SentryLevel.WARNING
                            }
                        Sentry.captureEvent(event)
                    }
                }
                log.error("High CPU Usage: {}% - System Info: {}", String.format("%.2f", cpuLoad), systemInfo)

                if (dynamicLogLevelEnabled) {
                    scheduleLogLevelReset()
                }
            }
        }
    }

    private fun collectSystemInfo(osBean: com.sun.management.OperatingSystemMXBean): Map<String, Any> =
        buildMap {
            put("total_physical_memory_size", osBean.totalMemorySize)
            put("free_physical_memory_size", osBean.freeMemorySize)
            put("process_cpu_load", String.format("%.2f%%", osBean.processCpuLoad * 100.0))
            put("system_cpu_load", String.format("%.2f%%", osBean.cpuLoad * 100.0))

            val threadBean = ManagementFactory.getThreadMXBean()
            put("thread_count", threadBean.threadCount)
            put("peak_thread_count", threadBean.peakThreadCount)
        }

    private fun scheduleLogLevelReset() {
        Executors.newSingleThreadScheduledExecutor().apply {
            schedule({
                (LoggerFactory.getILoggerFactory() as? LoggerContext)?.apply {
                    getLogger(Logger.ROOT_LOGGER_NAME).level = Level.toLevel(defaultRootLogLevel, Level.INFO)
                }
                log.warn("Root log level reset to {}.", defaultRootLogLevel)
            }, 30, TimeUnit.MINUTES)
            shutdown()
        }
    }
}
