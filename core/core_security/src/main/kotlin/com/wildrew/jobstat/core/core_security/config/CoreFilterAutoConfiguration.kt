package com.wildrew.jobstat.core.core_security.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wildrew.jobstat.core.core_security.filter.ScopedValueGatewayHeaderAuthenticationFilter
import com.wildrew.jobstat.core.core_security.filter.ScopedValueJwtTokenFilter
import com.wildrew.jobstat.core.core_security.filter.ThreadLocalGatewayHeaderAuthenticationFilter
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
    CoreTokenAutoConfiguration::class,
)
class CoreFilterAutoConfiguration {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Bean("coreSecurityFilter")
    @ConditionalOnMissingBean(name = ["coreSecurityFilter"])
    @ConditionalOnProperty(
        name = ["spring.threads.virtual.enabled"],
        havingValue = "true",
        matchIfMissing = false,
    )
    @ConditionalOnClass(ScopedSecurityContextHolder::class)
    fun scopedValueCoreSecurityFilter(
        requestMappingHandlerMapping: RequestMappingHandlerMapping,
        objectMapper: ObjectMapper,
    ): OncePerRequestFilter {
        log.info("Configuring ScopedValueJwtTokenFilter")
        return ScopedValueGatewayHeaderAuthenticationFilter(
            requestMappingHandlerMapping,
            objectMapper,
        )
    }

    @Bean("coreSecurityFilter")
    @ConditionalOnMissingBean(name = ["coreSecurityFilter"])
    @ConditionalOnProperty(
        name = ["spring.threads.virtual.enabled"],
        havingValue = "false",
        matchIfMissing = true,
    )
    fun threadLocalCoreSecurityFilter(
        requestMappingHandlerMapping: RequestMappingHandlerMapping,
        objectMapper: ObjectMapper,
    ): OncePerRequestFilter {
        log.info("Configuring ThreadLocalJwtTokenFilter")
        return ThreadLocalGatewayHeaderAuthenticationFilter(
            requestMappingHandlerMapping,
            objectMapper,
        )
    }
}
