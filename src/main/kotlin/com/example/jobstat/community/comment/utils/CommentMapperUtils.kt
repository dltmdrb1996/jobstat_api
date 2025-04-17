package com.example.jobstat.community.comment.utils

import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.core.global.extension.toEpochMilli
import java.time.LocalDateTime

object CommentMapperUtils {
    internal inline fun <T> mapToCommentDto(
        comment: Comment,
        creator: (
            id: String,
            boardId: String,
            userId: Long?,
            author: String,
            content: String,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
            eventTs: Long,
        ) -> T,
    ): T =
        with(comment) {
            creator(
                id.toString(),
                board.id.toString(),
                userId,
                author,
                content,
                createdAt,
                updatedAt,
                updatedAt.toEpochMilli(),
            )
        }

    internal inline fun <T> mapToCommentDtoWithStringDates(
        comment: Comment,
        creator: (
            id: String,
            boardId: String,
            userId: Long?,
            author: String,
            content: String,
            createdAt: String,
            updatedAt: String,
            eventTs: Long,
        ) -> T,
    ): T =
        with(comment) {
            creator(
                id.toString(),
                board.id.toString(),
                userId,
                author,
                content,
                createdAt.toString(),
                updatedAt.toString(),
                updatedAt.toEpochMilli(),
            )
        }
}
