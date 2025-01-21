package com.example.jobstat.utils

interface TestFixture<T> {
    fun create(): T
}