package com.example.jobstat.core.config

import com.example.jobstat.core.security.PasswordUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
class AdminPageConfig(
    @Value("\${admin.username}") private val adminUsername: String,
    @Value("\${admin.password}") private val adminPassword: String,
    private val passwordUtil: PasswordUtil,
) {
    @Bean
    fun adminSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/admin/**", "/api/admin/**", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**")
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests {
                it.anyRequest().authenticated()
            }.csrf { it.disable() }

        return http.build()
    }

    @Bean
    fun adminUserDetailsService(): UserDetailsService {
        val user =
            User
                .withUsername(adminUsername)
                // bcrypt 암호화 적용
                .password(passwordUtil.encode(adminPassword))
                .roles("ADMIN")
                .build()
        return InMemoryUserDetailsManager(user)
    }
}
