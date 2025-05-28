package com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.repository

import com.wildrew.jobstat.core.core_global.model.BaseEntity
import com.wildrew.jobstat.core.core_jdbc_batch.core.exception.BatchProcessingException
import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.EntityMapper
import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.Operation
import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.SqlGenerator
import com.wildrew.jobstat.core.core_jdbc_batch.core.model.BatchOptions
import com.wildrew.jobstat.core.core_jdbc_batch.core.model.BatchResult
import com.wildrew.jobstat.core.core_jdbc_batch.core.model.SelectPage
import com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.update.ColumnType
import com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.update.ColumnUpdate
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.ConnectionCallback
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.annotation.Transactional
import java.sql.*
import java.time.LocalDateTime
import kotlin.math.min

abstract class JdbcBatchRepository<T : BaseEntity>(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val sqlGenerator: SqlGenerator,
    private val entityMapper: EntityMapper<T, Long>,
) : BatchRepository<T, Long> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private inner class BatchContext(
        val operation: String,
        val chunkSize: Int,
        val tableName: String = entityMapper.tableName,
        val sql: String? = null,
        val additionalInfo: Map<String, Any?> = emptyMap(),
    )

    private fun logAndHandleError(
        context: BatchContext,
        error: Exception,
        options: BatchOptions,
        errorCallback: () -> Unit = {},
    ) {
        val errorMessage = buildErrorMessage(context, error)

        if (!options.continueOnError) {
            logger.error(errorMessage, error)
            throw BatchProcessingException(errorMessage, error)
        }
        logger.warn(errorMessage, error)
        errorCallback()
    }

    private fun buildErrorMessage(
        context: BatchContext,
        error: Exception,
    ): String =
        """
            Batch operation failed:
            Operation: ${context.operation}
            Table: ${context.tableName}
            Chunk size: ${context.chunkSize}
            SQL: ${context.sql ?: "N/A"}
            Error type: ${error.javaClass.name}
            Error message: ${error.message}
            ${
            context.additionalInfo.takeIf { it.isNotEmpty() }?.let { info ->
                "Additional Info:\n" + info.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }
            } ?: ""
        }
        """.trimIndent()

    @Transactional
    override fun batchInsert(
        entities: List<T>,
        options: BatchOptions,
    ): BatchResult<T> =
        when {
            entities.isEmpty() -> BatchResult(emptyList())
            else -> executeBatch(entities, Operation.INSERT, options)
        }

    @Transactional
    override fun batchInsertWithIgnore(
        entities: List<T>,
        options: BatchOptions,
    ): BatchResult<T> =
        when {
            entities.isEmpty() -> BatchResult(emptyList())
            else -> executeIgnoreBatch(entities, options)
        }

    override fun batchSelectByIds(ids: List<Long>): List<T> {
        if (ids.isEmpty()) return emptyList()

        val batchSize = getBatchSize(ids.size)

        return ids.chunked(batchSize).flatMap { batch ->
            val sql = "SELECT * FROM ${entityMapper.tableName} WHERE ${entityMapper.idColumn} IN (:ids)"
            val params = MapSqlParameterSource().addValue("ids", batch)

            namedParameterJdbcTemplate.query(sql, params) { rs, _ ->
                entityMapper.fromRow(rs)
            }
        }
    }

    @Transactional
    override fun batchUpdate(
        entities: List<T>,
        options: BatchOptions,
    ): BatchResult<T> =
        if (entities.isEmpty()) {
            BatchResult(emptyList())
        } else {
            executeBatch(entities, Operation.UPDATE, options)
        }

    @Transactional
    override fun batchUpsert(
        entities: List<T>,
        options: BatchOptions,
    ): BatchResult<T> =
        if (entities.isEmpty()) {
            BatchResult(emptyList())
        } else {
            executeBatch(entities, Operation.UPSERT, options)
        }

    @Transactional
    override fun batchDelete(
        ids: List<Long>,
        options: BatchOptions,
    ): Int {
        if (ids.isEmpty()) return 0

        val sql = sqlGenerator.generateDeleteSql(entityMapper.tableName, entityMapper)
        var totalDeleted = 0

        ids.chunked(getBatchSize(ids.size)).forEach { chunk ->
            val params = MapSqlParameterSource().addValue("ids", chunk)

            try {
                val deleted = namedParameterJdbcTemplate.update(sql, params)
                totalDeleted += deleted
            } catch (e: Exception) {
                logAndHandleError(
                    BatchContext(
                        operation = "DELETE",
                        chunkSize = chunk.size,
                        sql = sql,
                        additionalInfo =
                            mapOf(
                                "First few IDs" to chunk.take(5),
                            ),
                    ),
                    e,
                    options,
                )
            }
        }

        return totalDeleted
    }

    @Transactional
    override fun batchInsertWithReturning(
        entities: List<T>,
        options: BatchOptions,
    ): BatchResult<T> =
        when {
            entities.isEmpty() -> BatchResult(emptyList())
            else -> executeBatchWithReturning(entities, Operation.INSERT, options)
        }

    @Transactional
    override fun batchUpdateWithReturning(
        entities: List<T>,
        options: BatchOptions,
    ): BatchResult<T> =
        when {
            entities.isEmpty() -> BatchResult(emptyList())
            else -> executeBatchWithReturning(entities, Operation.UPDATE, options)
        }

    @Transactional
    override fun batchUpsertWithReturning(
        entities: List<T>,
        options: BatchOptions,
    ): BatchResult<T> =
        when {
            entities.isEmpty() -> BatchResult(emptyList())
            else -> executeBatchWithReturning(entities, Operation.UPSERT, options)
        }

    @Transactional
    override fun bulkValueUpdate(
        ids: List<Long>,
        columnUpdate: ColumnUpdate,
        options: BatchOptions,
    ): Int {
        if (ids.isEmpty()) return 0

        var totalUpdated = 0

        ids.chunked(getBatchSize(ids.size)).forEach { chunk ->
            val sql =
                sqlGenerator.generateBulkValueUpdateSql(
                    entityMapper.tableName,
                    entityMapper,
                    columnUpdate,
                )

            try {
                logger.debug("Executing bulk value update with SQL: $sql")

                val params =
                    MapSqlParameterSource()
                        .addValue("value", columnUpdate.value)
                        .addValue("ids", chunk)

                val updated = namedParameterJdbcTemplate.update(sql, params)
                totalUpdated += updated

                logger.debug("Successfully updated chunk. Updated rows: $updated")
            } catch (e: Exception) {
                logAndHandleError(
                    BatchContext(
                        operation = "BULK_VALUE_UPDATE",
                        chunkSize = chunk.size,
                        sql = sql,
                        additionalInfo =
                            mapOf(
                                "Column" to columnUpdate.columnName,
                                "Value" to columnUpdate.value,
                                "Type" to columnUpdate.type,
                                "First few IDs" to chunk.take(5),
                            ),
                    ),
                    e,
                    options,
                )
            }
        }

        return totalUpdated
    }

    @Transactional(readOnly = true)
    override fun batchSelect(
        criteria: SelectPage<Long>,
        options: BatchOptions,
    ): List<T> {
        val sql = sqlGenerator.generateSelectSql(entityMapper.tableName, entityMapper)
        val params =
            MapSqlParameterSource()
                .addValue("lastId", criteria.lastId ?: 0)
                .addValue(
                    "limit",
                    min(
                        criteria.limit ?: options.batchSize,
                        MAX_BATCH_SIZE,
                    ),
                )

        return try {
            namedParameterJdbcTemplate.query(sql, params) { rs, _ ->
                entityMapper.fromRow(rs)
            }
        } catch (e: Exception) {
            logAndHandleError(
                BatchContext(
                    operation = "SELECT",
                    chunkSize = criteria.limit ?: options.batchSize,
                    sql = sql,
                    additionalInfo =
                        mapOf(
                            "Last ID" to criteria.lastId,
                            "Limit" to criteria.limit,
                        ),
                ),
                e,
                options,
            )
            emptyList()
        }
    }

    @Transactional
    override fun findAll(): List<T> {
        val sql = sqlGenerator.generateSelectAllSql(entityMapper.tableName, entityMapper)

        return try {
            jdbcTemplate.query(sql) { rs, _ ->
                entityMapper.fromRow(rs)
            }
        } catch (e: Exception) {
            logAndHandleError(
                BatchContext(
                    operation = "FIND_ALL",
                    chunkSize = 0,
                    sql = sql,
                ),
                e,
                BatchOptions(),
            )
            emptyList()
        }
    }

    @Transactional(readOnly = true)
    override fun findByDateRange(
        dateColumn: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        selectPage: SelectPage<Long>,
    ): List<T> {
        logger.info("Finding entities by date range: $startDate ~ $endDate")
        val sql =
            sqlGenerator.generateCursorBasedSelectSql(
                tableName = entityMapper.tableName,
                entityMapper = entityMapper,
                dateColumn = dateColumn,
            )

        val params =
            MapSqlParameterSource()
                .addValue("startDate", startDate)
                .addValue("endDate", endDate)
                .addValue("limit", selectPage.limit ?: 100)
                .addValue("lastId", selectPage.lastId ?: 0) // 항상 lastId를 추가 (null 가능)

        return try {
            namedParameterJdbcTemplate.query(sql, params) { rs, _ ->
                entityMapper.fromRow(rs)
            }
        } catch (e: Exception) {
            logAndHandleError(
                BatchContext(
                    operation = "DATE_RANGE_SELECT_CURSOR",
                    chunkSize = selectPage.limit ?: 100, // default limit if not provided
                    sql = sql,
                    additionalInfo =
                        mapOf(
                            "Date Column" to dateColumn,
                            "Start Date" to startDate,
                            "End Date" to endDate,
                            "Last ID" to selectPage.lastId,
                        ),
                ),
                e,
                BatchOptions(),
            )
            emptyList()
        }
    }

    @Transactional
    override fun batchColumnUpdate(
        updates: List<Pair<Long, ColumnUpdate>>,
        options: BatchOptions,
    ): Int {
        if (updates.isEmpty()) return 0

        var totalUpdated = 0
        val updatesByColumn = updates.groupBy { it.second.columnName }

        updatesByColumn.forEach { (columnName, columnUpdates) ->
            val sql =
                sqlGenerator.generateColumnSpecificBatchUpdateSql(
                    entityMapper.tableName,
                    entityMapper,
                    columnName,
                )

            try {
                jdbcTemplate.batchUpdate(sql, columnUpdates, getBatchSize(columnUpdates.size)) { ps, update ->
                    setValueByType(ps, 1, update.second.value, update.second.type)
                    ps.setObject(2, update.first)
                }
                totalUpdated += columnUpdates.size
            } catch (e: Exception) {
                logAndHandleError(
                    BatchContext(
                        operation = "COLUMN_UPDATE",
                        chunkSize = columnUpdates.size,
                        sql = sql,
                        additionalInfo =
                            mapOf(
                                "Column" to columnName,
                                "First few updates" to
                                    columnUpdates.take(5).map {
                                        "ID: ${it.first}, Value: ${it.second.value}"
                                    },
                            ),
                    ),
                    e,
                    options,
                )
            }
        }

        return totalUpdated
    }

    private fun executeBatch(
        entities: List<T>,
        operation: Operation,
        options: BatchOptions,
    ): BatchResult<T> {
        val successful = mutableListOf<T>()
        val failed = mutableListOf<Pair<T, Throwable>>()

        val sql =
            when (operation) {
                Operation.INSERT -> sqlGenerator.generateInsertSql(entityMapper.tableName, entityMapper)
                Operation.UPDATE -> sqlGenerator.generateUpdateSql(entityMapper.tableName, entityMapper)
                Operation.UPSERT -> sqlGenerator.generateUpsertSql(entityMapper.tableName, entityMapper)
            }

        entities.chunked(getBatchSize(entities.size)).forEach { chunk ->
            try {
                jdbcTemplate.batchUpdate(
                    sql,
                    object : BatchPreparedStatementSetter {
                        override fun setValues(
                            ps: PreparedStatement,
                            i: Int,
                        ) {
                            entityMapper.setValues(ps, chunk[i], operation)
                        }

                        override fun getBatchSize(): Int = chunk.size
                    },
                )
                successful.addAll(chunk)
            } catch (e: Exception) {
                logAndHandleError(
                    BatchContext(
                        operation = operation.name,
                        chunkSize = chunk.size,
                        sql = sql,
                        additionalInfo =
                            mapOf(
                                "First few entities" to chunk.take(5).map { it.id },
                            ),
                    ),
                    e,
                    options,
                ) { failed.addAll(chunk.map { it to e }) }
            }
        }

        return BatchResult(successful, failed)
    }

    private fun executeBatchWithReturning(
        entities: List<T>,
        operation: Operation,
        options: BatchOptions,
    ): BatchResult<T> {
        val successful = mutableListOf<T>()
        val failed = mutableListOf<Pair<T, Throwable>>()
        val generatedIds = mutableListOf<Long>()

        val sql =
            when (operation) {
                Operation.INSERT -> sqlGenerator.generateInsertSql(entityMapper.tableName, entityMapper)
                Operation.UPDATE -> sqlGenerator.generateUpdateSql(entityMapper.tableName, entityMapper)
                Operation.UPSERT -> sqlGenerator.generateUpsertSql(entityMapper.tableName, entityMapper)
            }

        entities.chunked(getBatchSize(entities.size)).forEach { chunk ->
            try {
                when (operation) {
                    Operation.INSERT -> {
                        // Insert의 경우 생성된 ID 수집
                        jdbcTemplate.execute(
                            ConnectionCallback { connection ->
                                connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { ps ->
                                    chunk.forEach { entity ->
                                        entityMapper.setValues(ps, entity, operation)
                                        ps.addBatch()
                                    }
                                    ps.executeBatch()

                                    // 생성된 ID 수집
                                    val rs = ps.generatedKeys
                                    while (rs.next()) {
                                        generatedIds.add(entityMapper.extractId(rs))
                                    }
                                }
                            },
                        )
                    }

                    else -> {
                        jdbcTemplate.batchUpdate(
                            sql,
                            object : BatchPreparedStatementSetter {
                                override fun setValues(
                                    ps: PreparedStatement,
                                    i: Int,
                                ) {
                                    entityMapper.setValues(ps, chunk[i], operation)
                                }

                                override fun getBatchSize(): Int = chunk.size
                            },
                        )
                        generatedIds.addAll(chunk.mapNotNull { it.id })
                    }
                }

                // 생성/수정된 엔티티 조회
                if (generatedIds.isNotEmpty()) {
                    val selectSql =
                        """
                        SELECT * FROM ${entityMapper.tableName} 
                        WHERE ${entityMapper.idColumn} IN (:ids)
                        """.trimIndent()

                    val params =
                        MapSqlParameterSource()
                            .addValue("ids", generatedIds)

                    val results =
                        namedParameterJdbcTemplate.query(selectSql, params) { rs, _ ->
                            entityMapper.fromRow(rs)
                        }
                    successful.addAll(results)
                    generatedIds.clear()
                }
            } catch (e: Exception) {
                logAndHandleError(
                    BatchContext(
                        operation = "${operation.name}_WITH_RETURN",
                        chunkSize = chunk.size,
                        sql = sql,
                        additionalInfo =
                            mapOf(
                                "First few entities" to chunk.take(5).map { it.id },
                            ),
                    ),
                    e,
                    options,
                ) { failed.addAll(chunk.map { it to e }) }
            }
        }

        return BatchResult(successful, failed)
    }

    private fun executeIgnoreBatch(
        entities: List<T>,
        options: BatchOptions,
    ): BatchResult<T> {
        val successful = mutableListOf<T>()
        val failed = mutableListOf<Pair<T, Throwable>>()

        val sql = sqlGenerator.generateIgnoreInsertSql(entityMapper.tableName, entityMapper)

        entities.chunked(getBatchSize(entities.size)).forEach { chunk ->
            try {
                jdbcTemplate.batchUpdate(
                    sql,
                    object : BatchPreparedStatementSetter {
                        override fun setValues(
                            ps: PreparedStatement,
                            i: Int,
                        ) {
                            entityMapper.setValues(ps, chunk[i], Operation.INSERT)
                        }

                        override fun getBatchSize(): Int = chunk.size
                    },
                )
                successful.addAll(chunk)
            } catch (e: Exception) {
                logAndHandleError(
                    BatchContext(
                        operation = "INSERT_IGNORE",
                        chunkSize = chunk.size,
                        sql = sql,
                        additionalInfo =
                            mapOf(
                                "First few entities" to chunk.take(5).map { it.id },
                            ),
                    ),
                    e,
                    options,
                ) { failed.addAll(chunk.map { it to e }) }
            }
        }

        return BatchResult(successful, failed)
    }

    private fun getBatchSize(totalSize: Int): Int =
        when {
            totalSize > 100000 -> 2000 // 대용량 처리 최적화
            totalSize > 10000 -> 2000
            totalSize > 1000 -> 1000
            else -> 500
        }

    private fun setValueByType(
        ps: PreparedStatement,
        index: Int,
        value: Any?,
        type: ColumnType,
    ) {
        try {
            when {
                value == null -> ps.setNull(index, getSqlType(type))
                else ->
                    when (type) {
                        ColumnType.STRING -> ps.setString(index, value as String)
                        ColumnType.LONG -> ps.setLong(index, value as Long)
                        ColumnType.INT -> ps.setInt(index, value as Int)
                        ColumnType.BOOLEAN -> ps.setBoolean(index, value as Boolean)
                        ColumnType.LOCAL_DATE_TIME -> ps.setTimestamp(index, Timestamp.valueOf(value as LocalDateTime))
                        ColumnType.ENUM -> ps.setString(index, (value as Enum<*>).name)
                    }
            }
        } catch (e: Exception) {
            val errorMessage =
                """
                Failed to set parameter value:
                Index: $index
                Value: $value
                Type: $type
                Error: ${e.message}
                """.trimIndent()
            logger.error(errorMessage, e)
            throw BatchProcessingException(errorMessage, e)
        }
    }

    private fun getSqlType(type: ColumnType): Int =
        when (type) {
            ColumnType.STRING -> Types.VARCHAR
            ColumnType.LONG -> Types.BIGINT
            ColumnType.INT -> Types.INTEGER
            ColumnType.BOOLEAN -> Types.BOOLEAN
            ColumnType.LOCAL_DATE_TIME -> Types.TIMESTAMP
            ColumnType.ENUM -> Types.VARCHAR
        }

    companion object {
        const val MAX_BATCH_SIZE = 10000

        private val DEFAULT_BATCH_SIZES = listOf(500, 1000, 10000)
    }
}
