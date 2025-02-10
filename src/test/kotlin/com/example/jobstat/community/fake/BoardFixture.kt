package com.example.jobstat.community.fake

import com.example.jobstat.community.internal.entity.Board
import com.example.jobstat.community.internal.entity.BoardCategory
import com.example.jobstat.utils.IdFixture
import java.time.LocalDateTime

internal class BoardFixture private constructor(
    private var title: String = "테스트 제목",
    private var content: String = "테스트 내용",
    private var author: String = "테스트사용자",
    private var password: String? = null,
    private var viewCount: Int = 0,
    private var likeCount: Int = 0,
    private var category: BoardCategory = CategoryFixture.aCategory().create(),
    private var createdAt: LocalDateTime = LocalDateTime.now(),
    private var updatedAt: LocalDateTime = LocalDateTime.now(),
) : IdFixture<Board>() {
    override fun create(): Board =
        Board.create(title, content, author, password, category).apply {
            repeat(viewCount) { incrementViewCount() }
            repeat(likeCount) { incrementLikeCount() }
        }

    fun withTitle(title: String) = apply { this.title = title }

    fun withContent(content: String) = apply { this.content = content }

    fun withAuthor(author: String) = apply { this.author = author }

    fun withPassword(password: String?) = apply { this.password = password }

    fun withViewCount(viewCount: Int) = apply { this.viewCount = viewCount }

    fun withLikeCount(likeCount: Int) = apply { this.likeCount = likeCount }

    fun withCategory(category: BoardCategory) = apply { this.category = category }

    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun withUpdatedAt(updatedAt: LocalDateTime) = apply { this.updatedAt = updatedAt }

    companion object {
        fun aBoard() = BoardFixture()
    }
}
