package com.wildrew.app.community_read.repository

import com.wildrew.app.community_read.repository.impl.RedisBoardIdListRepository
import com.wildrew.jobstat.core.core_global.model.BoardRankingMetric
import com.wildrew.app.utils.base.RedisIntegrationTestSupport
import com.wildrew.jobstat.core.core_global.model.BoardRankingPeriod
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.script.RedisScript

@DisplayName("RedisBoardIdListRepository 통합 테스트")
class RedisBoardIdListRepositoryIntegrationTest : RedisIntegrationTestSupport() {
    @Autowired
    @Qualifier("cursorPaginationScript")
    private lateinit var cursorPaginationScript: RedisScript<List<*>>

    @Autowired
    private lateinit var boardIdListRepository: RedisBoardIdListRepository

    @BeforeEach
    fun setUp() {
        flushAll()
    }

    @Nested
    @DisplayName("전체 게시글 목록 조회 readAllByTime")
    inner class ReadAllByTime {
        private val key = RedisBoardIdListRepository.ALL_BOARDS_KEY
        private val totalElements = 5L

        @BeforeEach
        fun setupData() {
            zAdd(key, 100.0, "5")
            zAdd(key, 90.0, "4")
            zAdd(key, 80.0, "3")
            zAdd(key, 70.0, "2")
            zAdd(key, 60.0, "1")
        }

        @Test
        @DisplayName("성공(Offset): 페이지별 시간 역순 ID 목록 반환 (Edge cases 포함)")
        fun `when readAllByTimeByOffset, then return paginated ids ordered by time desc with edge cases`() {
            // 1페이지
            val pageable1 = PageRequest.of(0, 3)
            val page1 = boardIdListRepository.readAllByTimeByOffset(pageable1)
            assertEquals(listOf(5L, 4L, 3L), page1.content)
            assertEquals(totalElements, page1.totalElements)
            assertTrue(page1.hasNext())
            // 2페이지 (페이지 크기보다 적은 데이터)
            val pageable2 = PageRequest.of(1, 3)
            val page2 = boardIdListRepository.readAllByTimeByOffset(pageable2)
            assertEquals(listOf(2L, 1L), page2.content)
            assertEquals(totalElements, page2.totalElements)
            assertFalse(page2.hasNext())
            // 3페이지 (빈 페이지)
            val pageable3 = PageRequest.of(2, 3)
            val page3 = boardIdListRepository.readAllByTimeByOffset(pageable3)
            assertTrue(page3.content.isEmpty())
            assertEquals(totalElements, page3.totalElements)
            assertFalse(page3.hasNext())
            // 오프셋이 총 개수를 초과
            val pageable4 = PageRequest.of(2, 2) // offset 4, size 2
            val page4 = boardIdListRepository.readAllByTimeByOffset(pageable4)
            assertEquals(listOf(1L), page4.content) // 마지막 요소
            assertEquals(totalElements, page4.totalElements)
            assertFalse(page4.hasNext())

            val pageable5 = PageRequest.of(3, 2) // offset 6
            val page5 = boardIdListRepository.readAllByTimeByOffset(pageable5)
            assertTrue(page5.content.isEmpty())
        }

        @Test
        @DisplayName("성공(Cursor): 커서 기반 시간 역순 ID 목록 반환 (Edge cases 포함)")
        fun `when readAllByTimeByCursor, then return cursor paginated ids ordered by time desc with edge cases`() {
            // 기본 페이징
            val page1 = boardIdListRepository.readAllByTimeByCursor(null, 2L)
            assertEquals(listOf(5L, 4L), page1)
            val page2 = boardIdListRepository.readAllByTimeByCursor(page1.lastOrNull(), 2L)
            assertEquals(listOf(3L, 2L), page2)
            // 남은 항목보다 큰 limit
            val page3 = boardIdListRepository.readAllByTimeByCursor(page2.lastOrNull(), 4L)
            assertEquals(listOf(1L), page3)
            // 더 이상 항목 없음
            val page4 = boardIdListRepository.readAllByTimeByCursor(page3.lastOrNull(), 2L)
            assertTrue(page4.isEmpty())
            // 존재하지 않는 커서
            val page5 = boardIdListRepository.readAllByTimeByCursor(99L, 2L)
            println(page5)
            assertTrue(page5.isEmpty())
        }
    }

    @Nested
    @DisplayName("카테고리별 게시글 목록 조회 (readAllByCategory...)")
    inner class ReadAllByCategory {
        private val categoryId = 10L
        private val key = RedisBoardIdListRepository.getCategoryKey(categoryId)
        private val totalElements = 3L

        @BeforeEach fun setupData() {
            zAdd(key, 100.0, "5")
            zAdd(key, 90.0, "4")
            zAdd(key, 80.0, "3")
        }

        @Test
        @DisplayName("성공(Offset): 페이지별 카테고리 ID 목록 반환")
        fun `offset pagination works for category`() {
            val page1 = boardIdListRepository.readAllByCategoryByOffset(categoryId, PageRequest.of(0, 2))
            assertEquals(listOf(5L, 4L), page1.content)
            assertEquals(totalElements, page1.totalElements)
            assertTrue(page1.hasNext())
            val page2 = boardIdListRepository.readAllByCategoryByOffset(categoryId, PageRequest.of(1, 2))
            assertEquals(listOf(3L), page2.content)
            assertEquals(totalElements, page2.totalElements)
            assertFalse(page2.hasNext())
        }

        @Test
        @DisplayName("성공(Cursor): 커서 기반 카테고리 ID 목록 반환")
        fun `cursor pagination works for category`() {
            val page1 = boardIdListRepository.readAllByCategoryByCursor(categoryId, null, 2L)
            assertEquals(listOf(5L, 4L), page1)
            val page2 = boardIdListRepository.readAllByCategoryByCursor(categoryId, page1.lastOrNull(), 2L)
            assertEquals(listOf(3L), page2)
            val page3 = boardIdListRepository.readAllByCategoryByCursor(categoryId, page2.lastOrNull(), 2L)
            assertTrue(page3.isEmpty())
        }
    }

    @Nested
    @DisplayName("랭킹별 게시글 목록 조회 (readAllBy[Metric][Period]...)")
    inner class ReadAllByRanking {
        private val totalElements = 4L
        private val rankings =
            listOf(
                Triple(BoardRankingMetric.LIKES, BoardRankingPeriod.DAY, listOf(11L, 10L, 12L, 13L)),
                Triple(BoardRankingMetric.LIKES, BoardRankingPeriod.WEEK, listOf(21L, 20L, 22L, 23L)),
                Triple(BoardRankingMetric.LIKES, BoardRankingPeriod.MONTH, listOf(31L, 30L, 32L, 33L)),
                Triple(BoardRankingMetric.VIEWS, BoardRankingPeriod.DAY, listOf(41L, 40L, 42L, 43L)),
                Triple(BoardRankingMetric.VIEWS, BoardRankingPeriod.WEEK, listOf(51L, 50L, 52L, 53L)),
                Triple(BoardRankingMetric.VIEWS, BoardRankingPeriod.MONTH, listOf(61L, 60L, 62L, 63L)),
            )

        @BeforeEach fun setupRankingData() {
            rankings.forEach { (metric, period, ids) ->
                val key = RedisBoardIdListRepository.getRankingKey(metric, period)!!
                ids.forEachIndexed { index, id -> zAdd(key, (totalElements - index).toDouble() * 100, id.toString()) }
            }
        }

        @TestFactory fun `offset pagination tests for all rankings`() =
            rankings.map { (metric, period, expectedOrder) ->
                DynamicTest.dynamicTest("성공(Offset - ${metric.name}/${period.name}): 페이지별 랭킹 ID 목록 반환") {
                    val pageable1 = PageRequest.of(0, 2)
                    val pageable2 = PageRequest.of(1, 2)
                    val page1 = boardIdListRepository.readAllBy(metric, period, pageable1)
                    val page2 = boardIdListRepository.readAllBy(metric, period, pageable2)
                    assertEquals(expectedOrder.subList(0, 2), page1.content)
                    assertEquals(totalElements, page1.totalElements)
                    assertTrue(page1.hasNext())
                    assertEquals(expectedOrder.subList(2, 4), page2.content)
                    assertEquals(totalElements, page2.totalElements)
                    assertFalse(page2.hasNext())
                }
            }

        @TestFactory fun `cursor pagination tests for all rankings`() =
            rankings.map { (metric, period, expectedOrder) ->
                DynamicTest.dynamicTest("성공(Cursor - ${metric.name}/${period.name}): 커서 기반 랭킹 ID 목록 반환") {
                    val limit = 3L
                    val page1 = boardIdListRepository.readAllByCursor(metric, period, null, limit)
                    val page2 = boardIdListRepository.readAllByCursor(metric, period, page1.lastOrNull(), limit)
                    val page3 = boardIdListRepository.readAllByCursor(metric, period, page2.lastOrNull(), limit)
                    assertEquals(expectedOrder.subList(0, 3), page1)
                    assertEquals(expectedOrder.subList(3, 4), page2)
                    assertTrue(page3.isEmpty())
                }
            }

        /**
         * 테스트 팩토리를 단순화하기 위한 헬퍼 확장 함수.
         * 메트릭과 기간에 따라 적절한 오프셋 기반 랭킹 조회 메서드를 호출합니다.
         */
        private fun BoardIdListRepository.readAllBy(
            metric: BoardRankingMetric,
            period: BoardRankingPeriod,
            pageable: Pageable,
        ): Page<Long> =
            when (metric) {
                BoardRankingMetric.LIKES ->
                    when (period) {
                        BoardRankingPeriod.DAY -> this.readAllByLikesDayByOffset(pageable)
                        BoardRankingPeriod.WEEK -> this.readAllByLikesWeekByOffset(pageable)
                        BoardRankingPeriod.MONTH -> this.readAllByLikesMonthByOffset(pageable)
                    }
                BoardRankingMetric.VIEWS ->
                    when (period) {
                        BoardRankingPeriod.DAY -> this.readAllByViewsDayByOffset(pageable)
                        BoardRankingPeriod.WEEK -> this.readAllByViewsWeekByOffset(pageable)
                        BoardRankingPeriod.MONTH -> this.readAllByViewsMonthByOffset(pageable)
                    }
            }

        /**
         * 테스트 팩토리를 단순화하기 위한 헬퍼 확장 함수.
         * 메트릭과 기간에 따라 적절한 커서 기반 랭킹 조회 메서드를 호출합니다.
         */
        private fun BoardIdListRepository.readAllByCursor(
            metric: BoardRankingMetric,
            period: BoardRankingPeriod,
            lastId: Long?,
            limit: Long,
        ): List<Long> =
            when (metric) {
                BoardRankingMetric.LIKES ->
                    when (period) {
                        BoardRankingPeriod.DAY -> this.readAllByLikesDayByCursor(lastId, limit)
                        BoardRankingPeriod.WEEK -> this.readAllByLikesWeekByCursor(lastId, limit)
                        BoardRankingPeriod.MONTH -> this.readAllByLikesMonthByCursor(lastId, limit)
                    }
                BoardRankingMetric.VIEWS ->
                    when (period) {
                        BoardRankingPeriod.DAY -> this.readAllByViewsDayByCursor(lastId, limit)
                        BoardRankingPeriod.WEEK -> this.readAllByViewsWeekByCursor(lastId, limit)
                        BoardRankingPeriod.MONTH -> this.readAllByViewsMonthByCursor(lastId, limit)
                    }
            }
    }

    @Nested
    @DisplayName("파이프라인 업데이트 메서드")
    inner class PipelineUpdateMethods {
        private val allKey = RedisBoardIdListRepository.ALL_BOARDS_KEY
        private val catId = 1L
        private val catKey = RedisBoardIdListRepository.getCategoryKey(catId)
        private val boardId1 = 101L
        private val boardId2 = 102L
        private val score1 = 100.0
        private val score2 = 200.0

        @Test
        @DisplayName("성공: addBoardInPipeline - 전체 목록에 게시글 추가 및 Trim")
        fun `addBoardInPipeline adds board to all list and trims`() {
            val limit = RedisBoardIdListRepository.ALL_BOARD_LIMIT_SIZE
            redisTemplate.executePipelined { connection ->
                repeat(limit.toInt() + 5) {
                    boardIdListRepository.addBoardInPipeline(connection as StringRedisConnection, (it + 1).toLong(), (it + 1).toDouble())
                }
                null
            }
            assertEquals(limit, redisTemplate.opsForZSet().size(allKey))
            assertNull(redisTemplate.opsForZSet().score(allKey, "1"))
            assertNotNull(redisTemplate.opsForZSet().score(allKey, (limit + 5).toString()))
        }

        @Test
        @DisplayName("성공: addBoardToCategoryInPipeline - 카테고리 목록에 게시글 추가 및 Trim")
        fun `addBoardToCategoryInPipeline adds board to category list and trims`() {
            val limit = RedisBoardIdListRepository.CATEGORY_LIMIT_SIZE
            redisTemplate.executePipelined { connection ->
                repeat(limit.toInt() + 3) {
                    boardIdListRepository.addBoardToCategoryInPipeline(connection as StringRedisConnection, (it + 1).toLong(), catId, (it + 1).toDouble())
                }
                null
            }
            assertEquals(limit, redisTemplate.opsForZSet().size(catKey))
            assertNull(redisTemplate.opsForZSet().score(catKey, "1"))
            assertNotNull(redisTemplate.opsForZSet().score(catKey, (limit + 3).toString()))
        }

        @Test
        @DisplayName("성공: removeBoardInPipeline - 전체 및 카테고리 목록에서 게시글 제거")
        fun `removeBoardInPipeline removes board from all and category lists`() {
            // Given: 두 개의 게시글을 두 목록에 추가
            redisTemplate.executePipelined { conn ->
                val sConn = conn as StringRedisConnection
                boardIdListRepository.addBoardInPipeline(sConn, boardId1, score1)
                boardIdListRepository.addBoardToCategoryInPipeline(sConn, boardId1, catId, score1)
                boardIdListRepository.addBoardInPipeline(sConn, boardId2, score2)
                boardIdListRepository.addBoardToCategoryInPipeline(sConn, boardId2, catId, score2)
                null
            }
            // When: 첫 번째 게시글 삭제
            redisTemplate.executePipelined { conn ->
                boardIdListRepository.removeBoardInPipeline(conn as StringRedisConnection, boardId1, catId)
                null
            }
            // Then: 두 목록에서 제거 확인
            assertEquals(1, redisTemplate.opsForZSet().size(allKey))
            assertEquals(1, redisTemplate.opsForZSet().size(catKey))
            assertNull(redisTemplate.opsForZSet().score(allKey, boardId1.toString()))
            assertNull(redisTemplate.opsForZSet().score(catKey, boardId1.toString()))
            assertNotNull(redisTemplate.opsForZSet().score(allKey, boardId2.toString()))
            assertNotNull(redisTemplate.opsForZSet().score(catKey, boardId2.toString()))
        }

        @Test
        @DisplayName("성공: replaceRankingListInPipeline - 랭킹 목록 교체 및 Trim")
        fun `replaceRankingListInPipeline replaces ranking list and trims`() {
            val rankKey = RedisBoardIdListRepository.BOARDS_BY_LIKES_WEEK_KEY
            val limit = RedisBoardIdListRepository.RANKING_LIMIT_SIZE
            zAdd(rankKey, 50.0, "old_board") // Given: 기존 랭킹 데이터
            // 제한을 초과하는 새 랭킹 데이터 생성
            val newRankings =
                (1..(limit + 10)).map {
                    com.wildrew.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload.RankingEntry(it.toLong(), (limit + 10 - it).toDouble())
                }
            // When: 랭킹 목록 교체
            redisTemplate.executePipelined { conn ->
                boardIdListRepository.replaceRankingListInPipeline(conn as StringRedisConnection, rankKey, newRankings)
                null
            }
            // Then: 목록이 교체, trim되고 이전 데이터가 삭제되었는지 확인
            assertEquals(limit, redisTemplate.opsForZSet().size(rankKey))
            assertNull(redisTemplate.opsForZSet().score(rankKey, "old_board"))
            assertNotNull(redisTemplate.opsForZSet().score(rankKey, "1"))
            assertNull(redisTemplate.opsForZSet().score(rankKey, (limit + 1).toString()))
        }
    }
}
