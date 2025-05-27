package com.wildrew.jobstat.auth.utils

interface TestFixture<T> {
    fun create(): T
}
