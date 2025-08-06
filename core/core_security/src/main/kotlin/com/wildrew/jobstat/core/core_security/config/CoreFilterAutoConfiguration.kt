package com.wildrew.jobstat.core.core_security.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wildrew.jobstat.core.core_security.filter.GatewayHeaderAuthenticationFilter
import com.wildrew.jobstat.core.core_serializer.config.CoreSerializerAutoConfiguration
import com.wildrew.jobstat.core.core_token.config.CoreTokenAutoConfiguration
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.OncePerRequestFilter

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@AutoConfigureAfter(
    CoreSerializerAutoConfiguration::class,
    CoreTokenAutoConfiguration::class,
)
@Configuration
class CoreFilterAutoConfiguration(
    private val objectMapper: ObjectMapper,
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Bean("coreSecurityFilter")
    fun coreSecurityFilter(): OncePerRequestFilter = GatewayHeaderAuthenticationFilter(objectMapper)

//    @Bean("coreSecurityFilter")
//    @ConditionalOnProperty(
//        name = ["spring.threads.virtual.enabled"],
//        havingValue = "true",
//        matchIfMissing = false,
//    )
//    fun scopedValueGatewayHeaderAuthenticationFilter(): ScopedValueGatewayHeaderAuthenticationFilter {
//        return ScopedValueGatewayHeaderAuthenticationFilter(objectMapper)
//    }
//
//    @Bean("coreSecurityFilter")
//    @ConditionalOnProperty(
//        name = ["spring.threads.virtual.enabled"],
//        havingValue = "false",
//        matchIfMissing = true,
//    )
//    fun threadLocalGatewayHeaderAuthenticationFilter(): ThreadLocalGatewayHeaderAuthenticationFilter {
//        return ThreadLocalGatewayHeaderAuthenticationFilter(objectMapper)
//    }
}
