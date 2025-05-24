package com.wildrew.app.community_read.repository

import com.wildrew.app.community_read.repository.impl.RedisCommentIdListRepository
import com.wildrew.app.utils.base.RedisIntegrationTestSupport
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.script.RedisScript

@DisplayName("RedisCommentIdListRepository 통합 테스트")
class RedisCommentIdListRepositoryIntegrationTest : RedisIntegrationTestSupport() {
    @Autowired
    @Qualifier("cursorPaginationScript")
    private lateinit var cursorPaginationScript: RedisScript<List<*>>

    private lateinit var commentIdListRepository: RedisCommentIdListRepository

    private val boardId = 1L
    private val key = RedisCommentIdListRepository.getBoardCommentsKey(boardId)

    @BeforeEach
    fun setUp() {
        flushAll()
        @Suppress("UNCHECKED_CAST")
        val typedCursorScript = cursorPaginationScript as RedisScript<List<*>>
        commentIdListRepository = RedisCommentIdListRepository(redisTemplate, typedCursorScript)
    }

    @Nested
    @DisplayName("댓글 ID 목록 조회 (Offset/Cursor/InfiniteScroll)")
    inner class ReadCommentIds {
        private val totalElements = 5L

        @BeforeEach fun setupData() {
            zAdd(key, 100.0, "105")
            zAdd(key, 90.0, "104")
            zAdd(key, 80.0, "103")
            zAdd(key, 70.0, "102")
            zAdd(key, 60.0, "101")
        }

        @Test
        @DisplayName("성공(Offset - readCommentsByBoardId): 페이지별 시간 역순 ID 목록 반환")
        fun `readCommentsByBoardId offset pagination`() {
            val page1 = commentIdListRepository.readCommentsByBoardId(boardId, PageRequest.of(0, 3))
            val page2 = commentIdListRepository.readCommentsByBoardId(boardId, PageRequest.of(1, 3))
            assertEquals(listOf(105L, 104L, 103L), page1.content)
            assertEquals(totalElements, page1.totalElements)
            assertTrue(page1.hasNext())
            assertEquals(listOf(102L, 101L), page2.content)
            assertEquals(totalElements, page2.totalElements)
            assertFalse(page2.hasNext())
        }

        @Test
        @DisplayName("성공(Cursor - readCommentsByBoardIdByCursor): 커서 기반 시간 역순 ID 목록 반환 (Lua Script)")
        fun `readCommentsByBoardIdByCursor lua cursor pagination`() {
            val page1 = commentIdListRepository.readCommentsByBoardIdByCursor(boardId, null, 2L)
            assertEquals(listOf(105L, 104L), page1)
            val page2 = commentIdListRepository.readCommentsByBoardIdByCursor(boardId, page1.lastOrNull(), 2L)
            assertEquals(listOf(103L, 102L), page2)
            val page3 = commentIdListRepository.readCommentsByBoardIdByCursor(boardId, page2.lastOrNull(), 2L)
            assertEquals(listOf(101L), page3)
            val page4 = commentIdListRepository.readCommentsByBoardIdByCursor(boardId, page3.lastOrNull(), 2L)
            assertTrue(page4.isEmpty())
            val page5 = commentIdListRepository.readCommentsByBoardIdByCursor(boardId, 999L, 2L)
            assertTrue(page5.isEmpty())
        }

        @Test
        @DisplayName("성공(InfiniteScroll - readAllByBoardInfiniteScroll): 첫 페이지 조회")
        fun `readAllByBoardInfiniteScroll first page`() {
            assertEquals(listOf(105L, 104L, 103L), commentIdListRepository.readAllByBoardInfiniteScroll(boardId, null, 3L))
        }

        @Test
        @DisplayName("성공(InfiniteScroll - readAllByBoardInfiniteScroll): 다음 페이지 조회 (Padding 사용)")
        fun `readAllByBoardInfiniteScroll next page uses padding`() {
            flushAll()
            zAddPadded(key, 100.0, 105L)
            zAddPadded(key, 90.0, 104L)
            zAddPadded(key, 80.0, 103L)
            zAddPadded(key, 70.0, 102L)
            val result = commentIdListRepository.readAllByBoardInfiniteScroll(boardId, 104L, 2L)
            assertEquals(listOf(103L, 102L), result)
        }

        @Test
        @DisplayName("성공(InfiniteScroll): 마지막 ID 스코어 없으면 빈 목록 반환")
        fun `readAllByBoardInfiniteScroll returns empty on missing score`() {
            assertTrue(commentIdListRepository.readAllByBoardInfiniteScroll(boardId, 999L, 2L).isEmpty())
        }
    }

    @Nested
    @DisplayName("add / delete 메서드 (패딩 사용)")
    inner class AddDeleteMethods {
        private val limit = RedisCommentIdListRepository.COMMENT_LIMIT_SIZE

        @Test
        @DisplayName("성공(add): 패딩된 ID/스코어 추가 및 Trim")
        fun `add uses padding and trims`() {
            val itemsToAdd = limit.toInt() + 5
            val baseScore = 1000.0
            val firstCommentId = 10L
            for (i in 0 until itemsToAdd) {
                val currentCommentId = firstCommentId + i
                val currentScore = baseScore + i
                commentIdListRepository.add(boardId, currentCommentId, currentScore)
            }

            assertEquals(limit, redisTemplate.opsForZSet().size(key)!!, "ZSet 크기가 limit과 일치해야 합니다.")

            assertNull(getPaddedScore(key, firstCommentId), "가장 낮은 점수의 요소(ID: $firstCommentId)는 제거되어야 합니다.")
            assertNull(getPaddedScore(key, firstCommentId + 1), "그 다음 낮은 점수의 요소(ID: ${firstCommentId + 1})는 제거되어야 합니다.")

            val lastAddedId = firstCommentId + itemsToAdd - 1
            assertNotNull(getPaddedScore(key, lastAddedId), "가장 높은 점수의 요소(ID: $lastAddedId)는 유지되어야 합니다.")
        }

        @Test
        @DisplayName("성공(add): 동일 ID/스코어 추가 시 변화 없음")
        fun `add same id score no change`() {
            zAddPadded(key, 100.0, 1L)
            val initSize = redisTemplate.opsForZSet().size(key)
            commentIdListRepository.add(boardId, 1L, 100.0)
            assertEquals(initSize, redisTemplate.opsForZSet().size(key))
            assertEquals(100.0, getPaddedScore(key, 1L))
        }

        @Test
        @DisplayName("성공(add): 동일 ID, 다른 스코어 추가 시 스코어 업데이트")
        fun `add same id different score updates score`() {
            zAddPadded(key, 100.0, 1L)
            commentIdListRepository.add(boardId, 1L, 150.0)
            assertEquals(150.0, getPaddedScore(key, 1L))
        }

        @Test
        @DisplayName("성공(delete): 패딩된 ID 제거")
        fun `delete uses padding`() {
            zAddPadded(key, 100.0, 1L)
            zAddPadded(key, 200.0, 2L)
            commentIdListRepository.delete(boardId, 1L)
            assertEquals(1, redisTemplate.opsForZSet().size(key))
            assertNull(getPaddedScore(key, 1L))
            assertNotNull(getPaddedScore(key, 2L))
        }
    }

    @Nested
    @DisplayName("파이프라인 업데이트 메서드 (Unpadded 사용)")
    inner class PipelineUpdateMethods {
        private val limit = RedisCommentIdListRepository.COMMENT_LIMIT_SIZE

        @Test
        @DisplayName("성공: addCommentInPipeline - Unpadded ID 추가 및 Trim")
        fun `addCommentInPipeline uses unpadded and trims`() {
            redisTemplate.executePipelined { conn ->
                repeat(limit.toInt() + 5) { commentIdListRepository.addCommentInPipeline(conn as StringRedisConnection, boardId, (it + 1).toLong(), (it + 1).toDouble()) }
                null
            }
            assertEquals(limit, redisTemplate.opsForZSet().size(key))
            assertNull(getScore(key, 1L))
            assertNotNull(getScore(key, limit + 5))
        }

        @Test
        @DisplayName("성공: removeCommentInPipeline - Unpadded ID 제거")
        fun `removeCommentInPipeline uses unpadded`() {
            zAdd(key, 100.0, "101")
            zAdd(key, 200.0, "102")
            redisTemplate.executePipelined { conn ->
                commentIdListRepository.removeCommentInPipeline(conn as StringRedisConnection, boardId, 101L)
                null
            }
            assertEquals(1, redisTemplate.opsForZSet().size(key))
            assertNull(getScore(key, 101L))
            assertNotNull(getScore(key, 102L))
        }
    }
}
