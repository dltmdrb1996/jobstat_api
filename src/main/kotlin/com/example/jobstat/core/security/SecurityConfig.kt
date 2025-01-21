package com.example.jobstat.core.security

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.server.adapter.ForwardedHeaderTransformer

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtTokenFilter: JwtTokenFilter,
    private val userDetailsService: UserDetailsService,
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, authException ->
                    val exception = AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE)
                    response.status = exception.httpStatus.value()
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.characterEncoding = "UTF-8"
                    val errorResponse =
                        mapOf(
                            "status" to exception.httpStatus.value(),
                            "error" to exception.httpStatus.reasonPhrase,
                            "message" to (authException.message ?: "Authentication failed"),
                        )
                    objectMapper.writeValue(response.writer, errorResponse)
                }
            }.authorizeHttpRequests {
                it
                    .requestMatchers(*JwtTokenFilter.PERMIT_URLS)
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
            .headers { headers ->
                headers
                    .xssProtection { xss -> xss.disable() }
                    .frameOptions { frameOptions -> frameOptions.deny() }
                    .contentSecurityPolicy { csp ->
                        csp.policyDirectives(
                            "default-src 'self'; " +
                                "script-src 'self' https://scripts.goatstat.com; " +
                                "style-src 'self' https://styles.goatstat.com; " +
                                "img-src 'self' https://images.goatstat.com data:; " +
                                "font-src 'self' https://fonts.goatstat.com; " +
                                "connect-src 'self' https://api.goatstat.com; " +
                                "frame-src 'none'; " +
                                "object-src 'none'; " +
                                "base-uri 'self'; " +
                                "form-action 'self'; " +
                                "upgrade-insecure-requests;",
                        )
                    }
            }.setSharedObject(ForwardedHeaderTransformer::class.java, ForwardedHeaderTransformer())

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins =
            listOf(
                "https://www.goatstat.com",
                "https://goatstat.com",
                "http://localhost:8081",
                "http://localhost:3000",
                "http://localhost:8080",
            )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type")
        configuration.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
