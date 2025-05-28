package com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.sql

import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.EntityMapper
import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.SqlGenerator
import com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.update.ColumnUpdate
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class SnowflakeIdAwareSqlGenerator : SqlGenerator {
    // 생성된 SQL 쿼리를 캐싱하기 위한 ConcurrentHashMap.
    // 동일한 SQL을 반복적으로 생성하는 오버헤드를 줄이기 위해 사용.
    private val cache = ConcurrentHashMap<String, String>()

    override fun generateInsertSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String =
        cache.getOrPut("insert_$tableName") {
            // 이제 ID 컬럼도 삽입 대상에 포함 (시스템 정의 컬럼은 여전히 제외)
            // ID는 외부에서 생성되어 제공되므로 INSERT 문에 포함되어야 함.
            val insertableColumns =
                entityMapper.columns.filterNot { entityMapper.systemColumns.contains(it) }

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
            // 이제 ID 컬럼도 삽입 대상에 포함 (시스템 정의 컬럼은 여전히 제외)
            val insertableColumns =
                entityMapper.columns.filterNot { entityMapper.systemColumns.contains(it) }

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
            // ID 컬럼과 시스템 컬럼을 제외한 업데이트 가능한 컬럼 필터링
            val updatableColumns =
                entityMapper.columns.filterNot { col ->
                    col == entityMapper.idColumn || entityMapper.systemColumns.contains(col)
                }

            """
            UPDATE `$tableName` 
            SET ${updatableColumns.joinToString(", ") { "`$it` = ?" }},
                `updated_at` = CURRENT_TIMESTAMP(6)
            WHERE `${entityMapper.idColumn}` = ?
            """.trimIndent()
        }

    // UPSERT SQL 쿼리를 생성하는 메서드 (데이터가 없으면 INSERT, 있으면 UPDATE)
    override fun generateUpsertSql(
        tableName: String,
        entityMapper: EntityMapper<*, *>,
    ): String =
        cache.getOrPut("upsert_$tableName") {
            // 시스템 컬럼을 제외한 컬럼 리스트를 필터링 (ID 컬럼 포함)
            val allInsertableColumns =
                entityMapper.columns.filterNot {
                    it in entityMapper.systemColumns
                }

            // 업데이트 시 ID 컬럼은 제외 (PK는 업데이트 대상이 아님)
            val columnsToUpdateOnDuplicate =
                allInsertableColumns.filterNot { it == entityMapper.idColumn }

            // UPSERT SQL 쿼리를 생성.
            // ON DUPLICATE KEY 구문으로 충돌 발생 시 업데이트 처리.
            """
            INSERT INTO `$tableName` (
                ${allInsertableColumns.joinToString(", ") { "`$it`" }},
                `created_at`,
                `updated_at`
            )
            VALUES (
                ${allInsertableColumns.joinToString(", ") { "?" }},
                CURRENT_TIMESTAMP(6),
                CURRENT_TIMESTAMP(6)
            )
            ON DUPLICATE KEY UPDATE
                ${
                columnsToUpdateOnDuplicate.joinToString(", ") { col ->
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
            val allSelectableColumns = (entityMapper.columns + entityMapper.systemColumns).distinct()
            """
            SELECT ${allSelectableColumns.joinToString(", ") { "`$it`" }}
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
            val allSelectableColumns = (entityMapper.columns + entityMapper.systemColumns).distinct()
            """
            SELECT ${allSelectableColumns.joinToString(", ") { "`$it`" }}
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
        dateColumn: String, // 이 dateColumn이 systemColumns에 포함될 수도, 안 될 수도 있음에 유의
    ): String =
        cache.getOrPut("cursor_based_select_${tableName}_$dateColumn") {
            val allSelectableColumns = (entityMapper.columns + entityMapper.systemColumns).distinct()
            """
            SELECT ${allSelectableColumns.joinToString(", ") { "`$it`" }}
            FROM `$tableName`
            WHERE `$dateColumn` >= :startDate
              AND `$dateColumn` < :endDate
              AND (`deleted_at` IS NULL) -- 'deleted_at'도 systemColumns 또는 columns에 있어야 함
              AND `${entityMapper.idColumn}` > :lastId
            ORDER BY `${entityMapper.idColumn}` ASC
            LIMIT :limit
            """.trimIndent()
        }

    companion object {
        // MySQL의 최대 파라미터 제한을 고려한 값 (이 코드에서는 직접 사용되지 않음)
        // private const val MAX_IN_PARAMS = 1000
    }
}
