package com.wildrew.jobstat.core.core_security.config // 또는 com.wildrew.jobstat.core.core_security.autoconfigure

import com.wildrew.jobstat.core.core_security.util.PasswordUtil
import org.slf4j.LoggerFactory // 로깅 추가
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication // 추가
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
// import org.springframework.core.annotation.Order // 필요시
// import org.springframework.boot.autoconfigure.security.SecurityProperties // Order 사용 시
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET) // 웹 애플리케이션 환경에서만
@ConditionalOnProperty(
    name = ["jobstat.core.security.admin-page.enabled"], // 프로퍼티 이름
    havingValue = "true",                             // 이 값이 true일 때 활성화
    matchIfMissing = true                             // 프로퍼티가 없으면 true로 간주 (기본 활성화!)
)
@AutoConfigureAfter(
    CoreWebSecurityAutoConfiguration::class, // 기본 웹 보안 설정 이후
    CorePasswordUtilConfig::class         // PasswordUtil 빈 필요
)
class CoreAdminPageSecurityAutoConfiguration {

    private val log = LoggerFactory.getLogger(CoreAdminPageSecurityAutoConfiguration::class.java)

    companion object {
        // SECURED_PATHS는 라이브러리 사용자가 프로퍼티로 커스터마이징 가능하게 할 수도 있습니다.
        // 여기서는 고정값으로 두겠습니다.
        private val SECURED_PATHS = arrayOf(
            "/admin/**",
            "/api/admin/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
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
            }
            .csrf { csrf -> csrf.disable() } // 최신 방식의 CSRF 비활성화
            .build()
    }

    @Bean("coreAdminUserDetailsService")
    @ConditionalOnMissingBean(name = ["coreAdminUserDetailsService"])
    fun coreAdminUserDetailsService(
        @Value("\${jobstat.core.security.admin.username:admin}") adminUsername: String,
        @Value("\${jobstat.core.security.admin.password:admin}") adminPasswordValue: String,
        passwordUtil: PasswordUtil // CorePasswordUtilConfig 등에서 제공
    ): UserDetailsService {
        log.info("Configuring CoreAdminUserDetailsService with username: {}", adminUsername)
        val user = User.withUsername(adminUsername)
            .password(passwordUtil.encode(adminPasswordValue))
            .roles("ADMIN")
            .build()
        return InMemoryUserDetailsManager(user)
    }
}