package com.wildrew.jobstat.core.core_jdbc_batch.core.model

data class BatchOptions(
    val batchSize: Int = 1000,
    val continueOnError: Boolean = false,
    val retryEnabled: Boolean = true,
)
