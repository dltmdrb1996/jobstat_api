package com.example.jobstat.community_read.repository

import org.springframework.data.redis.connection.StringRedisConnection

interface BoardCountRepository {
    /**
     * 게시글 총 개수 조회
     */
    fun getTotalCount(): Long
    
    /**
     * 파이프라인을 통한 게시글 개수 증감
     */
    fun applyCountInPipeline(conn: StringRedisConnection, delta: Long)
}