package com.example.jobstat.core.core_id_generator

interface SnowflakeGenerator {
    @Throws(IllegalStateException::class)
    fun nextId(): Long
}
