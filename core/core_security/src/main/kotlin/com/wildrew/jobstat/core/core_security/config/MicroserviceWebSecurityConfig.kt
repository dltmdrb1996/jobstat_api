package com.wildrew.jobstat.core.core_security.config

import com.wildrew.jobstat.core.core_serializer.config.CoreSerializerAutoConfiguration
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.OncePerRequestFilter

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@AutoConfigureAfter(
    CoreFilterAutoConfiguration::class,
    CoreSerializerAutoConfiguration::class,
)
class MicroserviceWebSecurityConfig(
    private val environment: Environment,
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        coreSecurityFilter: OncePerRequestFilter,
    ): SecurityFilterChain {
        http
            .cors { it.disable() }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(
                coreSecurityFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            ).authorizeHttpRequests { it.anyRequest().permitAll() }

//        if (environment.acceptsProfiles(Profiles.of("!prod"))) {
//            log.info("Applying local CORS configuration for non-prod profile.")
//            http.cors { it.configurationSource(createLocalCorsConfigurationSource()) }
//        }

        log.info("SecurityFilterChain configuration complete.")

        return http.build()
    }

    private fun createLocalCorsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf("http://localhost:*", "http://127.0.0.1:*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)

        return source
    }
}
