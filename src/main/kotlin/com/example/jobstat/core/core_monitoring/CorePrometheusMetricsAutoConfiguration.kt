package com.example.jobstat.core.core_monitoring // 예시 패키지

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.MeterBinder
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
@ConditionalOnClass(PrometheusMeterRegistry::class, MeterRegistry::class) // Micrometer 및 Prometheus 관련 클래스 존재 시
@ConditionalOnProperty(name = ["management.prometheus.metrics.export.enabled"], havingValue = "true", matchIfMissing = true) // Prometheus 익스포트 활성화 시
class CorePrometheusMetricsAutoConfiguration(
    private val environment: Environment // 현재 프로파일 등 환경 정보 접근용
) {

    private val log = LoggerFactory.getLogger(CorePrometheusMetricsAutoConfiguration::class.java)

    // spring.application.name은 기본적으로 스프링 부트가 설정하므로,
    // 라이브러리에서 커스텀 프로퍼티로 받을 필요 없이 Environment에서 가져올 수 있음
    // @Value("\${spring.application.name:unknown-service}")
    // private lateinit var applicationName: String;

    @Bean
    fun commonTagsMeterRegistryCustomizer(
        @Value("\${spring.application.name:unknown-app}") applicationName: String // 빈 메소드 파라미터로 주입 가능
    ): MeterRegistryCustomizer<MeterRegistry> {
        val activeProfiles = environment.activeProfiles.joinToString(separator = "_", prefix = "", postfix = "").ifEmpty { "default" }
        val instanceId = System.getenv("HOSTNAME") ?: System.getenv("COMPUTERNAME") ?: "localhost"

        log.info(
            "Configuring common tags for Prometheus metrics. Application: '{}', Profiles: '{}', InstanceId: '{}'",
            applicationName, activeProfiles, instanceId
        )

        return MeterRegistryCustomizer { registry ->
            registry.config().commonTags(
                listOf(
                    Tag.of("application", applicationName),
                    Tag.of("environment", activeProfiles), // 현재 활성 프로파일을 태그로 추가
                    Tag.of("instance_id", instanceId)    // 호스트명 또는 인스턴스 ID 추가
                    // 필요에 따라 다른 공통 태그 추가 (예: region, version 등)
                )
            )
        }
    }

    // 추가적으로 라이브러리에서 제공하는 커스텀 메트릭 바인더 등을 여기에 빈으로 등록할 수 있습니다.
    // 예를 들어, 특정 컴포넌트의 상태나 성능을 나타내는 게이지(Gauge)나 카운터(Counter) 등
    /*
    @Bean
    fun myCustomMetricsBinder(meterRegistry: MeterRegistry): MyCustomMetricsBinder {
        val binder = MyCustomMetricsBinder()
        binder.bindTo(meterRegistry)
        log.info("MyCustomMetricsBinder registered.")
        return binder
    }
    */
}
