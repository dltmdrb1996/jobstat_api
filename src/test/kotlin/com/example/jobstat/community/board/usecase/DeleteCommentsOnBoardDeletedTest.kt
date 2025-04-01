package com.example.jobstat.community.board.usecase

import com.example.jobstat.comment.service.CommentService
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("DeleteCommentsOnBoardDeleted 유스케이스 테스트")
class DeleteCommentsOnBoardDeletedTest {
    private lateinit var commentService: CommentService
    private lateinit var deleteCommentsOnBoardDeleted: DeleteCommentsOnBoardDeleted
    
    @BeforeEach
    fun setUp() {
        commentService = mock()
        deleteCommentsOnBoardDeleted = DeleteCommentsOnBoardDeleted(
            commentService,
            Validation.buildDefaultValidatorFactory().validator
        )
    }
    
    @Nested
    @DisplayName("댓글 삭제 기능")
    inner class DeleteComments {
        
        @Test
        @DisplayName("게시글 ID가 주어지면 관련된 모든 댓글을 삭제한다")
        fun deleteAllCommentsWithValidBoardId() {
            // given
            val boardId = 123L
            val deletedCount = 5
            whenever(commentService.deleteAllByBoardId(boardId)).thenReturn(deletedCount)
            
            // when
            val response = deleteCommentsOnBoardDeleted(DeleteCommentsOnBoardDeleted.Request(boardId))
            
            // then
            verify(commentService, times(1)).deleteAllByBoardId(boardId)
            assertTrue(response.success)
            assertEquals(deletedCount, response.deletedCount)
        }
        
        @Test
        @DisplayName("댓글이 없는 게시글도 성공적으로 처리한다")
        fun handleBoardWithNoComments() {
            // given
            val boardId = 123L
            whenever(commentService.deleteAllByBoardId(boardId)).thenReturn(0)
            
            // when
            val response = deleteCommentsOnBoardDeleted(DeleteCommentsOnBoardDeleted.Request(boardId))
            
            // then
            verify(commentService, times(1)).deleteAllByBoardId(boardId)
            assertTrue(response.success)
            assertEquals(0, response.deletedCount)
        }
        
        @Test
        @DisplayName("서비스에서 예외가 발생해도 유스케이스는 실패 응답을 반환한다")
        fun handleServiceException() {
            // given
            val boardId = 123L
            whenever(commentService.deleteAllByBoardId(boardId)).thenThrow(RuntimeException("Test exception"))
            
            // when
            val response = deleteCommentsOnBoardDeleted(DeleteCommentsOnBoardDeleted.Request(boardId))
            
            // then
            verify(commentService, times(1)).deleteAllByBoardId(boardId)
            assertFalse(response.success)
            assertEquals(0, response.deletedCount)
        }
    }
    
    @Nested
    @DisplayName("유효성 검사")
    inner class Validation {
        
        @Test
        @DisplayName("게시글 ID가 0 이하면 예외가 발생한다")
        fun validateBoardId() {
            // given
            val invalidBoardId = 0L
            
            // when & then
            assertFailsWith<ConstraintViolationException> {
                deleteCommentsOnBoardDeleted(DeleteCommentsOnBoardDeleted.Request(invalidBoardId))
            }
            
            verifyNoInteractions(commentService)
        }
    }
} 