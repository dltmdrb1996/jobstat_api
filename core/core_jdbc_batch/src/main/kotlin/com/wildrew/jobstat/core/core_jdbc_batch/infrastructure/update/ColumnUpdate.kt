package com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.update

data class ColumnUpdate(
    val columnName: String,
    val value: Any?,
    val type: ColumnType,
)

enum class ColumnType {
    STRING,
    LONG,
    INT,
    BOOLEAN,
    LOCAL_DATE_TIME,
    ENUM,
}
