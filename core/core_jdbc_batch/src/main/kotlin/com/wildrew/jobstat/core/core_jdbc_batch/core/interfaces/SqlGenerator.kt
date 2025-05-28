package com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces

import com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.update.ColumnUpdate

interface SqlGenerator {
    fun generateInsertSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String

    fun generateUpdateSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String

    fun generateUpsertSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String

    fun generateSelectSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String

    fun generateDeleteSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String

    fun generateSelectAllSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String

    fun generateBulkValueUpdateSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
        columnUpdate: ColumnUpdate,
    ): String

    fun generateColumnSpecificBatchUpdateSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
        columnName: String,
    ): String

    fun generateIgnoreInsertSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String

    fun generateCursorBasedSelectSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
        dateColumn: String,
    ): String
}
