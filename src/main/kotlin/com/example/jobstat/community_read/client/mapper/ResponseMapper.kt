package com.example.jobstat.community_read.client.mapper

import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.community_read.model.CommentReadModel
import com.example.jobstat.community_read.client.response.BoardReadResponse
import com.example.jobstat.community_read.client.response.CommentReadResponse

/**
 * 커뮤니티 도메인 모델 -> 읽기 응답 모델 변환 매퍼
 */
object ResponseMapper {


    /**
     * BoardReadModel을 BoardReadResponse로 변환
     */
    fun toResponse(boardReadModel: BoardReadModel): BoardReadResponse {
        return BoardReadResponse(
            id = boardReadModel.id,
            title = boardReadModel.title,
            content = boardReadModel.content,
            author = boardReadModel.author,
            categoryId = boardReadModel.categoryId,
            categoryName = boardReadModel.categoryName ?: "", // categoryName이 null일 경우 빈 문자열 처리
            viewCount = boardReadModel.viewCount,
            likeCount = boardReadModel.likeCount,
            commentCount = boardReadModel.commentCount,
            createdAt = boardReadModel.createdAt,
            updatedAt = boardReadModel.updatedAt
        )
    }
    /**
     * CommentReadModel을 CommentReadResponse로 변환
     */
    fun toResponse(commentReadModel: CommentReadModel): CommentReadResponse {
        return CommentReadResponse(
            id = commentReadModel.id,
            boardId = commentReadModel.boardId,
            content = commentReadModel.content,
            author = commentReadModel.author,
            createdAt = commentReadModel.createdAt,
            updatedAt = commentReadModel.updatedAt,
            parentId = commentReadModel.parentId,
            likeCount = commentReadModel.likeCount
        )
    }
} 