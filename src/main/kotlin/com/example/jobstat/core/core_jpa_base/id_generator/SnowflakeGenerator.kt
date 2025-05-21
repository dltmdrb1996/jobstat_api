package com.example.jobstat.core.core_jpa_base.id_generator

interface SnowflakeGenerator {
    @Throws(IllegalStateException::class)
    fun nextId(): Long
}
