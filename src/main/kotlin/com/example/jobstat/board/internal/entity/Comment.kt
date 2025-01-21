package com.example.jobstat.board.internal.entity

import com.example.jobstat.core.base.SoftDeleteBaseEntity
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import jakarta.persistence.*

@Entity
@Table(name = "comments")
class Comment private constructor(
    content: String,
    author: String,
) : SoftDeleteBaseEntity() {
    @Column(nullable = false, length = 1000)
    var content: String = content
        protected set

    @Column(nullable = false)
    var author: String = author
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    lateinit var board: Board
        protected set

    @PrePersist
    @PreUpdate
    fun validate() {
        if (content.isBlank() || author.isBlank()) {
            throw AppException.fromErrorCode(ErrorCode.INVALID_ARGUMENT, "내용과 작성자는 비어있을 수 없습니다.")
        }
    }

    fun updateContent(content: String) {
        this.content = content
    }

    companion object {
        fun create(
            content: String,
            author: String,
        ): Comment = Comment(content, author)
    }
}
