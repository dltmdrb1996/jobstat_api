package com.wildrew.jobstat.core.core_security.config

import com.wildrew.jobstat.core.core_security.util.PasswordUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication // 추가
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
    name = ["jobstat.core.security.admin-page.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@AutoConfigureAfter(
    CoreWebSecurityAutoConfiguration::class,
    CorePasswordUtilConfig::class,
)
class CoreAdminPageSecurityAutoConfiguration {
    private val log = LoggerFactory.getLogger(CoreAdminPageSecurityAutoConfiguration::class.java)

    companion object {
        private val SECURED_PATHS =
            arrayOf(
                "/admin/**",
                "/api/admin/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-resources/**",
            )
    }

    @Bean("coreAdminSecurityFilterChain")
    @ConditionalOnMissingBean(name = ["coreAdminSecurityFilterChain"])
    @Order(SecurityProperties.BASIC_AUTH_ORDER - 10)
    fun coreAdminSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        log.info("Configuring CoreAdminPageSecurityFilterChain for paths: {}", SECURED_PATHS.joinToString())
        return http
            .securityMatcher(*SECURED_PATHS)
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests { authorize ->
                authorize.anyRequest().hasRole("ADMIN")
            }.csrf { csrf -> csrf.disable() }
            .build()
    }

    @Bean("coreAdminUserDetailsService")
    @ConditionalOnMissingBean(name = ["coreAdminUserDetailsService"])
    fun coreAdminUserDetailsService(
        @Value("\${jobstat.core.security.admin.username:admin}") adminUsername: String,
        @Value("\${jobstat.core.security.admin.password:admin}") adminPasswordValue: String,
        passwordUtil: PasswordUtil,
    ): UserDetailsService {
        log.info("Configuring CoreAdminUserDetailsService with username: {}", adminUsername)
        val user =
            User
                .withUsername(adminUsername)
                .password(passwordUtil.encode(adminPasswordValue))
                .roles("ADMIN")
                .build()
        return InMemoryUserDetailsManager(user)
    }
}
