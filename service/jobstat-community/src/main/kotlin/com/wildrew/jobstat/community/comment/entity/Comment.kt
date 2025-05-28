package com.wildrew.jobstat.community.comment.entity

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.comment.utils.CommentConstants
import com.wildrew.jobstat.core.core_jpa_base.base.AuditableEntitySnow
import jakarta.persistence.*

@Entity
@Table(name = "comments")
class Comment(
    content: String,
    author: String,
    password: String?,
    boardId: Long,
    userId: Long?,
) : AuditableEntitySnow() {
    @Column(nullable = false, length = CommentConstants.MAX_CONTENT_LENGTH)
    var content: String = content
        protected set

    @Column(nullable = false, length = CommentConstants.MAX_AUTHOR_LENGTH)
    var author: String = author
        protected set

    @Column(nullable = true)
    var userId: Long? = userId
        protected set

    @Column(length = CommentConstants.ENCODED_PASSWORD_LENGTH)
    var password: String? = password
        protected set

    @Column(name = "board_id", nullable = false)
    var boardId: Long = boardId
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", insertable = false, updatable = false)
    lateinit var board: Board
        protected set

    fun updateContent(newContent: String) {
        require(newContent.isNotBlank() && newContent.length <= CommentConstants.MAX_CONTENT_LENGTH) {
            CommentConstants.ErrorMessages.INVALID_CONTENT
        }
        this.content = newContent
    }

    companion object {
        fun create(
            content: String,
            author: String,
            password: String?,
            board: Board,
            userId: Long?,
        ): Comment {
            require(content.isNotBlank()) { CommentConstants.ErrorMessages.CONTENT_REQUIRED }
            require(content.length <= CommentConstants.MAX_CONTENT_LENGTH) {
                CommentConstants.ErrorMessages.INVALID_CONTENT
            }
            require(author.isNotBlank()) { CommentConstants.ErrorMessages.AUTHOR_REQUIRED }
            val comment = Comment(content, author, password, board.id, userId)
            comment.board = board

            board.addComment(comment)
            return comment
        }
    }
}
