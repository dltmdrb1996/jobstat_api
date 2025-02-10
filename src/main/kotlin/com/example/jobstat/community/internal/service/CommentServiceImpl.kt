package com.example.jobstat.community.internal.service

import com.example.jobstat.community.internal.entity.Comment
import com.example.jobstat.community.internal.entity.ReadComment
import com.example.jobstat.community.internal.repository.BoardRepository
import com.example.jobstat.community.internal.repository.CommentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
internal class CommentServiceImpl(
    private val commentRepository: CommentRepository,
    private val boardRepository: BoardRepository,
) : CommentService {
    override fun createComment(
        boardId: Long,
        content: String,
        author: String,
        password: String?,
        userId: Long?,
    ): ReadComment {
        val board = boardRepository.findById(boardId)
        val comment = Comment.create(content, author, password, board, userId)
        board.addComment(comment)
        return commentRepository.save(comment)
    }

    @Transactional(readOnly = true)
    override fun getCommentById(id: Long): ReadComment = commentRepository.findById(id)

    @Transactional(readOnly = true)
    override fun getCommentsByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<ReadComment> = commentRepository.findByBoardId(boardId, pageable).map { it as ReadComment }

    @Transactional(readOnly = true)
    override fun getCommentsByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<ReadComment> = commentRepository.findByAuthor(author, pageable).map { it as ReadComment }

    @Transactional(readOnly = true)
    override fun getCommentsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable,
    ): Page<ReadComment> = commentRepository.findByBoardIdAndAuthor(boardId, author, pageable).map { it as ReadComment }

    @Transactional(readOnly = true)
    override fun getRecentCommentsByBoardId(boardId: Long): List<ReadComment> = commentRepository.findRecentComments(boardId, Pageable.ofSize(5))

    override fun updateComment(
        id: Long,
        content: String,
    ): ReadComment {
        val comment = commentRepository.findById(id)
        comment.updateContent(content)
        return commentRepository.save(comment)
    }

    override fun deleteComment(id: Long) {
        val comment = commentRepository.findById(id)
        comment.board.removeComment(comment)
        commentRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    override fun countCommentsByBoardId(boardId: Long): Long = commentRepository.countByBoardId(boardId)

    @Transactional(readOnly = true)
    override fun hasCommentedOnBoard(
        boardId: Long,
        author: String,
    ): Boolean = commentRepository.existsByBoardIdAndAuthor(boardId, author)
}
