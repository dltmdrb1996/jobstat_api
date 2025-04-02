package com.example.jobstat.community.counting

data class BoardCounters(
    val boardId: Long,
    val viewCount: Int,
    val likeCount: Int,
    val userLiked: Boolean
)