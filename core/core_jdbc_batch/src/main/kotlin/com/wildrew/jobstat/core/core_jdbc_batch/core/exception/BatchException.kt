package com.wildrew.jobstat.core.core_jdbc_batch.core.exception

sealed class BatchException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class BatchProcessingException(
    message: String,
    cause: Throwable? = null,
) : BatchException(message, cause)

class BatchResourceException(
    message: String,
    cause: Throwable? = null,
) : BatchException(message, cause)
