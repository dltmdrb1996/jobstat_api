package com.wildrew.jobstat.core.core_jdbc_batch.core.model

data class SelectPage<ID>(
    val lastId: ID? = null,
    val limit: Int? = null,
)
