package com.wildrew.jobstat.core.core_jdbc_batch.core.model

import com.wildrew.jobstat.core.core_jdbc_batch.core.exception.BatchProcessingException

data class BatchResult<T>(
    val successful: List<T>,
    val failed: List<Pair<T, Throwable>> = emptyList(),
) {
    val isFullySuccessful: Boolean get() = failed.isEmpty()

    fun throwIfAnyFailed(): Boolean {
        if (failed.isNotEmpty()) {
            throw BatchProcessingException(
                "Batch processing partially failed. Failed: ${failed.size}",
            )
        }
        return true
    }
}
