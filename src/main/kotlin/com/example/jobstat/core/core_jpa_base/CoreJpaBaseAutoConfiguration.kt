package com.example.jobstat.core.core_jpa_base // 패키지 변경 권장

import jakarta.persistence.EntityManager // JPA API 존재 확인용
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration // @EnableJpaAuditing은 @Configuration과 함께 사용
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@AutoConfiguration
@ConditionalOnClass(EntityManager::class)
class CoreJpaBaseAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @EnableJpaAuditing
    @ConditionalOnProperty(name = ["jobstat.core.jpa.auditing.enabled"], havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(
        name = ["jpaAuditingHandler", "auditingHandler"], // Spring Data JPA의 AuditingHandler 빈 이름들
        type = ["org.springframework.data.auditing.AuditingHandler"] // 타입으로도 명시 가능
    )
    class JpaAuditingConfiguration
}