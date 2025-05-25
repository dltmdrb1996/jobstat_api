package com.wildrew.jobstat.core.core_monitoring

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment

@AutoConfiguration
@ConditionalOnClass(PrometheusMeterRegistry::class, MeterRegistry::class)
@ConditionalOnProperty(name = ["management.prometheus.metrics.export.enabled"], havingValue = "true", matchIfMissing = true) // Prometheus 익스포트 활성화 시
class CorePrometheusMetricsAutoConfiguration(
    private val environment: Environment,
) {
    private val log = LoggerFactory.getLogger(CorePrometheusMetricsAutoConfiguration::class.java)

    @Bean
    fun commonTagsMeterRegistryCustomizer(
        @Value("\${spring.application.name:unknown-app}") applicationName: String,
    ): MeterRegistryCustomizer<MeterRegistry> {
        val activeProfiles = environment.activeProfiles.joinToString(separator = "_", prefix = "", postfix = "").ifEmpty { "default" }
        val instanceId = System.getenv("HOSTNAME") ?: System.getenv("COMPUTERNAME") ?: "localhost"

        log.info(
            "Configuring common tags for Prometheus metrics. Application: '{}', Profiles: '{}', InstanceId: '{}'",
            applicationName,
            activeProfiles,
            instanceId,
        )

        return MeterRegistryCustomizer { registry ->
            registry.config().commonTags(
                listOf(
                    Tag.of("application", applicationName),
                    Tag.of("environment", activeProfiles),
                    Tag.of("instance_id", instanceId),
                ),
            )
        }
    }
}
