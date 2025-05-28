package com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.update

data class UpdateValue<T, V>(
    val column: UpdatableColumn<T, V>,
    val value: V,
)
