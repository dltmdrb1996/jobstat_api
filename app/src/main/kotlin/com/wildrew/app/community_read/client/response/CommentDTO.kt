package com.wildrew.app.community_read.client.response

import com.wildrew.app.community_read.model.CommentReadModel
import com.wildrew.app.eacheach.toEpochMilli
import java.time.LocalDateTime

data class CommentDTO(
    val id: Long,
    val boardId: Long,
    val userId: Long?,
    val author: String,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(dto: CommentDTO): CommentReadModel =
            CommentReadModel(
                id = dto.id,
                boardId = dto.boardId,
                userId = dto.userId,
                author = dto.author,
                content = dto.content,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt,
                eventTs = dto.updatedAt.toEpochMilli(),
            )

        fun fromList(dtoList: List<CommentDTO>): List<CommentReadModel> = dtoList.map { from(it) }
    }
}
