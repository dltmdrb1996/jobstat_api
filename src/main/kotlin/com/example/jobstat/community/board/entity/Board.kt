package com.example.jobstat.community.board.entity

import com.example.jobstat.community.board.utils.BoardConstants
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.core.base.AuditableEntitySnow
import jakarta.persistence.*

@Entity
@Table(name = "boards")
internal class Board protected constructor(
    title: String,
    content: String,
    author: String,
    password: String?,
    category: BoardCategory,
    userId: Long?,
) : AuditableEntitySnow() {
    @Column(nullable = false, length = BoardConstants.MAX_TITLE_LENGTH)
    var title: String = title
        protected set

    @Column(nullable = false, length = BoardConstants.MAX_CONTENT_LENGTH)
    var content: String = content
        protected set

    @Column(nullable = true)
    var userId: Long? = userId
        protected set

    @Column(nullable = false, length = BoardConstants.MAX_AUTHOR_LENGTH)
    var author: String = author
        protected set

    @Column(length = BoardConstants.ENCODED_PASSWORD_LENGTH)
    var password: String? = password
        protected set

    @Column(nullable = false)
    var viewCount: Int = 0
        protected set

    @Column(nullable = false)
    var likeCount: Int = 0
        protected set

    @Column(nullable = false)
    var commentCount: Int = 0
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: BoardCategory = category
        protected set

    @OneToMany(mappedBy = "board", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var comments: MutableList<Comment> = mutableListOf()
        protected set

    fun updateContent(
        newTitle: String,
        newContent: String,
    ) {
        require(newTitle.isNotBlank()) { BoardConstants.ErrorMessages.TITLE_REQUIRED }
        require(newTitle.length <= BoardConstants.MAX_TITLE_LENGTH) { BoardConstants.ErrorMessages.INVALID_TITLE }
        require(newContent.isNotBlank()) { BoardConstants.ErrorMessages.CONTENT_REQUIRED }
        require(newContent.length <= BoardConstants.MAX_CONTENT_LENGTH) { BoardConstants.ErrorMessages.INVALID_CONTENT }
        this.title = newTitle
        this.content = newContent
    }

    fun addComment(comment: Comment) {
        comments.add(comment)
        incrementCommentCount()
    }

    fun removeComment(comment: Comment) {
        comments.remove(comment)
        decrementCommentCount()
    }

    private fun incrementCommentCount() {
        commentCount++
    }

    private fun decrementCommentCount() {
        commentCount--
    }

    fun incrementViewCount(count: Int = 1) {
        viewCount += count
    }

    fun incrementLikeCount(count: Int = 1) {
        likeCount += count
    }

    companion object {
        fun create(
            title: String,
            content: String,
            author: String,
            password: String? = null,
            category: BoardCategory,
            userId: Long? = null,
        ): Board {
            validateTitle(title)
            validateContent(content)
            require(author.isNotBlank()) { BoardConstants.ErrorMessages.AUTHOR_REQUIRED }
            return Board(title, content, author, password, category, userId)
        }

        private fun validateTitle(title: String) {
            require(title.isNotBlank()) { BoardConstants.ErrorMessages.TITLE_REQUIRED }
            require(title.length <= BoardConstants.MAX_TITLE_LENGTH) { BoardConstants.ErrorMessages.INVALID_TITLE }
        }

        private fun validateContent(content: String) {
            require(content.isNotBlank()) { BoardConstants.ErrorMessages.CONTENT_REQUIRED }
            require(content.length <= BoardConstants.MAX_CONTENT_LENGTH) { BoardConstants.ErrorMessages.INVALID_CONTENT }
        }
    }
}
