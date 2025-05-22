package com.wildrew.app.utils

interface TestFixture<T> {
    fun create(): T
}
