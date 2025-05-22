package com.wildrew.jobstat.core.core_monitoring.com.wildrew.jobstat.core.core_monitoring

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import io.sentry.Sentry // Sentry 의존성 필요
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
// import org.springframework.scheduling.annotation.EnableScheduling // 서비스 레벨에서 설정 권장
import org.springframework.scheduling.annotation.Scheduled
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@AutoConfiguration // AutoConfiguration으로 변경
@ConditionalOnProperty(name = ["jobstat.core.monitoring.enabled"], havingValue = "true", matchIfMissing = false) // 기본 비활성화
@ConditionalOnClass(Sentry::class) // Sentry 사용 시
class CoreMonitoringAutoConfiguration( // @Scheduled 메소드를 가지므로 @Configuration으로 두거나, 별도 @Component로 분리 후 AutoConfiguration에서 Bean 등록
    private val environment: Environment,
    @Value("\${jobstat.core.monitoring.cpu-threshold:80.0}") private val cpuThreshold: Double,
    @Value("\${logging.level.root:INFO}") private val defaultRootLogLevel: String, // 애플리케이션의 기본 로그 레벨 참조
    @Value("\${jobstat.core.monitoring.sentry.enabled:true}") private val sentryEnabled: Boolean,
    @Value("\${jobstat.core.monitoring.dynamic-log-level.enabled:false}") private val dynamicLogLevelEnabled: Boolean
) {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val isProd by lazy { environment.activeProfiles.contains("prod") }

    @Scheduled(fixedRateString = "\${jobstat.core.monitoring.schedule.fixed-rate:30000}")
    fun monitorSystemResources() { // public 이어야 AOP 프록시 동작
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        if (osBean is com.sun.management.OperatingSystemMXBean) {
            val cpuLoad = osBean.cpuLoad * 100.0 // Double로 연산

            if (cpuLoad > cpuThreshold) {
                if (dynamicLogLevelEnabled) { // 동적 로그 레벨 변경 기능 활성화 시
                    (LoggerFactory.getILoggerFactory() as? LoggerContext)?.getLogger(Logger.ROOT_LOGGER_NAME)?.level = Level.DEBUG
                    log.warn("Temporarily changed root log level to DEBUG due to high CPU usage.")
                }

                val systemInfo = collectSystemInfo(osBean)

                if (sentryEnabled && isProd && Sentry.isEnabled()) {
                    Sentry.withScope { scope ->
                        scope.level = SentryLevel.WARNING
                        scope.setExtra("cpu_usage", String.format("%.2f%%", cpuLoad))
                        scope.setExtra("system_info", systemInfo.toString())

                        val event = SentryEvent().apply {
                            this.message = Message().apply {
                                this.message = "High CPU Usage Alert: %.2f%%".format(cpuLoad)
                            }
                            this.level = SentryLevel.WARNING // 이벤트 레벨 명시
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
            put("total_physical_memory_size", osBean.totalMemorySize) // totalPhysicalMemorySize 사용 권장
            put("free_physical_memory_size", osBean.freeMemorySize) // freePhysicalMemorySize 사용 권장
            put("process_cpu_load", String.format("%.2f%%", osBean.processCpuLoad * 100.0))
            put("system_cpu_load", String.format("%.2f%%", osBean.cpuLoad * 100.0)) // systemCpuLoad 대신 getCpuLoad() 사용

            val threadBean = ManagementFactory.getThreadMXBean()
            put("thread_count", threadBean.threadCount)
            put("peak_thread_count", threadBean.peakThreadCount)
        }

    private fun scheduleLogLevelReset() {
        Executors.newSingleThreadScheduledExecutor().apply {
            schedule({
                (LoggerFactory.getILoggerFactory() as? LoggerContext)?.apply {
                    getLogger(Logger.ROOT_LOGGER_NAME).level = Level.toLevel(defaultRootLogLevel, Level.INFO) // 기본값 INFO
                }
                log.warn("Root log level reset to {}.", defaultRootLogLevel)
            }, 30, TimeUnit.MINUTES)
            shutdown() // 스케줄러 종료 중요
        }
    }
}