package com.example.jobstat.community.fake.repository

import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.comment.repository.CommentRepository
import com.example.jobstat.community.fake.CommentFixture
import com.example.jobstat.utils.base.BaseFakeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

internal class FakeCommentRepository : CommentRepository {
    private val baseRepo =
        object : BaseFakeRepository<com.example.jobstat.community.comment.entity.Comment, CommentFixture>() {
            override fun fixture() = CommentFixture.aComment()

            override fun createNewEntity(entity: com.example.jobstat.community.comment.entity.Comment): com.example.jobstat.community.comment.entity.Comment {
                if (!isValidId(entity.id)) {
                    setEntityId(entity, nextId())
                }
                return entity
            }

            override fun updateEntity(entity: com.example.jobstat.community.comment.entity.Comment): com.example.jobstat.community.comment.entity.Comment = entity
        }

    override fun findAll(pageable: Pageable): Page<com.example.jobstat.community.comment.entity.Comment> {
        val comments = applyPageable(baseRepo.findAll(), pageable)
        return PageImpl(comments, pageable, baseRepo.findAll().size.toLong())
    }

    override fun save(comment: com.example.jobstat.community.comment.entity.Comment): com.example.jobstat.community.comment.entity.Comment = baseRepo.save(comment)

    override fun findById(id: Long): com.example.jobstat.community.comment.entity.Comment = baseRepo.findById(id)

    override fun findByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<com.example.jobstat.community.comment.entity.Comment> {
        val filtered = baseRepo.findAll().filter { it.board.id == boardId }
        val comments = applyPageable(filtered, pageable)
        return PageImpl(comments, pageable, filtered.size.toLong())
    }

    override fun findRecentComments(
        boardId: Long,
        pageable: Pageable,
    ): List<com.example.jobstat.community.comment.entity.Comment> =
        baseRepo
            .findAll()
            .filter { it.board.id == boardId }
            .sortedByDescending { it.createdAt }
            .let { applyPageable(it, pageable) }

    override fun deleteById(id: Long) {
        baseRepo.deleteById(id)
    }

    override fun countByBoardId(boardId: Long): Long = baseRepo.findAll().count { it.board.id == boardId }.toLong()

    override fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<com.example.jobstat.community.comment.entity.Comment> {
        val filtered = baseRepo.findAll().filter { it.author == author }
        val comments = applyPageable(filtered, pageable)
        return PageImpl(comments, pageable, filtered.size.toLong())
    }

    override fun findByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable,
    ): Page<com.example.jobstat.community.comment.entity.Comment> {
        val filtered = baseRepo.findAll().filter { it.board.id == boardId && it.author == author }
        val comments = applyPageable(filtered, pageable)
        return PageImpl(comments, pageable, filtered.size.toLong())
    }

    override fun existsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
    ): Boolean = baseRepo.findAll().any { it.board.id == boardId && it.author == author }

    override fun deleteByBoardId(boardId: Long) {
        baseRepo.findAll().filter { it.board.id == boardId }.forEach { baseRepo.delete(it) }
    }

    private fun <T> applyPageable(
        list: List<T>,
        pageable: Pageable,
    ): List<T> {
        val start = pageable.pageNumber * pageable.pageSize
        val end = minOf(start + pageable.pageSize, list.size)
        return if (start <= end) list.subList(start, end) else emptyList()
    }

    fun clear() {
        baseRepo.clear()
    }
}
