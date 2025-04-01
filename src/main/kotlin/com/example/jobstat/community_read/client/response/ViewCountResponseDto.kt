package com.example.jobstat.community_read.client.response

import java.time.LocalDateTime

/**
 * 조회수 응답 DTO
 */
data class ViewCountResponseDto(
    val boardId: Long,
    val viewCount: Int,
    val incrementedAt: LocalDateTime
) 