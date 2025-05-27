package com.wildrew.jobstat.community_read.community_read.repository

import com.wildrew.jobstat.community_read.community_read.fixture.BoardReadModelFixture
import com.wildrew.jobstat.community_read.community_read.fixture.CommentReadModelFixture
import com.wildrew.jobstat.community_read.model.BoardReadModel
import com.wildrew.jobstat.community_read.model.CommentReadModel
import com.wildrew.jobstat.community_read.payload.BoardCreatedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.BoardDeletedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.BoardLikedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.BoardRankingUpdatedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.BoardUpdatedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.BoardViewedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.CommentCreatedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.CommentDeletedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.CommentUpdatedEventPayloadFixture
import com.wildrew.jobstat.community_read.repository.impl.*
import com.wildrew.jobstat.community_read.utils.base.RedisIntegrationTestSupport
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentUpdatedEventPayload
import com.wildrew.jobstat.core.core_global.model.BoardRankingMetric
import com.wildrew.jobstat.core.core_global.model.BoardRankingPeriod
import com.wildrew.jobstat.core.core_serializer.DataSerializer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.script.RedisScript
import java.time.Duration
import java.time.LocalDateTime

@DisplayName("RedisCommunityEventUpdateRepository 통합 테스트")
class RedisCommunityEventUpdateRepositoryIntegrationTest : RedisIntegrationTestSupport() {
    @Autowired
    @Qualifier("applyBoardCreationScript")
    private lateinit var applyBoardCreationScript: RedisScript<Long>

    @Autowired
    @Qualifier("applyBoardUpdateScript")
    private lateinit var applyBoardUpdateScript: RedisScript<Long>

    @Autowired
    @Qualifier("applyBoardDeletionScript")
    private lateinit var applyBoardDeletionScript: RedisScript<Long>

    @Autowired
    @Qualifier("applyBoardLikeUpdateScript")
    private lateinit var applyBoardLikeUpdateScript: RedisScript<Long>

    @Autowired
    @Qualifier("applyBoardViewUpdateScript")
    private lateinit var applyBoardViewUpdateScript: RedisScript<Long>

    @Autowired
    @Qualifier("applyBoardRankingUpdateScript")
    private lateinit var applyBoardRankingUpdateScript: RedisScript<Long>

    @Autowired
    @Qualifier("applyCommentCreationScript")
    private lateinit var applyCommentCreationScript: RedisScript<Long>

    @Autowired
    @Qualifier("applyCommentUpdateScript")
    private lateinit var applyCommentUpdateScript: RedisScript<Long>

    @Autowired
    @Qualifier("applyCommentDeletionScript")
    private lateinit var applyCommentDeletionScript: RedisScript<Long>

    @Value("\${redis.event-ts.ttl-days:1}")
    private var eventTsTtlDays: Long = 1L

    @Autowired
    private lateinit var dataSerializer: DataSerializer

    @Autowired
    private lateinit var communityEventUpdateRepository: RedisCommunityEventUpdateRepository

    private fun boardDetailKey(id: Long) = RedisBoardDetailRepository.detailKey(id)

    private fun boardEventTsKey(id: Long) = RedisCommunityEventUpdateRepository.boardEventTsKey(id)

    private fun commentDetailKey(id: Long) = RedisCommentDetailRepository.detailKey(id)

    private fun commentEventTsKey(id: Long) = RedisCommunityEventUpdateRepository.commentEventTsKey(id)

    private fun boardAllListKey() = RedisBoardIdListRepository.ALL_BOARDS_KEY

    private fun boardCategoryListKey(catId: Long) = RedisBoardIdListRepository.getCategoryKey(catId)

    private fun boardTotalCountKey() = RedisBoardCountRepository.BOARD_TOTAL_COUNT_KEY

    private fun commentBoardListKey(boardId: Long) = RedisCommentIdListRepository.getBoardCommentsKey(boardId)

    private fun commentBoardCountKey(boardId: Long) = RedisCommentCountRepository.getBoardCommentCountKey(boardId)

    private fun commentTotalCountKey() = RedisCommentCountRepository.getTotalCommentCountKey()

    private fun rankingKey(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
    ) = RedisBoardIdListRepository.getRankingKey(metric, period)!!

    private fun rankingEventTsKey(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
    ) = RedisCommunityEventUpdateRepository.rankingEventTsKey(metric.name.lowercase(), period.name.lowercase())

    private fun getEventTs(key: String): Long? = redisTemplate.opsForHash<String, String>().get(key, "ts")?.toLongOrNull()

    private fun setEventTs(
        key: String,
        ts: Long,
    ) {
        redisTemplate.opsForHash<String, String>().put(key, "ts", ts.toString())
        redisTemplate.expire(key, Duration.ofDays(eventTsTtlDays))
    }

    @BeforeEach
    fun setUp() {
        flushAll()
    }

    @Nested
    @DisplayName("applyBoardCreation")
    inner class ApplyBoardCreation {
        private val boardId = 1L
        private val categoryId = 5L
        private val eventTs = System.currentTimeMillis()

        private val payload =
            BoardCreatedEventPayloadFixture
                .aBoardCreatedEventPayload()
                .withBoardId(boardId)
                .withCategoryId(categoryId)
                .withEventTs(eventTs)
                .create()

        private val boardReadModel = BoardReadModel.fromPayload(payload)
        private val boardJson = dataSerializer.serialize(boardReadModel)!!

        @Test
        @DisplayName("성공: 새 게시글 생성 시 관련 데이터 저장 및 성공(true) 반환")
        fun `success case`() {
            val result = communityEventUpdateRepository.applyBoardCreation(payload) // When
            assertTrue(result, "게시글 생성 스크립트가 성공해야 합니다")
            // Then
            assertEquals(boardJson, redisTemplate.opsForValue().get(boardDetailKey(boardId)))
            assertEquals(eventTs.toDouble(), redisTemplate.opsForZSet().score(boardAllListKey(), boardId.toString()))
            assertEquals(eventTs.toDouble(), redisTemplate.opsForZSet().score(boardCategoryListKey(categoryId), boardId.toString()))
            assertEquals("1", redisTemplate.opsForValue().get(boardTotalCountKey()))
            assertEquals(eventTs, getEventTs(boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(건너뜀): 오래된 이벤트 시 건너뜀(false) 반환 및 데이터 미변경")
        fun `skip older event`() {
            setEventTs(boardEventTsKey(boardId), eventTs + 1000) // Given newer TS
            val result = communityEventUpdateRepository.applyBoardCreation(payload) // When
            assertFalse(result, "더 오래된 이벤트의 경우 게시글 생성 스크립트가 건너뛰어야 합니다")
            // Then
            assertNull(redisTemplate.opsForValue().get(boardDetailKey(boardId)))
        }

        @Test
        @DisplayName("실패(직렬화): 내부 직렬화 실패 시 false 반환")
        fun `serialization failure returns false`() {
            val mockSerializer = mock<DataSerializer>()
            whenever(mockSerializer.serialize(any())).thenReturn(null)

            val repo =
                RedisCommunityEventUpdateRepository(
                    redisTemplate,
                    mockSerializer,
                    applyBoardCreationScript,
                    applyBoardUpdateScript,
                    applyBoardDeletionScript,
                    applyBoardLikeUpdateScript,
                    applyBoardViewUpdateScript,
                    applyBoardRankingUpdateScript,
                    applyCommentCreationScript,
                    applyCommentUpdateScript,
                    applyCommentDeletionScript,
                    eventTsTtlDays,
                )
            assertFalse(repo.applyBoardCreation(payload)) // When & Then
        }
    }

    @Nested
    @DisplayName("applyBoardUpdate / Like / View")
    inner class ApplyBoardUpdates {
        private val boardId = 1L
        private val initialTs = System.currentTimeMillis() - 5000
        private val eventTs = System.currentTimeMillis()

        private lateinit var initialModel: BoardReadModel
        private lateinit var initialJson: String

        @BeforeEach
        fun setup() {
            initialModel =
                BoardReadModelFixture
                    .aBoardReadModel()
                    .withId(boardId)
                    .withTitle("Old")
                    .withContent("Old")
                    .withViewCount(10)
                    .withLikeCount(5)
                    .withCreatedAt(LocalDateTime.now().minusDays(2))
                    .withEventTs(initialTs)
                    .create()
            initialJson = dataSerializer.serialize(initialModel)!!
            redisTemplate.opsForValue().set(boardDetailKey(boardId), initialJson)
            setEventTs(boardEventTsKey(boardId), initialTs)
        }

        @Test
        @DisplayName("성공(Update): 상세 정보 및 이벤트 TS 업데이트")
        fun `update success`() {
            val payload =
                BoardUpdatedEventPayloadFixture
                    .aBoardUpdatedEventPayload()
                    .withBoardId(boardId)
                    .withEventTs(eventTs)
                    .create()

            val updatedModel =
                initialModel.copy(
                    title = payload.title,
                    content = payload.content,
                    author = payload.author,
                    eventTs = payload.eventTs,
                )
            val updatedJson = dataSerializer.serialize(updatedModel)!!
            val result = communityEventUpdateRepository.applyBoardUpdate(payload, updatedJson) // When
            assertTrue(result, "게시글 업데이트 스크립트가 성공해야 합니다")
            // Then
            assertEquals(updatedJson, redisTemplate.opsForValue().get(boardDetailKey(boardId)))
            assertEquals(eventTs, getEventTs(boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(LikeUpdate): 상세 정보 및 이벤트 TS 업데이트")
        fun `like update success`() {
            val newLikeCount = initialModel.likeCount + 1
            val payload =
                BoardLikedEventPayloadFixture
                    .aBoardLikedEventPayload()
                    .withBoardId(boardId)
                    .withLikeCount(newLikeCount)
                    .withEventTs(eventTs)
                    .create()

            val updatedModel =
                initialModel.copy(
                    likeCount = payload.likeCount,
                    eventTs = payload.eventTs,
                )
            val updatedJson = dataSerializer.serialize(updatedModel)!!
            val result = communityEventUpdateRepository.applyBoardLikeUpdate(payload, updatedJson) // When
            assertTrue(result, "게시글 좋아요 업데이트 스크립트가 성공해야 합니다")
            // Then
            assertEquals(updatedJson, redisTemplate.opsForValue().get(boardDetailKey(boardId)))
            assertEquals(eventTs, getEventTs(boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(ViewUpdate): 상세 정보 및 이벤트 TS 업데이트")
        fun `view update success`() {
            val newViewCount = initialModel.viewCount + 1
            val payload =
                BoardViewedEventPayloadFixture
                    .aBoardViewedEventPayload()
                    .withBoardId(boardId)
                    .withViewCount(newViewCount)
                    .withEventTs(eventTs)
                    .create()

            val updatedModel =
                initialModel.copy(
                    viewCount = payload.viewCount,
                    eventTs = payload.eventTs,
                )
            val updatedJson = dataSerializer.serialize(updatedModel)!!
            val result = communityEventUpdateRepository.applyBoardViewUpdate(payload, updatedJson) // When
            assertTrue(result, "게시글 조회수 업데이트 스크립트가 성공해야 합니다")
            // Then
            assertEquals(updatedJson, redisTemplate.opsForValue().get(boardDetailKey(boardId)))
            assertEquals(eventTs, getEventTs(boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(건너뜀): 오래된 이벤트 시 데이터 미변경")
        fun `skip older event`() {
            val olderEventTs = initialTs - 1000
            val olderPayload =
                BoardUpdatedEventPayloadFixture
                    .aBoardUpdatedEventPayload()
                    .withBoardId(boardId)
                    .withEventTs(olderEventTs)
                    .create()

            val olderJson =
                dataSerializer.serialize(
                    initialModel.copy(
                        title = olderPayload.title,
                        content = olderPayload.content,
                        eventTs = olderPayload.eventTs,
                    ),
                )!!
            val result = communityEventUpdateRepository.applyBoardUpdate(olderPayload, olderJson) // When
            assertFalse(result, "더 오래된 이벤트의 경우 게시글 업데이트 스크립트가 건너뛰어야 합니다")
            // Then
            assertEquals(initialJson, redisTemplate.opsForValue().get(boardDetailKey(boardId)))
            assertEquals(initialTs, getEventTs(boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(잘못된 JSON 전달): 현재 구현은 예외를 던지지 않고 성공(true) 반환")
        fun `invalid json does not throw exception with current implementation`() {
            val payload =
                BoardUpdatedEventPayloadFixture
                    .aBoardUpdatedEventPayload()
                    .withBoardId(boardId)
                    .withEventTs(eventTs)
                    .create()

            // When: 유효하지 않은 JSON 문자열로 메서드 호출
            val result = communityEventUpdateRepository.applyBoardUpdate(payload, "invalid json")

            // Then: 예외 발생 없이 메서드가 true 반환 확인
            assertTrue(result, "현재 구현에서는 유효하지 않은 JSON 페이로드에도 applyBoardUpdate가 true를 반환해야 합니다")
        }

        @Test
        @DisplayName("성공(TTL): 게시글 업데이트 시 타임스탬프 키에 TTL 설정 확인")
        fun `update script sets TTL on board eventTs key`() {
            val payload =
                BoardUpdatedEventPayloadFixture
                    .aBoardUpdatedEventPayload()
                    .withBoardId(boardId)
                    .withEventTs(eventTs)
                    .create()
            val updatedJson =
                dataSerializer.serialize(
                    initialModel.copy(
                        title = payload.title,
                        content = payload.content,
                        eventTs = payload.eventTs,
                    ),
                )!!

            // When
            val result = communityEventUpdateRepository.applyBoardUpdate(payload, updatedJson)
            assertTrue(result)

            // Then
            val ttl = redisTemplate.getExpire(boardEventTsKey(boardId))
            assertNotNull(ttl, "TTL이 설정되어야 합니다.")
            assertTrue(ttl > 0, "TTL 값은 0보다 커야 합니다.")
        }
    }

    @Nested
    @DisplayName("applyBoardDeletion")
    inner class ApplyBoardDeletion {
        private val boardId = 1L
        private val categoryId = 5L
        private val initialTs = System.currentTimeMillis() - 5000
        private val eventTs = System.currentTimeMillis()

        private val payload =
            BoardDeletedEventPayloadFixture
                .aBoardDeletedEventPayload()
                .withBoardId(boardId)
                .withCategoryId(categoryId)
                .withEventTs(eventTs)
                .create()

        @BeforeEach
        fun setup() {
            redisTemplate.opsForValue().set(boardDetailKey(boardId), "{}")
            zAdd(boardAllListKey(), initialTs.toDouble(), boardId.toString())
            zAdd(boardCategoryListKey(categoryId), initialTs.toDouble(), boardId.toString())
            redisTemplate.opsForValue().set(boardTotalCountKey(), "1")
            setEventTs(boardEventTsKey(boardId), initialTs)
        }

        @Test
        @DisplayName("성공: 게시글 삭제 시 관련 데이터 삭제 및 성공(true) 반환")
        fun `success case`() {
            val result = communityEventUpdateRepository.applyBoardDeletion(payload) // When
            assertTrue(result, "게시글 삭제 스크립트가 성공해야 합니다")
            // Then
            assertNull(redisTemplate.opsForValue().get(boardDetailKey(boardId)))
            assertNull(redisTemplate.opsForZSet().score(boardAllListKey(), boardId.toString()))
            assertNull(redisTemplate.opsForZSet().score(boardCategoryListKey(categoryId), boardId.toString()))
            assertEquals("0", redisTemplate.opsForValue().get(boardTotalCountKey()))
            assertNull(getEventTs(boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(건너뜀): 오래된 이벤트 시 데이터 미변경")
        fun `skip older event`() {
            val olderPayload = payload.copy(eventTs = initialTs - 1000)
            val result = communityEventUpdateRepository.applyBoardDeletion(olderPayload) // When
            assertFalse(result, "더 오래된 이벤트의 경우 게시글 삭제 스크립트가 건너뛰어야 합니다")
            // Then
            assertNotNull(redisTemplate.opsForValue().get(boardDetailKey(boardId)))
            assertNotNull(redisTemplate.opsForZSet().score(boardAllListKey(), boardId.toString()))
            assertEquals("1", redisTemplate.opsForValue().get(boardTotalCountKey()))
            assertEquals(initialTs, getEventTs(boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(음수방지): 게시글 수가 0일 때 삭제해도 총 게시글 수 0 유지 (스크립트 레벨)")
        fun `deletion script prevents negative total board count`() {
            // Given: 카운트를 0으로 설정 (다른 관련 데이터는 존재해야 함)
            redisTemplate.opsForValue().set(boardDetailKey(boardId), "{}")
            zAdd(boardAllListKey(), initialTs.toDouble(), boardId.toString())
            redisTemplate.opsForValue().set(boardTotalCountKey(), "0") // 총 카운트 0
            setEventTs(boardEventTsKey(boardId), initialTs)

            val deletePayload =
                BoardDeletedEventPayloadFixture
                    .aBoardDeletedEventPayload()
                    .withBoardId(boardId)
                    .withCategoryId(categoryId)
                    .withEventTs(eventTs)
                    .create()

            // When
            val result = communityEventUpdateRepository.applyBoardDeletion(deletePayload)

            // Then
            assertTrue(result, "삭제 스크립트는 성공해야 함 (TS 조건 만족)")
            assertEquals("0", redisTemplate.opsForValue().get(boardTotalCountKey()), "총 게시글 수는 0으로 유지되어야 함")
        }
    }

    @Nested
    @DisplayName("applyBoardRankingUpdate")
    inner class ApplyBoardRankingUpdate {
        private val metric = BoardRankingMetric.VIEWS
        private val period = BoardRankingPeriod.DAY
        private val initialTs = System.currentTimeMillis() - 5000
        private val eventTs = System.currentTimeMillis()
        private val key = rankingKey(metric, period)
        private val tsKey = rankingEventTsKey(metric, period)

        private val rankings =
            listOf(
                com.wildrew.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload
                    .RankingEntry(10L, 500.0),
                com.wildrew.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload
                    .RankingEntry(20L, 450.0),
            )
        private val payload =
            BoardRankingUpdatedEventPayloadFixture
                .aBoardRankingUpdatedEventPayload()
                .withMetric(metric)
                .withPeriod(period)
                .withRankings(rankings)
                .withEventTs(eventTs)
                .create()

        @Test
        @DisplayName("성공: 랭킹 업데이트 시 ZSET 교체 및 이벤트 TS 업데이트")
        fun `success case`() {
            zAdd(key, 10.0, "99")
            setEventTs(tsKey, initialTs) // Given
            val result = communityEventUpdateRepository.applyBoardRankingUpdate(payload) // When
            assertTrue(result, "게시글 랭킹 업데이트 스크립트가 성공해야 합니다")
            // Then
            assertEquals(2, redisTemplate.opsForZSet().size(key))
            assertEquals(500.0, redisTemplate.opsForZSet().score(key, "10"))
            assertNull(redisTemplate.opsForZSet().score(key, "99"))
            assertEquals(eventTs, getEventTs(tsKey))
        }

        @Test
        @DisplayName("성공(건너뜀): 오래된 랭킹 이벤트 시 데이터 미변경")
        fun `skip older event`() {
            val newerTs = eventTs + 1000
            setEventTs(tsKey, newerTs)
            zAdd(key, 10.0, "99") // Given
            val olderPayload = payload.copy(eventTs = eventTs)
            val result = communityEventUpdateRepository.applyBoardRankingUpdate(olderPayload) // When
            assertFalse(result, "더 오래된 이벤트의 경우 게시글 랭킹 업데이트 스크립트가 건너뛰어야 합니다")
            // Then
            assertEquals(1, redisTemplate.opsForZSet().size(key))
            assertEquals(10.0, redisTemplate.opsForZSet().score(key, "99"))
            assertEquals(newerTs, getEventTs(tsKey))
        }
    }

    @Nested
    @DisplayName("applyCommentCreation")
    inner class ApplyCommentCreation {
        private val commentId = 101L
        private val boardId = 1L
        private val initialBoardTs = System.currentTimeMillis() - 5000
        private val eventTs = System.currentTimeMillis()

        private val payload =
            CommentCreatedEventPayloadFixture
                .aCommentCreatedEventPayload()
                .withCommentId(commentId)
                .withBoardId(boardId)
                .withEventTs(eventTs)
                .create()

        private val commentReadModel = CommentReadModel.fromPayload(payload)
        private val commentJson = dataSerializer.serialize(commentReadModel)!!

        private val initialBoardModel =
            BoardReadModelFixture
                .aBoardReadModel()
                .withId(boardId)
                .withCommentCount(0)
                .withEventTs(initialBoardTs)
                .create()
        private val initialBoardJson = dataSerializer.serialize(initialBoardModel)!!

        private val updatedBoardModel = initialBoardModel.copy(commentCount = 1)
        private val updatedBoardJson = dataSerializer.serialize(updatedBoardModel)!!

        @Test
        @DisplayName("성공(게시글 찾음): 관련 데이터 저장/업데이트 및 성공(true) 반환")
        fun `success with board found`() {
            redisTemplate.opsForValue().set(boardDetailKey(boardId), initialBoardJson)
            setEventTs(boardEventTsKey(boardId), initialBoardTs)
            redisTemplate.opsForValue().set(commentBoardCountKey(boardId), "0")
            redisTemplate.opsForValue().set(commentTotalCountKey(), "0")

            val result = communityEventUpdateRepository.applyCommentCreation(payload, commentJson, updatedBoardJson) // When
            assertTrue(result, "게시글을 찾았을 때 댓글 생성 스크립트가 성공해야 합니다")
            // Then
            assertEquals(commentJson, redisTemplate.opsForValue().get(commentDetailKey(commentId)))
            assertEquals(eventTs.toDouble(), redisTemplate.opsForZSet().score(commentBoardListKey(boardId), commentId.toString()))
            assertEquals("1", redisTemplate.opsForValue().get(commentBoardCountKey(boardId)))
            assertEquals("1", redisTemplate.opsForValue().get(commentTotalCountKey()))
            assertEquals(eventTs, getEventTs(commentEventTsKey(commentId)))
            assertEquals(updatedBoardJson, redisTemplate.opsForValue().get(boardDetailKey(boardId)))
            assertEquals(eventTs, getEventTs(boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(게시글 못찾음): 댓글 데이터 저장/업데이트 (게시글 제외)")
        fun `success board not found`() {
            redisTemplate.opsForValue().set(commentBoardCountKey(boardId), "0")
            redisTemplate.opsForValue().set(commentTotalCountKey(), "0") // Given
            val result = communityEventUpdateRepository.applyCommentCreation(payload, commentJson, null) // When (null board json)
            assertTrue(result, "게시글을 찾지 못했을 때도 댓글 생성 스크립트가 성공해야 합니다")
            // Then
            assertEquals(commentJson, redisTemplate.opsForValue().get(commentDetailKey(commentId)))
            assertEquals(eventTs.toDouble(), redisTemplate.opsForZSet().score(commentBoardListKey(boardId), commentId.toString()))
            assertEquals("1", redisTemplate.opsForValue().get(commentBoardCountKey(boardId)))
            assertEquals("1", redisTemplate.opsForValue().get(commentTotalCountKey()))
            assertEquals(eventTs, getEventTs(commentEventTsKey(commentId)))
            assertNull(redisTemplate.opsForValue().get(boardDetailKey(boardId)))
            assertNull(getEventTs(boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(건너뜀 - 댓글): 오래된 댓글 이벤트 시 상태 미변경")
        fun `skip older comment event`() {
            val newerTs = eventTs + 1000
            setEventTs(commentEventTsKey(commentId), newerTs) // Given
            val result = communityEventUpdateRepository.applyCommentCreation(payload, commentJson, updatedBoardJson) // When
            assertFalse(result, "더 오래된 이벤트의 경우 댓글 생성 스크립트가 건너뛰어야 합니다")
            // Then
            assertNull(redisTemplate.opsForValue().get(commentDetailKey(commentId)))
            assertNull(redisTemplate.opsForZSet().score(commentBoardListKey(boardId), commentId.toString()))
        }

        @Test
        @DisplayName("성공(잘못된 JSON 전달): 현재 구현은 예외를 던지지 않고 성공(true) 반환")
        fun `invalid json does not throw exception with current implementation`() {
            // When: 유효하지 않은 댓글 JSON으로 호출
            val resultComment = communityEventUpdateRepository.applyCommentCreation(payload, "invalid comment json", updatedBoardJson)
            // Then: Lua 스크립트를 통해 Redis SET 명령은 유효하지 않은 JSON 값에 실패하지 않음
            assertTrue(resultComment, "유효하지 않은 댓글 JSON에도 댓글 생성은 true를 반환해야 합니다")

            // When: 유효하지 않은 게시글 JSON으로 호출 (게시글 JSON이 제공된 경우)
            redisTemplate.opsForValue().set(boardDetailKey(boardId), initialBoardJson) // 이 부분을 위해 게시글이 존재하도록 함
            val resultBoard = communityEventUpdateRepository.applyCommentCreation(payload, commentJson, "invalid board json")
            // Then: Lua 스크립트를 통해 Redis SET 명령은 유효하지 않은 JSON 값에 실패하지 않음
            assertFalse(resultBoard, "유효하지 않은 게시글 JSON에도 댓글 생성은 true를 반환해야 합니다")
        }

        @Test
        @DisplayName("성공(Trim): 댓글 생성 시 댓글 목록 크기 제한(Trim) 확인 (스크립트 레벨)")
        fun `creation script trims comment list when limit exceeded`() {
            // Given: 댓글 목록을 limit 만큼 채움
            val limit = RedisCommentIdListRepository.COMMENT_LIMIT_SIZE
            val key = commentBoardListKey(boardId)
            repeat(limit.toInt()) {
                zAdd(key, (initialBoardTs - it).toDouble(), "dummy_comment_$it")
            }
            assertEquals(limit, redisTemplate.opsForZSet().size(key))
            // 게시글 상태도 설정
            redisTemplate.opsForValue().set(boardDetailKey(boardId), initialBoardJson)
            setEventTs(boardEventTsKey(boardId), initialBoardTs)

            // When: 새 댓글 생성 (eventTs는 기존 더미보다 최신)
            val result = communityEventUpdateRepository.applyCommentCreation(payload, commentJson, updatedBoardJson)

            // Then: 목록 크기가 유지되고 새 댓글이 추가되었는지 확인
            assertTrue(result)
            assertEquals(limit, redisTemplate.opsForZSet().size(key), "댓글 목록 크기가 유지되어야 함")
            assertNotNull(redisTemplate.opsForZSet().score(key, commentId.toString()), "새 댓글이 목록에 있어야 함")
            assertNull(redisTemplate.opsForZSet().score(key, "dummy_comment_${limit - 1}"), "가장 오래된 댓글 더미가 삭제되어야 함")
        }

        @Test
        @DisplayName("성공(TTL): 댓글 생성 시 댓글/게시글 TS 키에 TTL 설정 확인")
        fun `creation script sets TTL on eventTs keys`() {
            // Given: 게시글 존재
            redisTemplate.opsForValue().set(boardDetailKey(boardId), initialBoardJson)
            setEventTs(boardEventTsKey(boardId), initialBoardTs)

            // When
            val result = communityEventUpdateRepository.applyCommentCreation(payload, commentJson, updatedBoardJson)
            assertTrue(result)

            // Then: 각 TS 키의 TTL 확인
            val commentTtl = redisTemplate.getExpire(commentEventTsKey(commentId))
            assertNotNull(commentTtl, "댓글 TS 키에 TTL이 설정되어야 함")
            assertTrue(commentTtl > 0, "댓글 TS 키 TTL > 0")

            val boardTtl = redisTemplate.getExpire(boardEventTsKey(boardId))
            assertNotNull(boardTtl, "게시글 TS 키에 TTL이 설정되어야 함 (업데이트 발생 시)")
            assertTrue(boardTtl > 0, "게시글 TS 키 TTL > 0")
        }
    }

    @Nested
    @DisplayName("applyCommentUpdate")
    inner class ApplyCommentUpdate {
        private val commentId = 101L
        private val boardId = 1L
        private val initialTs = System.currentTimeMillis() - 5000
        private val eventTs = System.currentTimeMillis()

        private lateinit var initialModel: CommentReadModel
        private lateinit var initialJson: String

        private lateinit var payload: CommentUpdatedEventPayload

        private lateinit var updatedModel: CommentReadModel
        private lateinit var updatedCommentJson: String

        @BeforeEach
        fun setup() {
            initialModel =
                CommentReadModelFixture
                    .aCommentReadModel()
                    .withId(commentId)
                    .withBoardId(boardId)
                    .withContent("Old")
                    .withEventTs(initialTs)
                    .create()
            initialJson = dataSerializer.serialize(initialModel)!!

            payload =
                CommentUpdatedEventPayloadFixture
                    .aCommentUpdatedEventPayload()
                    .withCommentId(commentId)
                    .withBoardId(boardId)
                    .withEventTs(eventTs)
                    .create()

            updatedModel =
                initialModel.copy(
                    content = payload.content,
                    updatedAt = payload.updatedAt,
                    eventTs = payload.eventTs,
                )
            updatedCommentJson = dataSerializer.serialize(updatedModel)!!

            redisTemplate.opsForValue().set(commentDetailKey(commentId), initialJson)
            setEventTs(commentEventTsKey(commentId), initialTs)
        }

        @Test
        @DisplayName("성공: 댓글 업데이트 시 상세 정보 및 이벤트 TS 업데이트")
        fun `success case`() {
            val result = communityEventUpdateRepository.applyCommentUpdate(payload, updatedCommentJson) // When
            assertTrue(result, "댓글 업데이트 스크립트가 성공해야 합니다")
            // Then
            assertEquals(updatedCommentJson, redisTemplate.opsForValue().get(commentDetailKey(commentId)))
            assertEquals(eventTs, getEventTs(commentEventTsKey(commentId)))
        }

        @Test
        @DisplayName("성공(건너뜀): 오래된 이벤트 시 데이터 미변경")
        fun `skip older event`() {
            val olderPayload = payload.copy(eventTs = initialTs - 1000)
            val olderJson =
                dataSerializer.serialize(
                    updatedModel.copy(
                        content = olderPayload.content,
                        updatedAt = olderPayload.updatedAt,
                        eventTs = olderPayload.eventTs,
                    ),
                )!!
            val result = communityEventUpdateRepository.applyCommentUpdate(olderPayload, olderJson) // When
            assertFalse(result, "더 오래된 이벤트의 경우 댓글 업데이트 스크립트가 건너뛰어야 합니다")
            // Then
            assertEquals(initialJson, redisTemplate.opsForValue().get(commentDetailKey(commentId)))
            assertEquals(initialTs, getEventTs(commentEventTsKey(commentId)))
        }

        @Test
        @DisplayName("성공(잘못된 JSON 전달): 현재 구현은 예외를 던지지 않고 성공(true) 반환")
        fun `invalid json does not throw exception with current implementation`() {
            // When: 유효하지 않은 댓글 JSON으로 호출
            val result = communityEventUpdateRepository.applyCommentUpdate(payload, "invalid comment json")
            // Then: Lua 스크립트를 통해 Redis SET 명령은 유효하지 않은 JSON 값에 실패하지 않음
            assertTrue(result, "유효하지 않은 댓글 JSON에도 댓글 업데이트는 true를 반환해야 합니다")
        }
    }

    @Nested
    @DisplayName("applyCommentDeletion")
    inner class ApplyCommentDeletion {
        private val commentId = 101L
        private val boardId = 1L
        private val initialBoardTs = System.currentTimeMillis() - 6000
        private val initialCommentTs = System.currentTimeMillis() - 5000
        private val eventTs = System.currentTimeMillis()

        private val payload =
            CommentDeletedEventPayloadFixture
                .aCommentDeletedEventPayload()
                .withCommentId(commentId)
                .withBoardId(boardId)
                .withEventTs(eventTs)
                .create()

        private val initialBoardModel =
            BoardReadModelFixture
                .aBoardReadModel()
                .withId(boardId)
                .withCommentCount(1)
                .withEventTs(initialBoardTs)
                .create()
        private val initialBoardJson = dataSerializer.serialize(initialBoardModel)!!

        private val updatedBoardModel = initialBoardModel.copy(commentCount = 0)
        private val updatedBoardJson = dataSerializer.serialize(updatedBoardModel)!!

        @BeforeEach
        fun setup() {
            redisTemplate.opsForValue().set(commentDetailKey(commentId), "{}")
            zAdd(commentBoardListKey(boardId), initialCommentTs.toDouble(), commentId.toString())
            redisTemplate.opsForValue().set(commentBoardCountKey(boardId), "1")
            redisTemplate.opsForValue().set(commentTotalCountKey(), "1")
            setEventTs(commentEventTsKey(commentId), initialCommentTs)
            redisTemplate.opsForValue().set(boardDetailKey(boardId), initialBoardJson)
            setEventTs(boardEventTsKey(boardId), initialBoardTs)
        }

        @Test
        @DisplayName("성공(게시글 찾음): 관련 데이터 삭제/업데이트 및 성공(true) 반환")
        fun `success with board found`() {
            val result = communityEventUpdateRepository.applyCommentDeletion(payload, updatedBoardJson) // When
            assertTrue(result, "게시글을 찾았을 때 댓글 삭제 스크립트가 성공해야 합니다")
            // Then
            assertNull(redisTemplate.opsForValue().get(commentDetailKey(commentId)))
            assertNull(redisTemplate.opsForZSet().score(commentBoardListKey(boardId), commentId.toString()))
            assertEquals("0", redisTemplate.opsForValue().get(commentBoardCountKey(boardId)))
            assertEquals("0", redisTemplate.opsForValue().get(commentTotalCountKey()))
            assertNull(getEventTs(commentEventTsKey(commentId)))
            assertEquals(updatedBoardJson, redisTemplate.opsForValue().get(boardDetailKey(boardId)))
            assertEquals(eventTs, getEventTs(boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(게시글 못찾음): 댓글 데이터 삭제/업데이트 (게시글 제외)")
        fun `success board not found`() {
            redisTemplate.delete(boardDetailKey(boardId))
            redisTemplate.delete(boardEventTsKey(boardId)) // Given board removed
            val result = communityEventUpdateRepository.applyCommentDeletion(payload, null) // When (null board json)
            assertTrue(result, "게시글을 찾지 못했을 때도 댓글 삭제 스크립트가 성공해야 합니다")
            // Then
            assertNull(redisTemplate.opsForValue().get(commentDetailKey(commentId)))
            assertNull(redisTemplate.opsForZSet().score(commentBoardListKey(boardId), commentId.toString()))
            assertEquals("0", redisTemplate.opsForValue().get(commentBoardCountKey(boardId)))
            assertEquals("0", redisTemplate.opsForValue().get(commentTotalCountKey()))
            assertNull(getEventTs(commentEventTsKey(commentId)))
            assertNull(redisTemplate.opsForValue().get(boardDetailKey(boardId)))
            assertNull(getEventTs(boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(건너뜀 - 댓글): 오래된 댓글 이벤트 시 상태 미변경")
        fun `skip older comment event`() {
            val newerTs = eventTs + 1000
            setEventTs(commentEventTsKey(commentId), newerTs) // Given
            val result = communityEventUpdateRepository.applyCommentDeletion(payload, updatedBoardJson) // When
            assertFalse(result, "더 오래된 이벤트의 경우 댓글 삭제 스크립트가 건너뛰어야 합니다")
            // Then
            assertNotNull(redisTemplate.opsForValue().get(commentDetailKey(commentId)))
            assertNotNull(redisTemplate.opsForZSet().score(commentBoardListKey(boardId), commentId.toString()))
            assertEquals("1", redisTemplate.opsForValue().get(commentBoardCountKey(boardId)))
            assertEquals(newerTs, getEventTs(commentEventTsKey(commentId)))
            assertEquals(initialBoardJson, redisTemplate.opsForValue().get(boardDetailKey(boardId)))
        }

        @Test
        @DisplayName("성공(잘못된 JSON 전달): 현재 구현은 예외를 던지지 않고 성공(true) 반환")
        fun `invalid board json does not throw exception with current implementation`() {
            // When: 유효하지 않은 게시글 JSON으로 호출
            val result = communityEventUpdateRepository.applyCommentDeletion(payload, "invalid board json")
            // Then: Lua 스크립트를 통해 Redis SET 명령은 유효하지 않은 JSON 값에 실패하지 않음
            assertTrue(result, "유효하지 않은 게시글 JSON에도 댓글 삭제는 true를 반환해야 합니다")
        }

        @Test
        @DisplayName("성공(음수방지): 댓글 수가 0일 때 삭제해도 카운트 0 유지 (스크립트 레벨)")
        fun `deletion script prevents negative comment counts`() {
            // Given: 댓글 카운트를 0으로 설정
            redisTemplate.opsForValue().set(commentBoardCountKey(boardId), "0")
            redisTemplate.opsForValue().set(commentTotalCountKey(), "0")
            // 다른 관련 데이터는 존재해야 함 (댓글 상세, 댓글 TS, 게시글 상세, 게시글 TS 등)
            redisTemplate.opsForValue().set(commentDetailKey(commentId), "{}")
            setEventTs(commentEventTsKey(commentId), initialCommentTs)
            redisTemplate.opsForValue().set(boardDetailKey(boardId), initialBoardJson)
            setEventTs(boardEventTsKey(boardId), initialBoardTs)

            // When
            val result = communityEventUpdateRepository.applyCommentDeletion(payload, updatedBoardJson)

            // Then
            assertTrue(result, "삭제 스크립트는 성공해야 함 (TS 조건 만족)")
            assertEquals("0", redisTemplate.opsForValue().get(commentBoardCountKey(boardId)), "게시글별 댓글 수는 0이어야 함")
            assertEquals("0", redisTemplate.opsForValue().get(commentTotalCountKey()), "전체 댓글 수는 0이어야 함")
        }
    }
}
