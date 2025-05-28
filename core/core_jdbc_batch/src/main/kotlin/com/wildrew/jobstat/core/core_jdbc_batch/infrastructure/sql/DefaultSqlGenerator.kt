package com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.sql

import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.EntityMapper
import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.SqlGenerator
import com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.update.ColumnUpdate
import java.util.concurrent.ConcurrentHashMap

class DefaultSqlGenerator : SqlGenerator {
    // 생성된 SQL 쿼리를 캐싱하기 위한 ConcurrentHashMap.
    // 동일한 SQL을 반복적으로 생성하는 오버헤드를 줄이기 위해 사용.
    private val cache = ConcurrentHashMap<String, String>()

    override fun generateInsertSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String =
        cache.getOrPut("insert_$tableName") {
            // ID 컬럼과 시스템에서 정의된 컬럼(systemColumns)을 제외한 삽입 가능한 컬럼을 필터링
            val insertableColumns =
                entityMapper.columns.filterNot {
                    it == entityMapper.idColumn || entityMapper.systemColumns.contains(it)
                }

            // INSERT SQL 쿼리를 생성. '?'는 각 컬럼의 값에 대한 플레이스홀더.
            // `created_at`과 `updated_at`은 자동으로 현재 타임스탬프를 입력.
            """
            INSERT INTO `$tableName` (
                ${insertableColumns.joinToString(", ") { "`$it`" }},
                `created_at`,
                `updated_at`
            )
            VALUES 
                (${insertableColumns.joinToString(", ") { "?" }}, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """.trimIndent()
        }

    override fun generateIgnoreInsertSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String =
        cache.getOrPut("insert_ignore_$tableName") {
            val insertableColumns =
                entityMapper.columns.filterNot {
                    it == entityMapper.idColumn || entityMapper.systemColumns.contains(it)
                }

            """
            INSERT IGNORE INTO `$tableName` (
                ${insertableColumns.joinToString(", ") { "`$it`" }},
                `created_at`,
                `updated_at`
            )
            VALUES 
                (${insertableColumns.joinToString(", ") { "?" }}, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """.trimIndent()
        }

    override fun generateUpdateSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String =
        cache.getOrPut("update_$tableName") {
            val updatableColumns =
                entityMapper.columns.filterNot { col ->
                    col == entityMapper.idColumn || entityMapper.systemColumns.contains(col)
                }

            """
        UPDATE `$tableName` 
        SET ${updatableColumns.joinToString(", ") { "`$it` = ?" }},
            `updated_at` = CURRENT_TIMESTAMP(6)
        WHERE `${entityMapper.idColumn}` = ?
        """
        }

    // UPSERT SQL 쿼리를 생성하는 메서드 (데이터가 없으면 INSERT, 있으면 UPDATE)
    override fun generateUpsertSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String =
        cache.getOrPut("upsert_$tableName") {
            // 시스템 컬럼을 제외한 컬럼 리스트를 필터링
            val upsertColumns =
                entityMapper.columns.filterNot {
                    it in entityMapper.systemColumns
                }

            // UPSERT SQL 쿼리를 생성.
            // ON DUPLICATE KEY 구문으로 충돌 발생 시 업데이트 처리.
            """
            INSERT INTO `$tableName` (
                ${upsertColumns.joinToString(", ") { "`$it`" }},
                `created_at`,
                `updated_at`
            )
            VALUES (
                ${upsertColumns.joinToString(", ") { "?" }},
                CURRENT_TIMESTAMP(6),
                CURRENT_TIMESTAMP(6)
            )
            ON DUPLICATE KEY UPDATE
                ${
                upsertColumns.filterNot { it == entityMapper.idColumn }.joinToString(", ") { col ->
                    "`$col` = VALUES(`$col`)"
                }
            },
                `updated_at` = CURRENT_TIMESTAMP(6)
            """.trimIndent()
        }

    override fun generateSelectSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String =
        cache.getOrPut("select_$tableName") {
            // ID 컬럼을 조건으로 정렬 및 페이징 처리된 결과를 가져오는 SELECT 쿼리 생성
            """
            SELECT ${entityMapper.columns.joinToString(", ") { "`$it`" }}
            FROM `$tableName`
            WHERE `${entityMapper.idColumn}` > :lastId
            ORDER BY `${entityMapper.idColumn}` ASC
            LIMIT :limit
            """.trimIndent()
        }

    override fun generateSelectAllSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String =
        cache.getOrPut("select_all_$tableName") {
            """
            SELECT ${entityMapper.columns.joinToString(", ") { "`$it`" }}
            FROM `$tableName`
            ORDER BY `${entityMapper.idColumn}` ASC
            """.trimIndent()
        }

    override fun generateDeleteSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String =
        cache.getOrPut("delete_$tableName") {
            """
            DELETE /*+ INDEX($tableName PRIMARY) */ FROM `$tableName` 
            WHERE `${entityMapper.idColumn}` IN (:ids)
            """.trimIndent()
        }

    override fun generateBulkValueUpdateSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
        columnUpdate: ColumnUpdate,
    ): String =
        cache.getOrPut("bulk_value_update_${tableName}_${columnUpdate.columnName}") {
            """
            UPDATE /*+ INDEX($tableName PRIMARY) */ `$tableName` 
            SET `${columnUpdate.columnName}` = :value,
                `updated_at` = CURRENT_TIMESTAMP(6)
            WHERE `${entityMapper.idColumn}` IN (:ids)
            """.trimIndent()
        }

    override fun generateColumnSpecificBatchUpdateSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
        columnName: String,
    ): String =
        cache.getOrPut("column_specific_update_${tableName}_$columnName") {
            """
            UPDATE /*+ INDEX($tableName PRIMARY) */ `$tableName` 
            SET `$columnName` = ?,
                `updated_at` = CURRENT_TIMESTAMP(6)
            WHERE `${entityMapper.idColumn}` = ?
            """.trimIndent()
        }

    override fun generateCursorBasedSelectSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
        dateColumn: String,
    ): String =
        cache.getOrPut("cursor_based_select_$tableName") {
            """
            SELECT ${entityMapper.columns.joinToString(", ") { "`$it`" }}
            FROM `$tableName`
            WHERE `$dateColumn` >= :startDate 
              AND `$dateColumn` < :endDate
              AND (`deleted_at` IS NULL)
              ${"AND `${entityMapper.idColumn}` > :lastId"}
            ORDER BY `${entityMapper.idColumn}` ASC
            LIMIT :limit
            """.trimIndent()
        }

    companion object {
        private const val MAX_IN_PARAMS = 1000
    }
}
