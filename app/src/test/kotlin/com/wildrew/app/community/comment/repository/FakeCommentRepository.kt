package com.wildrew.app.community.comment.repository // 패키지 조정

import com.wildrew.jobstat.community.comment.entity.Comment
import com.wildrew.jobstat.community.comment.fixture.CommentFixture
import com.wildrew.jobstat.utils.base.BaseFakeRepository // 제공된 BaseFakeRepository 임포트
import org.springframework.data.domain.*
import kotlin.math.min

internal class FakeCommentRepository : CommentRepository {
    private val baseRepo =
        object : BaseFakeRepository<Comment, CommentFixture>() {
            override fun fixture(): CommentFixture = CommentFixture.aComment()

            override fun createNewEntity(entity: Comment): Comment {
                if (!isValidId(entity.id)) {
                    setEntityId(entity, nextId())
                }
                return entity
            }

            override fun updateEntity(entity: Comment): Comment = entity
        }

    override fun save(comment: Comment): Comment = baseRepo.save(comment)

    override fun deleteById(id: Long) {
        baseRepo.deleteById(id)
    }

    override fun delete(comment: Comment) {
        deleteById(comment.id)
    }

    override fun deleteByBoardId(boardId: Long) {
        val commentsToDelete = baseRepo.findAll().filter { it.board.id == boardId }
        commentsToDelete.forEach { deleteById(it.id) }
    }

    override fun findAll(pageable: Pageable): Page<Comment> {
        val allComments = baseRepo.findAll()
        val content = applyPageable(allComments, pageable)
        return PageImpl(content, pageable, allComments.size.toLong())
    }

    override fun findById(id: Long): Comment = baseRepo.findById(id)

    override fun findAllByIds(ids: List<Long>): List<Comment> = ids.mapNotNull { baseRepo.findByIdOrNull(it) }

    override fun findByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Comment> {
        val filtered = baseRepo.findAll().filter { it.board.id == boardId }
        val content = applyPageable(filtered, pageable)
        return PageImpl(content, pageable, filtered.size.toLong())
    }

    override fun findRecentComments(
        boardId: Long,
        pageable: Pageable,
    ): List<Comment> {
        val filtered = baseRepo.findAll().filter { it.board.id == boardId }
        val sorted = filtered.sortedByDescending { it.createdAt }
        return sorted.take(pageable.pageSize)
    }

    override fun findCommentsByBoardIdAfter(
        boardId: Long,
        lastCommentId: Long?,
        limit: Int,
    ): List<Comment> {
        val filtered = baseRepo.findAll().filter { it.board.id == boardId }
        val sorted = filtered.sortedByDescending { it.id }
        val startIndex = if (lastCommentId == null) 0 else sorted.indexOfFirst { it.id == lastCommentId } + 1
        return if (startIndex in sorted.indices) {
            sorted.subList(startIndex, min(startIndex + limit, sorted.size))
        } else {
            emptyList()
        }
    }

    override fun countByBoardId(boardId: Long): Long = baseRepo.findAll().count { it.board.id == boardId }.toLong()

    override fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Comment> {
        val filtered = baseRepo.findAll().filter { it.author == author }
        val content = applyPageable(filtered, pageable)
        return PageImpl(content, pageable, filtered.size.toLong())
    }

    override fun findByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable,
    ): Page<Comment> {
        val filtered = baseRepo.findAll().filter { it.board.id == boardId && it.author == author }
        val content = applyPageable(filtered, pageable)
        return PageImpl(content, pageable, filtered.size.toLong())
    }

    override fun existsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
    ): Boolean = baseRepo.findAll().any { it.board.id == boardId && it.author == author }

    override fun findCommentsByAuthorAfter(
        author: String,
        lastCommentId: Long?,
        limit: Int,
    ): List<Comment> {
        val filtered = baseRepo.findAll().filter { it.author == author }
        val sorted = filtered.sortedByDescending { it.id } // ID 내림차순
        val startIndex = if (lastCommentId == null) 0 else sorted.indexOfFirst { it.id == lastCommentId } + 1
        return if (startIndex in sorted.indices) {
            sorted.subList(startIndex, min(startIndex + limit, sorted.size))
        } else {
            emptyList()
        }
    }

    override fun existsById(id: Long): Boolean = baseRepo.existsById(id)

    private fun <T> applyPageable(
        list: List<T>,
        pageable: Pageable,
    ): List<T> {
        val sortedList =
            if (pageable.sort.isSorted) {
                list.sortedWith { o1, o2 ->
                    pageable.sort
                        .map { order ->
                            compareValuesBy(o1, o2) { entity ->
                                when (order.property) {
                                    "id" -> (entity as? Comment)?.id ?: 0L
                                    "createdAt" -> (entity as? Comment)?.createdAt?.toString() ?: ""
                                    else -> 0
                                }
                            }.let { if (order.isDescending) -it else it }
                        }.sum()
                }
            } else {
                list.sortedBy { (it as? Comment)?.id ?: Long.MAX_VALUE }
            }

        val start = pageable.offset.toInt()
        val end = min(start + pageable.pageSize, sortedList.size)
        return if (start < sortedList.size && start < end) sortedList.subList(start, end) else emptyList()
    }

    fun clear() {
        baseRepo.clear()
    }
}
