package com.example.jobstat.community_read.repository

import org.springframework.data.redis.connection.StringRedisConnection

interface BoardCountRepository {
    fun getTotalCount(): Long

    fun applyCountInPipeline(
        conn: StringRedisConnection,
        delta: Long,
    )
}
