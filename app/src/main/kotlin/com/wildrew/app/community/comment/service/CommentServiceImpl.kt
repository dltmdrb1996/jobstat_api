package com.wildrew.app.community.comment.service

import com.wildrew.app.community.board.repository.BoardRepository
import com.wildrew.app.community.comment.entity.Comment
import com.wildrew.app.community.comment.repository.CommentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentServiceImpl(
    private val commentRepository: CommentRepository,
    private val boardRepository: BoardRepository,
) : CommentService {
    @Transactional
    override fun createComment(
        boardId: Long,
        content: String,
        author: String,
        password: String?,
        userId: Long?,
    ): Comment =
        boardRepository.findById(boardId).let { board ->
            Comment
                .create(
                    content = content,
                    author = author,
                    password = password,
                    board = board,
                    userId = userId,
                ).let(commentRepository::save)
        }

    @Transactional
    override fun updateComment(
        id: Long,
        content: String,
    ): Comment =
        getCommentById(id).apply {
            updateContent(content)
        }

    @Transactional
    override fun deleteComment(id: Long) {
        getCommentById(id).also { comment ->
            commentRepository.deleteById(comment.id)
            comment.board.removeComment(comment)
        }
    }

    override fun getCommentById(id: Long): Comment = commentRepository.findById(id)

    override fun getCommentsByIds(ids: List<Long>): List<Comment> = commentRepository.findAllByIds(ids)

    override fun getCommentsByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Comment> = commentRepository.findByBoardId(boardId, pageable)

    override fun getCommentsByBoardIdAfter(
        boardId: Long,
        lastCommentId: Long?,
        limit: Int,
    ): List<Comment> = commentRepository.findCommentsByBoardIdAfter(boardId, lastCommentId, limit)

    override fun countCommentsByBoardId(boardId: Long): Long = commentRepository.countByBoardId(boardId)

    override fun getCommentsByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Comment> = commentRepository.findByAuthor(author, pageable)

    override fun getCommentsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable,
    ): Page<Comment> = commentRepository.findByBoardIdAndAuthor(boardId, author, pageable)

    override fun hasCommentedOnBoard(
        boardId: Long,
        author: String,
    ): Boolean = commentRepository.existsByBoardIdAndAuthor(boardId, author)

    override fun getCommentsByAuthorAfter(
        author: String,
        lastCommentId: Long?,
        limit: Int,
    ): List<Comment> = commentRepository.findCommentsByAuthorAfter(author, lastCommentId, limit)
}
