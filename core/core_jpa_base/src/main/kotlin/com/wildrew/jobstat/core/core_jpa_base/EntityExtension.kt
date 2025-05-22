package com.wildrew.jobstat.core.core_jpa_base

import jakarta.persistence.EntityNotFoundException
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

fun <T> T?.requireNotNull(exceptionSupplier: () -> Exception): T {
    if (this == null) {
        throw exceptionSupplier()
    }
    return this
}