package com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.update

import java.sql.PreparedStatement
import java.sql.Timestamp
import java.sql.Types
import java.time.LocalDateTime

class UpdatableColumn<T, V>(
    val columnName: String,
    private val value: V,
    private val setter: (PreparedStatement, Int, V) -> Unit,
) {
    // V 타입의 값을 설정하는 함수를 외부에 제공
    fun setValue(
        ps: PreparedStatement,
        index: Int,
    ) {
        setter(ps, index, value)
    }

    companion object {
        fun <T> string(
            columnName: String,
            value: String?,
        ) = UpdatableColumn<T, String?>(
            columnName = columnName,
            value = value,
            setter = { ps, index, v ->
                if (v == null) {
                    ps.setNull(index, Types.VARCHAR)
                } else {
                    ps.setString(index, v)
                }
            },
        )

        fun <T> long(
            columnName: String,
            value: Long?,
        ) = UpdatableColumn<T, Long?>(
            columnName = columnName,
            value = value,
            setter = { ps, index, v ->
                if (v == null) {
                    ps.setNull(index, Types.BIGINT)
                } else {
                    ps.setLong(index, v)
                }
            },
        )

        fun <T> int(
            columnName: String,
            value: Int?,
        ) = UpdatableColumn<T, Int?>(
            columnName = columnName,
            value = value,
            setter = { ps, index, v ->
                if (v == null) {
                    ps.setNull(index, Types.INTEGER)
                } else {
                    ps.setInt(index, v)
                }
            },
        )

        fun <T> boolean(
            columnName: String,
            value: Boolean?,
        ) = UpdatableColumn<T, Boolean?>(
            columnName = columnName,
            value = value,
            setter = { ps, index, v ->
                if (v == null) {
                    ps.setNull(index, Types.BOOLEAN)
                } else {
                    ps.setBoolean(index, v)
                }
            },
        )

        fun <T> localDateTime(
            columnName: String,
            value: LocalDateTime?,
        ) = UpdatableColumn<T, LocalDateTime?>(
            columnName = columnName,
            value = value,
            setter = { ps, index, v ->
                if (v == null) {
                    ps.setNull(index, Types.TIMESTAMP)
                } else {
                    ps.setTimestamp(index, Timestamp.valueOf(v))
                }
            },
        )

        fun <T, E : Enum<E>> enum(
            columnName: String,
            value: E?,
        ) = UpdatableColumn<T, E?>(
            columnName = columnName,
            value = value,
            setter = { ps, index, v ->
                if (v == null) {
                    ps.setNull(index, Types.VARCHAR)
                } else {
                    ps.setString(index, v.name)
                }
            },
        )
    }
}
