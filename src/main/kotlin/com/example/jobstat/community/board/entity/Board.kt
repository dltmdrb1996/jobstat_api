package com.example.jobstat.community.board.entity

import com.example.jobstat.community.board.BoardConstants
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.entity.ReadComment
import com.example.jobstat.core.base.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

interface ReadBoard {
    val id: Long
    val title: String
    val userId: Long?
    val content: String
    val author: String
    val password: String?
    val viewCount: Int
    val likeCount: Int
    val category: ReadBoardCategory
    val commentCount: Int
    val comments: List<ReadComment>
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
}

@Entity
@Table(name = "boards")
internal class Board protected constructor(
    title: String,
    content: String,
    author: String,
    password: String?,
    category: BoardCategory,
    userId: Long?,
) : BaseEntity(),
    ReadBoard {
    @Column(nullable = false, length = BoardConstants.MAX_TITLE_LENGTH)
    override var title: String = title
        protected set

    @Column(nullable = false, length = BoardConstants.MAX_CONTENT_LENGTH)
    override var content: String = content
        protected set

    @Column(nullable = true)
    override var userId: Long? = userId
        protected set

    @Column(nullable = false, length = BoardConstants.MAX_AUTHOR_LENGTH)
    override var author: String = author
        protected set

    @Column(length = BoardConstants.ENCODED_PASSWORD_LENGTH)
    override var password: String? = password
        protected set

    @Column(nullable = false)
    override var viewCount: Int = 0
        protected set

    @Column(nullable = false)
    override var likeCount: Int = 0
        protected set

    @Column(nullable = false)
    override var commentCount: Int = 0
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
            password: String? = null,
            category: BoardCategory,
            userId: Long? = null,
        ): Board {
            validateTitle(title)
            validateContent(content)
            require(author.isNotBlank()) { BoardConstants.ErrorMessages.AUTHOR_REQUIRED }
            password?.let {
                require(it.length in BoardConstants.MIN_PASSWORD_LENGTH..BoardConstants.MAX_PASSWORD_LENGTH) {
                    BoardConstants.ErrorMessages.INVALID_PASSWORD
                }
            }
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
