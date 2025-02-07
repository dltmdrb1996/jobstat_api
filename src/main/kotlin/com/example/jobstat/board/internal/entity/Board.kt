package com.example.jobstat.board.internal.entity

import com.example.jobstat.core.base.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

interface ReadBoard {
    val id: Long
    val title: String
    val content: String
    val author: String
    val password: String?
    val viewCount: Int
    val likeCount: Int
    val category: ReadBoardCategory
    val comments: List<ReadComment>
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
}

@Entity
@Table(name = "boards")
class Board protected constructor(
    title: String,
    content: String,
    author: String,
    password: String?,
    category: BoardCategory,
) : BaseEntity(),
    ReadBoard {
    @Column(nullable = false, length = 100)
    override var title: String = title
        protected set

    @Column(nullable = false, length = 5000)
    override var content: String = content
        protected set

    @Column(nullable = false, length = 50)
    override var author: String = author
        protected set

    @Column(length = 60)
    override var password: String? = password
        protected set

    @Column(nullable = false)
    override var viewCount: Int = 0
        protected set

    @Column(nullable = false)
    override var likeCount: Int = 0
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    override var category: BoardCategory = category
        protected set

    @OneToMany(mappedBy = "board", cascade = [CascadeType.ALL], orphanRemoval = true)
    override var comments: MutableList<Comment> = mutableListOf()
        protected set

    fun updateContent(
        newTitle: String,
        newContent: String,
    ) {
        require(newTitle.isNotBlank() && newTitle.length <= 100) { "제목은 1자에서 100자 사이여야 합니다" }
        require(newContent.isNotBlank() && newContent.length <= 5000) { "내용은 1자에서 5000자 사이여야 합니다" }
        this.title = newTitle
        this.content = newContent
    }

    fun addComment(comment: Comment) {
        comments.add(comment)
    }

    fun incrementViewCount() {
        viewCount++
    }

    fun incrementLikeCount() {
        likeCount++
    }

    companion object {
        fun create(
            title: String,
            content: String,
            author: String,
            password: String?,
            category: BoardCategory,
        ): Board {
            require(title.isNotBlank()) { "제목은 비워둘 수 없습니다" }
            require(title.length <= 100) { "제목은 1자에서 100자 사이여야 합니다" }
            require(content.isNotBlank()) { "내용은 비워둘 수 없습니다" }
            require(content.length <= 5000) { "내용은 1자에서 5000자 사이여야 합니다" }
            require(author.isNotBlank()) { "작성자는 비워둘 수 없습니다" }
            return Board(title, content, author, password, category)
        }
    }
}
