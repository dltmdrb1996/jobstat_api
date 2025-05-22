package com.wildrew.jobstat.core.core_security.config // 패키지 경로 예시

import com.fasterxml.jackson.databind.ObjectMapper
import com.wildrew.jobstat.core.core_security.filter.ScopedValueJwtTokenFilter
import com.wildrew.jobstat.core.core_security.filter.ThreadLocalJwtTokenFilter
import com.wildrew.jobstat.core.core_security.util.ScopedSecurityContextHolder
import com.wildrew.jobstat.core.core_serializer.config.CoreSerializerAutoConfiguration
import com.wildrew.jobstat.core.core_token.JwtTokenParser
import com.wildrew.jobstat.core.core_token.config.CoreTokenAutoConfiguration
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@AutoConfigureAfter(
    CoreSerializerAutoConfiguration::class,
    CoreTokenAutoConfiguration::class
)
class CoreFilterAutoConfiguration {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
    // 가상 스레드 활성화 시 ScopedValueJwtTokenFilter 빈 등록
    @Bean("jwtTokenFilter") // 빈 이름을 "jwtTokenFilter"로 통일
    @ConditionalOnMissingBean(name = ["jwtTokenFilter"]) // 동일 이름의 빈이 없을 때
    @ConditionalOnProperty(
        name = ["spring.threads.virtual.enabled"],
        havingValue = "true",
        matchIfMissing = false // 명시적으로 true일 때만
    )
    @ConditionalOnClass(ScopedSecurityContextHolder::class) // ScopedValue 관련 클래스 존재 시
    fun scopedValueJwtTokenFilter(
        jwtTokenParser: JwtTokenParser,
        requestMappingHandlerMapping: RequestMappingHandlerMapping,
        objectMapper: ObjectMapper
    ): OncePerRequestFilter { // 반환 타입을 OncePerRequestFilter 또는 상위 Filter로
        log.info("Configuring ScopedValueJwtTokenFilter")
        return ScopedValueJwtTokenFilter(
            jwtTokenParser,
            requestMappingHandlerMapping,
            objectMapper
        )
    }

    // 가상 스레드 비활성화 또는 프로퍼티 없을 시 ThreadLocalJwtTokenFilter 빈 등록
    @Bean("jwtTokenFilter") // 빈 이름을 "jwtTokenFilter"로 통일
    @ConditionalOnMissingBean(name = ["jwtTokenFilter"]) // 동일 이름의 빈이 없을 때
    @ConditionalOnProperty(
        name = ["spring.threads.virtual.enabled"],
        havingValue = "false",
        matchIfMissing = true // false이거나 프로퍼티가 없을 때 (기본값)
    )
    fun threadLocalJwtTokenFilter(
        jwtTokenParser: JwtTokenParser,
        requestMappingHandlerMapping: RequestMappingHandlerMapping,
        objectMapper: ObjectMapper
    ): OncePerRequestFilter { // 반환 타입을 OncePerRequestFilter 또는 상위 Filter로
        // ThreadLocalJwtTokenFilter의 클래스명이 JwtTokenFilter라면 아래와 같이 수정
        // return JwtTokenFilter(
        log.info("Configuring ThreadLocalJwtTokenFilter")
        return ThreadLocalJwtTokenFilter( // 클래스명 확인 필요
            jwtTokenParser,
            requestMappingHandlerMapping,
            objectMapper
        )
    }
}