package com.example.jobstat.community_read.client.response

/**
 * 게시글 기본 응답 DTO
 */
data class BoardResponseDto(
    val id: Long,
    val title: String,
    val createdAt: String
) 