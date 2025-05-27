package com.wildrew.jobstat.core.core_security.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wildrew.jobstat.core.core_security.util.JwtAuthenticationEntryPoint
import com.wildrew.jobstat.core.core_serializer.config.CoreSerializerAutoConfiguration
import jakarta.servlet.Filter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.server.adapter.ForwardedHeaderTransformer

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@AutoConfigureAfter(
    CoreFilterAutoConfiguration::class,
    CoreSerializerAutoConfiguration::class,
)
class CoreWebSecurityAutoConfiguration {
    @Value("\${ddns.domain:http://localhost:8080}")
    private lateinit var ddnsDomain: String

    private val log: Logger by lazy { LoggerFactory.getLogger(javaClass) }

    @Bean
    @ConditionalOnMissingBean(AuthenticationEntryPoint::class)
    fun coreJwtAuthenticationEntryPoint(objectMapper: ObjectMapper): JwtAuthenticationEntryPoint = JwtAuthenticationEntryPoint(objectMapper)

    @Bean("coreSecurityFilterChain")
    @ConditionalOnMissingBean(name = ["coreSecurityFilterChain"])
    fun coreSecurityFilterChain(
        http: HttpSecurity,
        @Qualifier("coreSecurityFilter") jwtTokenFilter: Filter,
        jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    ): SecurityFilterChain {
        http
            .cors { it.configurationSource(coreCorsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint(jwtAuthenticationEntryPoint) }
            .authorizeHttpRequests {
                it
                    .anyRequest()
                    .permitAll()
            }.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
            .headers { headers ->
                headers
                    .xssProtection { xss -> xss.disable() }
                    .frameOptions { frameOptions -> frameOptions.deny() }
                    .contentSecurityPolicy { csp ->
                        csp.policyDirectives(
                            "default-src 'self'; " +
                                "script-src 'self' https://jobstatanalysis.com; " +
                                "style-src 'self' 'unsafe-inline' https://jobstatanalysis.com; " +
                                "img-src 'self' data: https: blob:; " +
                                "font-src 'self' data: https://cdn.jsdelivr.net; " +
                                "connect-src 'self' https://jobstatanalysis.com $ddnsDomain; " +
                                "frame-src 'none'; " +
                                "object-src 'none'; " +
                                "base-uri 'self';",
                        )
                    }
            }.setSharedObject(ForwardedHeaderTransformer::class.java, ForwardedHeaderTransformer())

        return http.build()
    }

    //    @ConditionalOnMissingBean(CorsConfigurationSource::class)
    @Bean("coreCorsConfigurationSource") // 빈 이름 명시
    fun coreCorsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins =
            listOf(
                ddnsDomain,
                "https://www.jobstatanalysis.com",
                "https://jobstatanalysis.com",
                "jobstatanalysis.com",
                "https://spring-app:8081",
                "http://spring-app:8081",
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
