package com.wildrew.jobstat.core.core_jpa_base

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
        name = ["jpaAuditingHandler", "auditingHandler"],
        type = ["org.springframework.data.auditing.AuditingHandler"],
    )
    class JpaAuditingConfiguration
}
