package com.example.jobstat.core.global.utils.id_generator

interface SnowflakeGenerator {
    @Throws(IllegalStateException::class)
    fun nextId(): Long
}
