package com.example.jobstat.community.fake

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.utils.IdFixture
import java.time.LocalDateTime

internal class CommentFixture private constructor(
    private var content: String = "테스트 댓글",
    private var author: String = "댓글작성자",
    private var password: String? = null,
    private var board: Board = BoardFixture.aBoard().create(),
    private var userId: Long? = null,
    private var createdAt: LocalDateTime = LocalDateTime.now(),
    private var updatedAt: LocalDateTime = LocalDateTime.now(),
) : IdFixture<Comment>() {
    override fun create(): Comment = Comment.create(content, author, password, board, userId)

    fun withContent(content: String) = apply { this.content = content }

    fun withAuthor(author: String) = apply { this.author = author }

    fun withPassword(password: String?) = apply { this.password = password }

    fun withBoard(board: Board) = apply { this.board = board }

    fun withUserId(userId: Long?) = apply { this.userId = userId }

    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun withUpdatedAt(updatedAt: LocalDateTime) = apply { this.updatedAt = updatedAt }

    companion object {
        fun aComment() = CommentFixture()
    }
}
