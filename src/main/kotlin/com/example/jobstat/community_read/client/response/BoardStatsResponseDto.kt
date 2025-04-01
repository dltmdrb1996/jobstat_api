package com.example.jobstat.community_read.client.response

/**
 * 게시글 통계 응답 DTO
 */
data class BoardStatsResponseDto(
    val totalBoardCount: Long,
    val hasCommentedOnBoard: Boolean
) 