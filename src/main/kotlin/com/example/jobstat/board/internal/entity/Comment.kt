package com.example.jobstat.board.internal.entity

import com.example.jobstat.core.base.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

interface ReadComment {
    val id: Long
    val content: String
    val author: String
    val password: String?
    val board: ReadBoard
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
}

@Entity
@Table(name = "comments")
internal class Comment protected constructor(
    content: String,
    author: String,
    password: String?,
    board: Board,
) : BaseEntity(),
    ReadComment {
    @Column(nullable = false, length = 1000)
    override var content: String = content
        protected set

    @Column(nullable = false, length = 50)
    override var author: String = author
        protected set

    @Column(length = 60)
    override var password: String? = password
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    override var board: Board = board
        protected set

    fun updateContent(newContent: String) {
        require(newContent.isNotBlank() && newContent.length <= 1000) { "댓글 내용은 1자 이상 1000자 이하여야 합니다" }
        this.content = newContent
    }

    companion object {
        fun create(
            content: String,
            author: String,
            password: String?,
            board: Board,
        ): Comment {
            require(content.isNotBlank()) { "댓글 내용은 필수입니다" }
            require(content.length <= 1000) { "댓글 내용은 1000자 이하여야 합니다" }
            require(content.isNotBlank()) { "댓글 내용은 필수입니다" }
            require(author.isNotBlank()) { "댓글 작성자는 필수입니다" }
            return Comment(content, author, password, board)
        }
    }
}
