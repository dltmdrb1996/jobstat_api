package com.example.jobstat.core.core_security.config

import com.example.jobstat.core.core_security.util.ScopedSecurityContextHolder // ScopedValueTheadContextUtils가 의존
import com.example.jobstat.core.core_security.util.context_util.ScopedValueTheadContextUtils
import com.example.jobstat.core.core_security.util.context_util.TheadContextUtils // 인터페이스
import com.example.jobstat.core.core_security.util.context_util.ThreadLocalTheadContextUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@AutoConfiguration
class CoreTheadContextUtilsAutoConfiguration { // 클래스 이름을 더 명확하게 변경 가능

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
    /**
     * 가상 스레드가 활성화된 경우 ScopedValue 기반 SecurityUtils를 제공합니다.
     */
    @Bean("scopedValueTheadContextUtils") // 빈 이름 명시
    @ConditionalOnMissingBean(TheadContextUtils::class) // 사용자가 직접 TheadContextUtils 빈을 등록하지 않았을 때
    @ConditionalOnProperty(
        name = ["spring.threads.virtual.enabled"],
        havingValue = "true",
        matchIfMissing = false // 명시적으로 true일 때만 동작
    )
    @ConditionalOnClass(ScopedSecurityContextHolder::class) // ScopedValue 관련 클래스가 있을 때 (JDK 21+ 등)
    fun scopedValuePrimarySecurityUtils(): TheadContextUtils {
        log.info("ScopedValueTheadContextUtils is created.")
        // ScopedValueTheadContextUtils 생성 시 필요한 의존성이 있다면 주입받도록 수정 필요
        return ScopedValueTheadContextUtils()
    }

    /**
     * 가상 스레드가 비활성화되었거나, 관련 프로퍼티가 없거나,
     * 또는 ScopedValueTheadContextUtils 조건이 맞지 않을 경우 (예: JDK 버전)
     * ThreadLocal 기반 SecurityUtils를 기본으로 제공합니다.
     */
    @Bean("threadLocalTheadContextUtils") // 빈 이름 명시
    @Primary // TheadContextUtils 타입의 빈이 여러 개 있을 경우 우선권을 가짐
    @ConditionalOnMissingBean(TheadContextUtils::class) // 사용자가 직접 TheadContextUtils 빈을 등록하지 않았을 때
    @ConditionalOnProperty(
        name = ["spring.threads.virtual.enabled"],
        havingValue = "false", // 명시적으로 false 이거나
        matchIfMissing = true  // 프로퍼티가 없을 때 (기본값) 동작
    )
    // @ConditionalOnMissingClass("com.example.jobstat.core.core_security.util.ScopedSecurityContextHolder") // ScopedValue 방식이 조건에 안 맞을 때를 대비 (선택적)
    fun threadLocalPrimarySecurityUtils(): TheadContextUtils {
        log.info("ThreadLocalTheadContextUtils is created.")
        // ThreadLocalTheadContextUtils 생성 시 필요한 의존성이 있다면 주입받도록 수정 필요
        return ThreadLocalTheadContextUtils()
    }

    // 참고: 만약 ScopedValueTheadContextUtils와 ThreadLocalTheadContextUtils가
    // 동일한 TheadContextUtils 인터페이스를 구현하고,
    // 두 @Bean 메소드가 모두 @ConditionalOnMissingBean(TheadContextUtils::class)와 @Primary를 가지고 있다면,
    // @ConditionalOnProperty 조건에 따라 둘 중 하나만 활성화되어야 합니다.
    // 스프링은 조건을 만족하는 @Primary 빈 중 하나를 선택합니다.
    // 위 설정은 virtual-threads.enabled=true 일 때 scoped,
    // virtual-threads.enabled=false 또는 없을 때 thread-local 이 되도록 의도했습니다.
}