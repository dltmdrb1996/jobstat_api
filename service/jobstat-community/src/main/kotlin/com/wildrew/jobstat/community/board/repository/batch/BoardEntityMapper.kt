package com.wildrew.jobstat.community.board.repository.batch

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.EntityMapper
import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.Operation
import org.springframework.stereotype.Component
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDateTime

@Component
class BoardEntityMapper : EntityMapper<Board, Long> {
    override val tableName: String = "boards"

    override val columns: List<String> =
        listOf(
            "id",
            "title",
            "content",
            "user_id",
            "author",
            "password",
            "view_count",
            "like_count",
            "comment_count",
            "category_id",
        )

    override val idColumn: String = "id"

    override val systemColumns: Set<String> = setOf("created_at", "updated_at")

    override fun fromRow(rs: ResultSet): Board =
        Board(
            title = rs.getString("title"),
            content = rs.getString("content"),
            author = rs.getString("author"),
            password = rs.getString("password")?.takeIf { !rs.wasNull() },
            categoryId = rs.getLong("category_id"),
            userId = rs.getLong("user_id").takeIf { !rs.wasNull() },
        ).apply {
            id = rs.getLong("id")
            setCreatedAtForJdbc(rs.getObject("created_at", LocalDateTime::class.java))
            setUpdatedAtForJdbc(rs.getObject("updated_at", LocalDateTime::class.java))
            setViewCountForJdbc(rs.getInt("view_count"))
            setLikeCountForJdbc(rs.getInt("like_count"))
            setCommentCountForJdbc(rs.getInt("comment_count"))
        }

    /**
     * INSERT 문을 위한 값 설정.
     * SQL 생성기가 ID 컬럼을 포함하여 SQL을 생성했는지 여부에 따라 동작이 달라지지 않도록,
     * entityMapper.columns에 정의된 순서대로 모든 값을 설정합니다.
     * (SQL 생성기가 isIdApplicationAssigned를 보고 columnsToInsert를 결정)
     */
    override fun setValuesForInsert(
        ps: PreparedStatement,
        entity: Board,
        startIndex: Int,
    ): Int {
        var index = startIndex
        // `columns` 리스트에 정의된 순서대로 값을 설정
        // `isIdApplicationAssigned`가 true이므로 `id`가 `columns`의 첫 번째 요소임
        ps.setString(index++, entity.title)
        ps.setString(index++, entity.content)
        if (entity.userId != null) {
            ps.setLong(index++, entity.userId!!)
        } else {
            ps.setNull(index++, Types.BIGINT)
        }
        ps.setString(index++, entity.author)
        if (entity.password != null) {
            ps.setString(index++, entity.password!!)
        } else {
            ps.setNull(index++, Types.VARCHAR)
        }
        ps.setInt(index++, entity.viewCount)
        ps.setInt(index++, entity.likeCount)
        ps.setInt(index++, entity.commentCount)
        ps.setLong(index++, entity.categoryId)

        return index
    }

    override fun setValues(
        ps: PreparedStatement,
        entity: Board,
        operation: Operation,
    ) {
        var index = 1

        when (operation) {
            Operation.INSERT -> {
                ps.setLong(index++, entity.id)
                setValuesForInsert(ps, entity, index)
            }
            Operation.UPSERT -> {
                ps.setLong(index++, entity.id)
                setValuesForInsert(ps, entity, index)
            }
            Operation.UPDATE -> {
                val next = setValuesForInsert(ps, entity, index)
                ps.setLong(next, entity.id)
            }
        }
    }

    override fun getIdValue(entity: Board): Long? = entity.id

    override fun extractId(rs: ResultSet): Long = rs.getLong(idColumn)

    /**
     * `setValuesForInsert`에서 실제로 설정하는 파라미터의 수.
     * `columns` 리스트의 크기와 동일 (systemColumns 제외).
     * `isIdApplicationAssigned`가 true이면 ID 컬럼도 포함.
     */
    override fun getInsertParameterCount(): Int = columns.size

    /**
     * EntityMapper가 알고 있는 총 컬럼 수 (ID 포함, systemColumns는 SQL 생성기가 별도 처리)
     */
    override fun getColumnCount(): Int = columns.size

    override fun extractColumnValue(
        entity: Board,
        columnName: String,
    ): Any? =
        when (columnName) {
            "id" -> entity.id
            "created_at" -> entity.createdAt
            "updated_at" -> entity.updatedAt
            "title" -> entity.title
            "content" -> entity.content
            "user_id" -> entity.userId
            "author" -> entity.author
            "password" -> entity.password
            "view_count" -> entity.viewCount
            "like_count" -> entity.likeCount
            "comment_count" -> entity.commentCount
            "category_id" -> entity.categoryId
            else -> throw IllegalArgumentException("Unknown column name: $columnName for Board entity")
        }
}
