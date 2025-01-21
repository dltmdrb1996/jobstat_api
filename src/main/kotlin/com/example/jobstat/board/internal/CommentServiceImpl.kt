package com.example.jobstat.board.internal

import com.example.jobstat.board.CommentService
import com.example.jobstat.board.internal.entity.Comment
import com.example.jobstat.board.internal.repository.CommentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class CommentServiceImpl(
    private val commentRepository: CommentRepository,
) : CommentService {
    @Transactional
    override fun createComment(
        boardId: Long,
        content: String,
        author: String,
    ): Comment {
        val comment = Comment.create(content, author)
        return commentRepository.save(comment)
    }

    @Transactional(readOnly = true)
    override fun getCommentById(id: Long): Comment? = commentRepository.findById(id)

    @Transactional(readOnly = true)
    override fun getCommentsByBoardId(boardId: Long): List<Comment> = commentRepository.findByBoardId(boardId)

    @Transactional(readOnly = true)
    override fun getRecentCommentsByBoardId(boardId: Long): List<Comment> = commentRepository.findTop5ByBoardIdOrderByCreatedAtDesc(boardId)

    @Transactional
    override fun updateComment(
        id: Long,
        content: String,
    ): Comment {
        val comment = commentRepository.findById(id)
        comment.updateContent(content)
        return commentRepository.save(comment)
    }

    @Transactional
    override fun deleteComment(id: Long) {
        commentRepository.deleteById(id)
    }
}
