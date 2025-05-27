package com.wildrew.jobstat.auth.utils.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

@TestConfiguration
@Profile("test")
class TestMysqlConfig {
    @Bean(initMethod = "start", destroyMethod = "stop")
    fun mySqlContainer(): MySQLContainer<*> =
        MySQLContainer(DockerImageName.parse("mysql:8.0.28")).apply {
            withDatabaseName("test_db")
            withUsername("test")
            withPassword("test")
            withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_unicode_ci",
                "--lower_case_table_names=1",
            )
            withUrlParam("rewriteBatchedStatements", "true")
            withUrlParam("useSSL", "false")
            withUrlParam("allowPublicKeyRetrieval", "true")
            start() // 여기서 명시적으로 시작
        }

    @Bean
    @Primary
    fun dataSource(mySqlContainer: MySQLContainer<*>): DataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = mySqlContainer.jdbcUrl
                username = mySqlContainer.username
                password = mySqlContainer.password
                driverClassName = mySqlContainer.driverClassName
                maximumPoolSize = 10
                minimumIdle = 2
                connectionInitSql = "SET sql_mode='NO_ENGINE_SUBSTITUTION'"
            },
        )

    @Bean
    @Primary
    fun jdbcTemplate(dataSource: DataSource): JdbcTemplate = JdbcTemplate(dataSource)

    @Bean
    @Primary
    fun namedParameterJdbcTemplate(jdbcTemplate: JdbcTemplate): NamedParameterJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
}
