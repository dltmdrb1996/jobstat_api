package com.wildrew.jobstat.community_read.community_read.repository

import com.wildrew.jobstat.community_read.community_read.fixture.BoardReadModelFixture
import com.wildrew.jobstat.community_read.model.BoardReadModel
import com.wildrew.jobstat.community_read.repository.impl.RedisBoardDetailRepository
import com.wildrew.jobstat.community_read.utils.base.RedisIntegrationTestSupport
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_serializer.DataSerializer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.connection.StringRedisConnection

@DisplayName("RedisBoardDetailRepository 통합 테스트")
class RedisBoardDetailRepositoryIntegrationTest : RedisIntegrationTestSupport() {
    @Autowired
    private lateinit var dataSerializer: DataSerializer

    @Autowired
    private lateinit var boardDetailRepository: RedisBoardDetailRepository

    @BeforeEach
    fun setUp() {
        flushAll()
    }

    private fun detailKey(boardId: Long) = RedisBoardDetailRepository.detailKey(boardId)

    @Nested
    @DisplayName("findBoardDetail 메서드")
    inner class FindBoardDetail {
        @Test
        @DisplayName("성공: 존재하는 게시글 ID로 조회 시 BoardReadModel 반환")
        fun `given existing board id, when findBoardDetail, then return BoardReadModel`() {
            // Given
            val boardId = 1L
            val boardModel =
                BoardReadModelFixture
                    .aBoardReadModel()
                    .withId(boardId)
                    .create()
            val boardJson = dataSerializer.serialize(boardModel)!!
            redisTemplate.opsForValue().set(detailKey(boardId), boardJson)
            // When
            val foundBoard = boardDetailRepository.findBoardDetail(boardId)
            // Then
            assertNotNull(foundBoard)
            assertEquals(boardModel, foundBoard)
        }

        @Test
        @DisplayName("성공: 존재하지 않는 게시글 ID로 조회 시 null 반환")
        fun `given non-existing board id, when findBoardDetail, then return null`() {
            // Given
            val boardId = 99L
            // When
            val foundBoard = boardDetailRepository.findBoardDetail(boardId)
            // Then
            assertNull(foundBoard)
        }

        @Test
        @DisplayName("실패: 저장된 데이터가 유효하지 않은 JSON일 경우 역직렬화 실패로 null 반환")
        fun `given invalid json data, when findBoardDetail, then return null due to deserialization error`() {
            // Given
            val boardId = 2L
            redisTemplate.opsForValue().set(detailKey(boardId), "invalid json data")
            // When
            val foundBoard = boardDetailRepository.findBoardDetail(boardId)
            // Then
            assertNull(foundBoard)
        }
    }

    @Nested
    @DisplayName("findBoardDetails 메서드")
    inner class FindBoardDetails {
        @Test
        @DisplayName("성공: 여러 ID 목록으로 조회 시 존재하는 게시글만 Map으로 반환")
        fun `given list of ids, when findBoardDetails, then return map of existing boards`() {
            // Given
            val board1 =
                BoardReadModelFixture
                    .aBoardReadModel()
                    .withId(1L)
                    .withTitle("Board 1")
                    .create()
            val board2 =
                BoardReadModelFixture
                    .aBoardReadModel()
                    .withId(2L)
                    .withTitle("Board 2")
                    .create()
            val board3 =
                BoardReadModelFixture
                    .aBoardReadModel()
                    .withId(3L)
                    .withTitle("Board 3")
                    .create()
            val nonExistingId = 99L

            redisTemplate.opsForValue().set(detailKey(board1.id), dataSerializer.serialize(board1)!!)
            redisTemplate.opsForValue().set(detailKey(board2.id), dataSerializer.serialize(board2)!!)

            val idsToFind = listOf(nonExistingId, board1.id, board3.id, board2.id)

            // When
            val foundMap = boardDetailRepository.findBoardDetails(idsToFind)

            // Then
            assertEquals(2, foundMap.size)
            assertTrue(foundMap.containsKey(board1.id))
            assertTrue(foundMap.containsKey(board2.id))
            assertFalse(foundMap.containsKey(nonExistingId))
            assertFalse(foundMap.containsKey(board3.id))
            assertEquals(board1, foundMap[board1.id])
            assertEquals(board2, foundMap[board2.id])
        }

        @Test
        @DisplayName("성공: 빈 ID 목록으로 조회 시 빈 Map 반환")
        fun `given empty list, when findBoardDetails, then return empty map`() {
            // Given
            val idsToFind = emptyList<Long>()
            // When
            val foundMap = boardDetailRepository.findBoardDetails(idsToFind)
            // Then
            assertTrue(foundMap.isEmpty())
        }
    }

    @Nested
    @DisplayName("saveBoardDetail / saveBoardDetails / saveBoardDetailInPipeline 메서드")
    inner class SaveBoardDetails {
        @Test
        @DisplayName("성공: saveBoardDetail 호출 시 Redis에 JSON 저장")
        fun `when saveBoardDetail, then store json in redis`() {
            // Given
            val board = BoardReadModelFixture.aBoardReadModel().withId(1L).create()
            val eventTs = board.eventTs
            // When
            boardDetailRepository.saveBoardDetail(board, eventTs)
            // Then
            val savedJson = redisTemplate.opsForValue().get(detailKey(board.id))
            assertNotNull(savedJson)
            assertEquals(board, dataSerializer.deserialize(savedJson!!, BoardReadModel::class))
        }

        @Test
        @DisplayName("성공: saveBoardDetails 호출 시 여러 게시글 JSON 저장 (파이프라인)")
        fun `when saveBoardDetails, then store multiple jsons in redis using pipeline`() {
            // Given
            val board1 = BoardReadModelFixture.aBoardReadModel().withId(1L).create()
            val board2 = BoardReadModelFixture.aBoardReadModel().withId(2L).create()
            val boards = listOf(board1, board2)
            val eventTs = System.currentTimeMillis()
            // When
            boardDetailRepository.saveBoardDetails(boards, eventTs)
            // Then
            val savedJson1 = redisTemplate.opsForValue().get(detailKey(board1.id))
            val savedJson2 = redisTemplate.opsForValue().get(detailKey(board2.id))
            assertNotNull(savedJson1)
            assertNotNull(savedJson2)
            assertEquals(board1, dataSerializer.deserialize(savedJson1!!, BoardReadModel::class))
            assertEquals(board2, dataSerializer.deserialize(savedJson2!!, BoardReadModel::class))
        }

        @Test
        @DisplayName("성공: saveBoardDetailInPipeline 호출 시 파이프라인 내에서 JSON 저장")
        fun `when saveBoardDetailInPipeline, then set json within pipeline`() {
            // Given
            val board = BoardReadModelFixture.aBoardReadModel().withId(1L).create()
            // When
            redisTemplate.executePipelined { connection ->
                val stringConn = connection as StringRedisConnection
                boardDetailRepository.saveBoardDetailInPipeline(stringConn, board)
                null
            }
            // Then
            val savedJson = redisTemplate.opsForValue().get(detailKey(board.id))
            assertNotNull(savedJson)
            assertEquals(board, dataSerializer.deserialize(savedJson!!, BoardReadModel::class))
        }

        @Test
        @DisplayName("실패: 직렬화 실패 시 AppException(SERIALIZATION_FAILURE) 발생")
        fun `given serialization fails, when save methods are called, then throw AppException`() {
            // Given
            val board = BoardReadModelFixture.aBoardReadModel().withId(1L).create()
            val mockSerializer = mock<DataSerializer>()
            whenever(mockSerializer.serialize(any())).thenThrow(AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE))

            val repoWithMockSerializer = RedisBoardDetailRepository(redisTemplate, mockSerializer)

            // When & Then: Test saveBoardDetailInPipeline
            val pipelineException =
                assertThrows<AppException> {
                    redisTemplate.executePipelined { connection ->
                        val stringConn = connection as StringRedisConnection
                        repoWithMockSerializer.saveBoardDetailInPipeline(stringConn, board)
                        null
                    }
                }
            assertEquals(ErrorCode.SERIALIZATION_FAILURE, pipelineException.errorCode)

            // When & Then: Test saveBoardDetail
            val detailException =
                assertThrows<AppException> {
                    repoWithMockSerializer.saveBoardDetail(board, System.currentTimeMillis())
                }
            assertEquals(ErrorCode.SERIALIZATION_FAILURE, detailException.errorCode)

            // When & Then: Test saveBoardDetails
            val detailsException =
                assertThrows<AppException> {
                    repoWithMockSerializer.saveBoardDetails(listOf(board), System.currentTimeMillis())
                }
            assertEquals(ErrorCode.SERIALIZATION_FAILURE, detailsException.errorCode)
        }
    }
}
