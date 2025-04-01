package com.example.jobstat.community_read.client.response

/**
 * 작성자 활동 응답 DTO
 */
data class AuthorActivitiesResponseDto(
    val boards: List<BoardActivity>,
    val comments: List<CommentActivity>,
    val boardsTotalCount: Long,
    val commentsTotalCount: Long,
    val boardsHasNext: Boolean,
    val commentsHasNext: Boolean
)

/**
 * 게시글 활동 항목 DTO
 */
data class BoardActivity(
    val id: Long,
    val title: String,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: String
)

/**
 * 댓글 활동 항목 DTO
 */
data class CommentActivity(
    val id: Long,
    val content: String,
    val boardId: Long,
    val boardTitle: String,
    val createdAt: String
) 