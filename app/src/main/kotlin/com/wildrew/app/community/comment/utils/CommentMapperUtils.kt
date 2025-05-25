package com.wildrew.app.community.comment.utils

import com.wildrew.app.common.toEpochMilli
import com.wildrew.app.community.comment.entity.Comment
import java.time.LocalDateTime

object CommentMapperUtils {
    inline fun <T> mapToCommentDto(
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

    inline fun <T> mapToCommentDtoWithStringDates(
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
