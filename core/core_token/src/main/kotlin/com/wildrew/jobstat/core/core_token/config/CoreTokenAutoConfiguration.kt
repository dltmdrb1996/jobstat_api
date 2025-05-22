package com.wildrew.jobstat.core.core_token.config

import com.wildrew.jobstat.core.core_token.JwtTokenGenerator
import com.wildrew.jobstat.core.core_token.JwtTokenParser
import io.jsonwebtoken.Jwts // @ConditionalOnClass 확인용
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnClass(Jwts::class)
@ConditionalOnProperty(name = ["jobstat.core.token.jwt.enabled"], havingValue = "true", matchIfMissing = true) // JWT 기능 활성화 프로퍼티
class CoreTokenAutoConfiguration {

    private val log = LoggerFactory.getLogger(CoreTokenAutoConfiguration::class.java)

    @Value("\${jobstat.core.token.jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${jobstat.core.token.jwt.access-token-expiration-seconds:3600}")
    private var accessTokenExpirationSeconds: Int = 3600

    @Value("\${jobstat.core.token.jwt.refresh-token-expiration-seconds:86400}")
    private var refreshTokenExpirationSeconds: Int = 86400

    @Bean
    @ConditionalOnMissingBean(JwtTokenParser::class)
    fun coreJwtTokenParser(): JwtTokenParser {
        return JwtTokenParser(jwtSecret)
    }

    @Bean
    @ConditionalOnMissingBean(JwtTokenGenerator::class)
    fun coreJwtTokenGenerator(): JwtTokenGenerator {
        return JwtTokenGenerator(
            jwtSecret,
            accessTokenExpirationSeconds,
            refreshTokenExpirationSeconds
        )
    }
}