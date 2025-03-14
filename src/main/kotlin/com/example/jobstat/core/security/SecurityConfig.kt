package com.example.jobstat.core.security

import com.example.jobstat.core.utils.JwtAuthenticationEntryPoint
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.server.adapter.ForwardedHeaderTransformer

@Configuration
@EnableWebSecurity
class NoSecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .csrf { it.disable() } // CSRF 보호도 비활성화
        return http.build()
    }
}

//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity(prePostEnabled = true)
//class SecurityConfig(
//    private val jwtTokenFilter: JwtTokenFilter,
//    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
//) {
//    @Value("\${ddns.domain}")
//    private lateinit var ddnsDomain: String
//    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
//
//    @Bean
//    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
//        http
//            .cors { it.configurationSource(corsConfigurationSource()) }
//            .csrf { it.disable() }
//            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
//            .exceptionHandling { it.authenticationEntryPoint(jwtAuthenticationEntryPoint) }
//            .authorizeHttpRequests {
//                it
//                    .anyRequest()
//                    .permitAll() // 실제 인증은 JwtTokenFilter에서 처리
//            }.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
//            .headers { headers ->
//                headers
//                    .xssProtection { xss -> xss.disable() }
//                    .frameOptions { frameOptions -> frameOptions.deny() }
//                    .contentSecurityPolicy { csp ->
//                        csp.policyDirectives(
//                            "default-src 'self'; " +
//                                "script-src 'self' https://jobstatanalysis.com; " +
//                                "style-src 'self' 'unsafe-inline' https://jobstatanalysis.com; " +
//                                "img-src 'self' data: https: blob:; " +
//                                "font-src 'self' data: https://cdn.jsdelivr.net; " +
//                                "connect-src 'self' https://jobstatanalysis.com $ddnsDomain; " +
//                                "frame-src 'none'; " +
//                                "object-src 'none'; " +
//                                "base-uri 'self';",
//                        )
//                    }
//            }.setSharedObject(ForwardedHeaderTransformer::class.java, ForwardedHeaderTransformer())
//
//        return http.build()
//    }
//
//    @Bean
//    fun corsConfigurationSource(): CorsConfigurationSource {
//        val configuration = CorsConfiguration()
//        configuration.allowedOrigins =
//            listOf(
//                "$ddnsDomain",
//                "https://www.jobstatanalysis.com",
//                "https://jobstatanalysis.com",
//                "jobstatanalysis.com",
//                "https://spring-app:8081",
//                "http://spring-app:8081",
//                "http://localhost:8081",
//                "http://localhost:3000",
//                "http://localhost:8080",
//            )
//        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
//        configuration.allowedHeaders = listOf("Authorization", "Content-Type")
//        configuration.allowCredentials = true
//
//        val source = UrlBasedCorsConfigurationSource()
//        source.registerCorsConfiguration("/**", configuration)
//        return source
//    }
//}
