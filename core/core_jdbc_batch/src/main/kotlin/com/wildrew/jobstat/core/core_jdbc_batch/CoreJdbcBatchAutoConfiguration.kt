package com.wildrew.jobstat.core.core_jdbc_batch

import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.SqlGenerator
import com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.sql.DefaultSqlGenerator
import com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.sql.SnowflakeIdAwareSqlGenerator
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate

@AutoConfiguration
@ConditionalOnClass(JdbcTemplate::class, SqlGenerator::class)
@ConditionalOnProperty(
    name = ["jobstat.core.jdbc-batch.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class CoreJdbcBatchAutoConfiguration {
    private val logger = LoggerFactory.getLogger(CoreJdbcBatchAutoConfiguration::class.java)

    @Bean("coreDefaultSqlGenerator")
    @Primary
    fun coreDefaultSqlGenerator(): SqlGenerator {
        logger.info("기본 ID 생성 방식(Auto-increment)을 위한 'coreDefaultSqlGenerator' (DefaultSqlGenerator) 빈을 생성합니다.")
        return DefaultSqlGenerator()
    }

    @Bean("coreSnowflakeSqlGenerator")
    fun coreSnowflakeSqlGenerator(): SqlGenerator {
        logger.info("Snowflake ID 방식 삽입을 위한 'coreSnowflakeSqlGenerator' (SnowflakeIdAwareSqlGenerator) 빈을 생성합니다.")
        return SnowflakeIdAwareSqlGenerator()
    }
}
