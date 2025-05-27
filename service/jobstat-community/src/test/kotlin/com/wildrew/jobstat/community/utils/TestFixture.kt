package com.wildrew.jobstat.community.utils

interface TestFixture<T> {
    fun create(): T
}
