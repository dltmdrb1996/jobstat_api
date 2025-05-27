package com.wildrew.jobstat.community_read.utils

interface TestFixture<T> {
    fun create(): T
}
