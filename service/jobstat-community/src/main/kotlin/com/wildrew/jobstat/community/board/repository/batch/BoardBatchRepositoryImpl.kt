package com.wildrew.jobstat.community.board.repository.batch

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.EntityMapper
import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.SqlGenerator
import com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.repository.JdbcBatchRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class BoardBatchRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    @Qualifier("coreSnowflakeSqlGenerator")
    private val sqlGenerator: SqlGenerator,
    private val entityMapper: EntityMapper<Board, Long>,
) : JdbcBatchRepository<Board>(
        jdbcTemplate,
        namedParameterJdbcTemplate,
        sqlGenerator,
        entityMapper,
    )
