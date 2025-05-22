package com.wildrew.jobstat.core.core_security.config // 패키지 경로 예시

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
import org.springframework.context.annotation.Primary
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
@EnableWebSecurity // Spring Security 활성화
@EnableMethodSecurity(prePostEnabled = true) // 메소드 수준 보안 활성화
@AutoConfigureAfter(
    CoreFilterAutoConfiguration::class,
    CoreSerializerAutoConfiguration::class
)
class CoreWebSecurityAutoConfiguration {

    @Value("\${ddns.domain:http://localhost:8080}") // 기본값 설정
    private lateinit var ddnsDomain: String

    private val log: Logger by lazy { LoggerFactory.getLogger(javaClass) }

    @Bean
    @ConditionalOnMissingBean(AuthenticationEntryPoint::class)
    fun coreJwtAuthenticationEntryPoint(objectMapper: ObjectMapper): JwtAuthenticationEntryPoint {
        return JwtAuthenticationEntryPoint(objectMapper)
    }

    @Bean("coreSecurityFilterChain") // 빈 이름 명시
    @ConditionalOnMissingBean(name = ["coreSecurityFilterChain"])
    fun coreSecurityFilterChain(
        http: HttpSecurity,
        @Qualifier("jwtTokenFilter") jwtTokenFilter: Filter,
        jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint
    ): SecurityFilterChain {
        log.info("Configuring CoreWebSecurityAutoConfiguration with jwtTokenFilter: {}", jwtTokenFilter.javaClass.simpleName)
        http
            .cors { it.configurationSource(coreCorsConfigurationSource()) } // 내부 빈 호출
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint(jwtAuthenticationEntryPoint) }
            .authorizeHttpRequests {
                it
                    .anyRequest()
                    .permitAll() // 실제 인증은 JwtTokenFilter에서 처리
            }
            .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
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
                                    "connect-src 'self' https://jobstatanalysis.com $ddnsDomain; " + // ddnsDomain 사용
                                    "frame-src 'none'; " +
                                    "object-src 'none'; " +
                                    "base-uri 'self';"
                        )
                    }
            }
            .setSharedObject(ForwardedHeaderTransformer::class.java, ForwardedHeaderTransformer())

        return http.build()
    }

    @Bean("coreCorsConfigurationSource") // 빈 이름 명시
//    @ConditionalOnMissingBean(CorsConfigurationSource::class)
    fun coreCorsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins =
            listOf(
                ddnsDomain, // 프로퍼티 값 사용
                "https://www.jobstatanalysis.com",
                "https://jobstatanalysis.com",
                "jobstatanalysis.com",
                "https://spring-app:8081",
                "http://spring-app:8081",
                "http://localhost:8081",
                "http://localhost:3000",
                "http://localhost:8080"
            )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}