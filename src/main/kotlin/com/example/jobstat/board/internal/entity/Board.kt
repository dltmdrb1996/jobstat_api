package com.example.jobstat.board.internal.entity

import com.example.jobstat.core.base.SoftDeleteBaseEntity
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import jakarta.persistence.*

@Entity
@Table(name = "boards")
class Board(
    title: String,
    content: String,
    author: String,
) : SoftDeleteBaseEntity() {
    @Column(nullable = false, length = 100)
    var title: String = title
        protected set

    @Column(nullable = false, length = 5000)
    var content: String = content
        protected set

    @Column(nullable = false)
    var author: String = author
        protected set

    @Column(nullable = false)
    var viewCount: Int = 0
        protected set

    @Column(nullable = false)
    var likeCount: Int = 0
        protected set

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    var category: BoardCategory? = null
        protected set

    @OneToMany(mappedBy = "board", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    private val _comments: MutableList<Comment> = mutableListOf()
    val comments: List<Comment>
        get() = _comments

    @PrePersist
    @PreUpdate
    fun validate() {
        if (title.isBlank() || content.isBlank() || author.isBlank()) {
            throw AppException.fromErrorCode(ErrorCode.INVALID_ARGUMENT, "제목, 내용, 작성자는 비어있을 수 없습니다.")
        }
    }

    fun incrementViewCount() {
        viewCount++
    }

    fun incrementLikeCount() {
        likeCount++
    }

    fun updateContent(
        newTitle: String,
        newContent: String,
    ) {
        this.title = newTitle
        this.content = newContent
    }

    companion object {
        fun create(
            title: String,
            content: String,
            author: String,
        ): Board = Board(title, content, author)
    }
}
