package com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.repository

import com.wildrew.jobstat.core.core_jdbc_batch.core.model.BatchOptions
import com.wildrew.jobstat.core.core_jdbc_batch.core.model.BatchResult
import com.wildrew.jobstat.core.core_jdbc_batch.core.model.SelectPage
import com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.update.ColumnUpdate
import java.time.LocalDateTime

interface BatchRepository<T, ID> {
    fun batchInsert(
        entities: List<T>,
        options: BatchOptions = BatchOptions(),
    ): BatchResult<T>

    fun batchUpdate(
        entities: List<T>,
        options: BatchOptions = BatchOptions(),
    ): BatchResult<T>

    fun batchUpsert(
        entities: List<T>,
        options: BatchOptions = BatchOptions(),
    ): BatchResult<T>

    fun batchDelete(
        ids: List<ID>,
        options: BatchOptions = BatchOptions(),
    ): Int

    fun batchSelect(
        criteria: SelectPage<ID>,
        options: BatchOptions = BatchOptions(),
    ): List<T>

    fun findAll(): List<T>

    fun bulkValueUpdate(
        ids: List<ID>,
        columnUpdate: ColumnUpdate,
        options: BatchOptions = BatchOptions(),
    ): Int

    fun batchColumnUpdate(
        updates: List<Pair<ID, ColumnUpdate>>,
        options: BatchOptions = BatchOptions(),
    ): Int

    fun batchInsertWithReturning(
        entities: List<T>,
        options: BatchOptions = BatchOptions(),
    ): BatchResult<T>

    fun batchUpdateWithReturning(
        entities: List<T>,
        options: BatchOptions = BatchOptions(),
    ): BatchResult<T>

    fun batchUpsertWithReturning(
        entities: List<T>,
        options: BatchOptions = BatchOptions(),
    ): BatchResult<T>

    fun batchInsertWithIgnore(
        entities: List<T>,
        options: BatchOptions,
    ): BatchResult<T>

    fun batchSelectByIds(ids: List<ID>): List<T>

    fun findByDateRange(
        dateColumn: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        selectPage: SelectPage<ID>,
    ): List<T>
}
