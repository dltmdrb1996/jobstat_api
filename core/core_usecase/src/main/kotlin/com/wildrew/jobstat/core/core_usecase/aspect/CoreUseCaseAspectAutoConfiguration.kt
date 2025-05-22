package com.wildrew.jobstat.core.core_usecase.aspect

import org.aspectj.lang.Aspects
import org.slf4j.Logger
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy

@AutoConfiguration
 @ConditionalOnClass(Aspects::class, Logger::class) // AspectJ 및 로깅 라이브러리 존재 시
 @ConditionalOnProperty(
     name = ["jobstat.core.usecase.logging.enabled"], // 프로퍼티로 제어
     havingValue = "true",
     matchIfMissing = true // 기본적으로 활성화
 )
 @EnableAspectJAutoProxy // Spring AOP 활성화 (필요한 경우)
 class CoreUseCaseAspectAutoConfiguration {

     private val log = org.slf4j.LoggerFactory.getLogger(CoreUseCaseAspectAutoConfiguration::class.java)

     @Bean
     @ConditionalOnMissingBean(UseCaseLoggingAspect::class)
     fun useCaseLoggingAspect(): UseCaseLoggingAspect {
         log.info("Configuring UseCaseLoggingAspect bean.")
         return UseCaseLoggingAspect()
     }
 }