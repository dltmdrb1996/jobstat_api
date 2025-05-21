package com.example.jobstat.core.core_security.config

import com.example.jobstat.core.core_security.aspect.RateLimitAspect
import org.aspectj.lang.Aspects // @ConditionalOnClass 확인용
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnClass(Aspects::class)
@ConditionalOnProperty(name = ["jobstat.core.security.rate-limit.enabled"], havingValue = "true", matchIfMissing = true)
class CoreAspectAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RateLimitAspect::class)
    fun rateLimitAspect(): RateLimitAspect {
        return RateLimitAspect()
    }
}