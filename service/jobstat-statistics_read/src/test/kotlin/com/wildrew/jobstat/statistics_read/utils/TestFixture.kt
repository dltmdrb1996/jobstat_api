package com.wildrew.jobstat.statistics_read.utils

interface TestFixture<T> {
    fun create(): T
}
