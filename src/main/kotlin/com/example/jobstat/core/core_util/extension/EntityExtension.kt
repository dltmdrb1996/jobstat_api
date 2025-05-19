package com.example.jobstat.core.core_util.extension

import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*

fun <T> Optional<T>.orThrowNotFound(
    entityName: String,
    id: Any,
): T =
    this.orElseThrow {
        EntityNotFoundException("$entityName:$id")
    }

fun <T> T?.orThrowNotFound(
    entityName: String,
    id: Any,
): T =
    this.requireNotNull {
        EntityNotFoundException("$entityName:$id")
    }

fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> =
    flow {
        require(size > 0) { "Size must be greater than 0." }

        val chunk = mutableListOf<T>()

        collect { value ->
            chunk.add(value)
            if (chunk.size == size) {
                emit(chunk.toList()) // 현재 청크가 완성되면 emit
                chunk.clear() // 버퍼 초기화
            }
        }

        if (chunk.isNotEmpty()) {
            emit(chunk.toList()) // 남은 데이터 방출
        }
    }
