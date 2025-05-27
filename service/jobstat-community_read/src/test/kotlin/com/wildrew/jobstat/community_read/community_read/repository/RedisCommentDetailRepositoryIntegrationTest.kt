package com.wildrew.jobstat.community_read.community_read.repository

import com.wildrew.jobstat.community_read.community_read.fixture.CommentReadModelFixture
import com.wildrew.jobstat.community_read.model.CommentReadModel
import com.wildrew.jobstat.community_read.repository.impl.RedisCommentDetailRepository
import com.wildrew.jobstat.community_read.utils.base.RedisIntegrationTestSupport
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_serializer.DataSerializer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.connection.StringRedisConnection

@DisplayName("RedisCommentDetailRepository 통합 테스트")
class RedisCommentDetailRepositoryIntegrationTest : RedisIntegrationTestSupport() {
    @Autowired
    private lateinit var dataSerializer: DataSerializer

    @Autowired
    private lateinit var commentDetailRepository: RedisCommentDetailRepository

    @BeforeEach
    fun setUp() {
        flushAll()
    }

    private fun detailKey(commentId: Long) = RedisCommentDetailRepository.detailKey(commentId)

    @Nested
    @DisplayName("findCommentDetail 메서드")
    inner class FindCommentDetail {
        @Test
        @DisplayName("성공: 존재하는 댓글 ID로 조회 시 CommentReadModel 반환")
        fun `given existing comment id, when findCommentDetail, then return CommentReadModel`() {
            val commentId = 101L
            val boardId = 1L
            val commentModel =
                CommentReadModelFixture
                    .aCommentReadModel()
                    .withId(commentId)
                    .withBoardId(boardId)
                    .create()
            redisTemplate.opsForValue().set(detailKey(commentId), dataSerializer.serialize(commentModel)!!) // Given
            val foundComment = commentDetailRepository.findCommentDetail(commentId) // When
            assertNotNull(foundComment)
            assertEquals(commentModel, foundComment) // Then
        }

        @Test
        @DisplayName("성공: 존재하지 않는 댓글 ID로 조회 시 null 반환")
        fun `given non-existing comment id, when findCommentDetail, then return null`() {
            assertNull(commentDetailRepository.findCommentDetail(999L)) // When & Then
        }

        @Test
        @DisplayName("실패: 저장된 데이터가 유효하지 않은 JSON일 경우 역직렬화 실패로 null 반환")
        fun `given invalid json data, when findCommentDetail, then return null due to deserialization error`() {
            redisTemplate.opsForValue().set(detailKey(2L), "invalid json data") // Given
            assertNull(commentDetailRepository.findCommentDetail(2L)) // When & Then
        }
    }

    @Nested
    @DisplayName("findCommentDetails 메서드")
    inner class FindCommentDetails {
        @Test
        @DisplayName("성공: 여러 ID 목록으로 조회 시 존재하는 댓글만 Map으로 반환")
        fun `given list of ids, when findCommentDetails, then return map of existing comments`() {
            val comment1 =
                CommentReadModelFixture
                    .aCommentReadModel()
                    .withId(101L)
                    .withBoardId(1L)
                    .create()
            val comment2 =
                CommentReadModelFixture
                    .aCommentReadModel()
                    .withId(102L)
                    .withBoardId(1L)
                    .create()
            redisTemplate.opsForValue().set(detailKey(comment1.id), dataSerializer.serialize(comment1)!!) // Given
            redisTemplate.opsForValue().set(detailKey(comment2.id), dataSerializer.serialize(comment2)!!)
            val idsToFind = listOf(comment1.id, 999L, comment2.id)
            val foundMap = commentDetailRepository.findCommentDetails(idsToFind) // When
            assertEquals(2, foundMap.size)
            assertTrue(foundMap.containsKey(comment1.id))
            assertTrue(foundMap.containsKey(comment2.id)) // Then
            assertEquals(comment1, foundMap[comment1.id])
            assertEquals(comment2, foundMap[comment2.id])
        }

        @Test
        @DisplayName("성공: 빈 ID 목록으로 조회 시 빈 Map 반환")
        fun `given empty list, when findCommentDetails, then return empty map`() {
            assertTrue(commentDetailRepository.findCommentDetails(emptyList()).isEmpty()) // When & Then
        }
    }

    @Nested
    @DisplayName("saveCommentDetail / saveCommentDetails / saveCommentDetailInPipeline 메서드")
    inner class SaveCommentDetails {
        private val comment =
            CommentReadModelFixture
                .aCommentReadModel()
                .withId(101L)
                .withBoardId(1L)
                .create()
        private val eventTs = comment.eventTs

        @Test
        @DisplayName("성공: saveCommentDetail 호출 시 Redis에 JSON 저장")
        fun `when saveCommentDetail, then store json in redis`() {
            commentDetailRepository.saveCommentDetail(comment, eventTs) // When
            val savedJson = redisTemplate.opsForValue().get(detailKey(comment.id)) // Then
            assertNotNull(savedJson)
            assertEquals(comment, dataSerializer.deserialize(savedJson!!, CommentReadModel::class))
        }

        @Test
        @DisplayName("성공: saveCommentDetails 호출 시 여러 댓글 JSON 저장 (파이프라인)")
        fun `when saveCommentDetails, then store multiple jsons using pipeline`() {
            val comment1 =
                CommentReadModelFixture
                    .aCommentReadModel()
                    .withId(101L)
                    .withBoardId(1L)
                    .create()
            val comment2 =
                CommentReadModelFixture
                    .aCommentReadModel()
                    .withId(102L)
                    .withBoardId(1L)
                    .create()
            val commonEventTs = System.currentTimeMillis()
            commentDetailRepository.saveCommentDetails(listOf(comment1, comment2), commonEventTs) // When
            val savedJson1 = redisTemplate.opsForValue().get(detailKey(comment1.id))
            val savedJson2 = redisTemplate.opsForValue().get(detailKey(comment2.id))
            assertNotNull(savedJson1)
            assertNotNull(savedJson2) // Then
            assertEquals(comment1, dataSerializer.deserialize(savedJson1!!, CommentReadModel::class))
            assertEquals(comment2, dataSerializer.deserialize(savedJson2!!, CommentReadModel::class))
        }

        @Test
        @DisplayName("성공: saveCommentDetailInPipeline 호출 시 파이프라인 내에서 JSON 저장")
        fun `when saveCommentDetailInPipeline, then set json within pipeline`() {
            redisTemplate.executePipelined { conn ->
                commentDetailRepository.saveCommentDetailInPipeline(conn as StringRedisConnection, comment)
                null
            } // When
            val savedJson = redisTemplate.opsForValue().get(detailKey(comment.id))
            assertNotNull(savedJson) // Then
            assertEquals(comment, dataSerializer.deserialize(savedJson!!, CommentReadModel::class))
        }

        @Test
        @DisplayName("실패: 직렬화 실패 시 AppException(SERIALIZATION_FAILURE) 발생")
        fun `given serialization fails, when save methods are called, then throw AppException`() {
            val mockSerializer = mock<DataSerializer>()
            whenever(mockSerializer.serialize(any())).thenThrow(AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE))

            val repoWithMockSerializer = RedisCommentDetailRepository(redisTemplate, mockSerializer)

            val pipelineException =
                assertThrows<AppException> {
                    redisTemplate.executePipelined {
                        repoWithMockSerializer.saveCommentDetailInPipeline(it as StringRedisConnection, comment)
                        null
                    }
                }
            assertEquals(ErrorCode.SERIALIZATION_FAILURE, pipelineException.errorCode)

            val detailException = assertThrows<AppException> { repoWithMockSerializer.saveCommentDetail(comment, eventTs) }
            assertEquals(ErrorCode.SERIALIZATION_FAILURE, detailException.errorCode)

            val detailsException = assertThrows<AppException> { repoWithMockSerializer.saveCommentDetails(listOf(comment), eventTs) }
            assertEquals(ErrorCode.SERIALIZATION_FAILURE, detailsException.errorCode)
        }
    }
}
