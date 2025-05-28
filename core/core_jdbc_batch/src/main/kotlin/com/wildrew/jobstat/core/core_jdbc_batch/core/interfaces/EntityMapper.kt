package com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces

import java.sql.PreparedStatement
import java.sql.ResultSet

interface EntityMapper<T, ID> {
    val tableName: String
    val columns: List<String>
    val idColumn: String
    val systemColumns: Set<String>

    fun setValues(
        ps: PreparedStatement,
        entity: T,
        operation: Operation,
    )

    fun fromRow(rs: ResultSet): T

    fun getIdValue(entity: T): ID?

    // 새로 추가되는 메서드들
    fun setValuesForInsert(
        ps: PreparedStatement,
        entity: T,
        startIndex: Int,
    ): Int

    fun extractId(rs: ResultSet): Long

    fun getInsertParameterCount(): Int = columns.size - (if (idColumn in columns) 1 else 0) - systemColumns.size

    fun getColumnCount(): Int = columns.size - systemColumns.size - (if (idColumn in columns) 1 else 0)

    fun extractColumnValue(
        entity: T,
        columnName: String,
    ): Any?
}

enum class Operation {
    INSERT,
    UPDATE,
    UPSERT,
}
