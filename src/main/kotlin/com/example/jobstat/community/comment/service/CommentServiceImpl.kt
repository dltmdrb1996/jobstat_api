package com.example.jobstat.community.comment.service

import com.example.jobstat.community.board.repository.BoardRepository
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.repository.CommentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
internal class CommentServiceImpl(
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
    ): Comment {
        // 게시글 조회 및 댓글 생성
        return boardRepository.findById(boardId).let { board ->
            Comment.create(
                content = content,
                author = author,
                password = password,
                board = board,
                userId = userId,
            ).let(commentRepository::save)
        }
    }

    override fun getCommentById(id: Long): Comment = commentRepository.findById(id)
      
    override fun getCommentsByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Comment> = commentRepository.findByBoardId(boardId, pageable)

    override fun getCommentsByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Comment> = commentRepository.findByAuthor(author, pageable)

    override fun getCommentsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable,
    ): Page<Comment> = commentRepository.findByBoardIdAndAuthor(boardId, author, pageable)

    @Transactional
    override fun updateComment(
        id: Long,
        content: String,
    ): Comment = getCommentById(id).apply {
        updateContent(content)
    }

    @Transactional
    override fun deleteComment(id: Long) {
        getCommentById(id).also { comment ->
            commentRepository.deleteById(comment.id)
            comment.board.removeComment(comment)
        }
    }
    
    override fun countCommentsByBoardId(boardId: Long): Long =
        commentRepository.countByBoardId(boardId)

    override fun hasCommentedOnBoard(
        boardId: Long,
        author: String,
    ): Boolean = commentRepository.existsByBoardIdAndAuthor(boardId, author)
    
    override fun getCommentsByIds(ids: List<Long>): List<Comment> = 
        commentRepository.findAllByIds(ids)
}
