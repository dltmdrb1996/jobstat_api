package com.wildrew.jobstat.community_read.community_read.service

import com.wildrew.jobstat.community_read.community_read.fixture.BoardReadModelFixture
import com.wildrew.jobstat.community_read.community_read.fixture.CommentReadModelFixture
import com.wildrew.jobstat.community_read.community_read.repository.fake.*
import com.wildrew.jobstat.community_read.payload.BoardCreatedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.BoardDeletedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.BoardLikedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.BoardRankingUpdatedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.BoardUpdatedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.BoardViewedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.CommentCreatedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.CommentDeletedEventPayloadFixture
import com.wildrew.jobstat.community_read.payload.CommentUpdatedEventPayloadFixture
import com.wildrew.jobstat.community_read.repository.impl.RedisBoardCountRepository
import com.wildrew.jobstat.community_read.repository.impl.RedisBoardIdListRepository
import com.wildrew.jobstat.community_read.repository.impl.RedisCommentCountRepository
import com.wildrew.jobstat.community_read.repository.impl.RedisCommunityEventUpdateRepository
import com.wildrew.jobstat.community_read.service.CommunityEventHandlerVerLua
import com.wildrew.jobstat.core.core_global.model.BoardRankingMetric
import com.wildrew.jobstat.core.core_global.model.BoardRankingPeriod
import com.wildrew.jobstat.core.core_serializer.DataSerializer
import com.wildrew.jobstat.core.core_serializer.ObjectMapperDataSerializer
import com.wildrew.jobstat.core.core_serializer.config.CoreSerializerAutoConfiguration
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import org.springframework.data.domain.PageRequest

@DisplayName("CommunityEventHandlerVerLua 단위 테스트 (Fakes 사용)")
class CommunityEventHandlerVerLuaTest {
    private lateinit var fakeCommunityEventUpdateRepository: FakeCommunityEventUpdateRepository
    private lateinit var fakeBoardDetailRepository: FakeBoardDetailRepository
    private lateinit var fakeBoardIdListRepository: FakeBoardIdListRepository
    private lateinit var fakeBoardCountRepository: FakeBoardCountRepository
    private lateinit var fakeCommentDetailRepository: FakeCommentDetailRepository
    private lateinit var fakeCommentIdListRepository: FakeCommentIdListRepository
    private lateinit var fakeCommentCountRepository: FakeCommentCountRepository

    private val dataSerializer: DataSerializer =
        ObjectMapperDataSerializer(
            CoreSerializerAutoConfiguration.createDefaultObjectMapper(),
        )

    private lateinit var communityEventHandler: CommunityEventHandlerVerLua

    private val boardId = 1L
    private val commentId = 101L
    private val categoryId = 5L
    private val userId = 123L
    private val eventTs = System.currentTimeMillis()
    private val initialTs = eventTs - 5000

    @BeforeEach
    fun setUp() {
        fakeBoardDetailRepository = FakeBoardDetailRepository(dataSerializer)
        fakeBoardIdListRepository = FakeBoardIdListRepository()
        fakeBoardCountRepository = FakeBoardCountRepository()
        fakeCommentDetailRepository = FakeCommentDetailRepository(dataSerializer)
        fakeCommentIdListRepository = FakeCommentIdListRepository()
        fakeCommentCountRepository = FakeCommentCountRepository()
        fakeCommunityEventUpdateRepository =
            FakeCommunityEventUpdateRepository(
                fakeBoardDetailRepository,
                fakeBoardIdListRepository,
                fakeBoardCountRepository,
                fakeCommentDetailRepository,
                fakeCommentIdListRepository,
                fakeCommentCountRepository,
                dataSerializer,
            )
        communityEventHandler =
            CommunityEventHandlerVerLua(
                fakeCommunityEventUpdateRepository,
                fakeBoardDetailRepository,
                fakeCommentDetailRepository,
                dataSerializer,
            )
    }

    @AfterEach
    fun tearDown() {
        fakeBoardDetailRepository.clear()
        fakeBoardIdListRepository.clear()
        fakeBoardCountRepository.clear()
        fakeCommentDetailRepository.clear()
        fakeCommentIdListRepository.clear()
        fakeCommentCountRepository.clear()
        fakeCommunityEventUpdateRepository.clear()
    }

    @Nested
    @DisplayName("게시글 생성 이벤트 (handleBoardCreated)")
    inner class HandleBoardCreated {
        private val payload =
            BoardCreatedEventPayloadFixture
                .aBoardCreatedEventPayload()
                .withBoardId(boardId)
                .withCategoryId(categoryId)
                .withEventTs(eventTs)
                .create()

        @Test
        @DisplayName("성공: 유효 페이로드 시 Fake 레포지토리 상태 변경")
        fun `success case`() {
            communityEventHandler.handleBoardCreated(payload) // When
            assertNotNull(fakeBoardDetailRepository.findBoardDetail(boardId)) // Then
            assertTrue(fakeBoardIdListRepository.readAllByTimeByOffset(PageRequest.of(0, 10)).content.contains(boardId))
            assertEquals(1L, fakeBoardCountRepository.getTotalCount())
            assertEquals(eventTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(건너뜀): 오래된 이벤트 시 Fake 레포지토리 상태 미변경")
        fun `skip older event`() {
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId), eventTs + 1000) // Given
            communityEventHandler.handleBoardCreated(payload) // When
            assertNull(fakeBoardDetailRepository.findBoardDetail(boardId)) // Then
            assertEquals(0L, fakeBoardCountRepository.getTotalCount())
        }
    }

    @Nested
    @DisplayName("게시글 수정 이벤트 (handleBoardUpdated)")
    inner class HandleBoardUpdated {
        private val initialModel =
            BoardReadModelFixture
                .aBoardReadModel()
                .withId(boardId)
                .withTitle("Old Title")
                .withEventTs(initialTs)
                .create()

        private val payload =
            BoardUpdatedEventPayloadFixture
                .aBoardUpdatedEventPayload()
                .withBoardId(boardId)
                .withEventTs(eventTs)
                .create()

        @BeforeEach
        fun setup() {
            fakeBoardDetailRepository.saveBoardDetail(initialModel, initialTs)
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId), initialTs)
        }

        @Test
        @DisplayName("성공: 게시글 존재 시 Fake 상세 정보 업데이트")
        fun `success case`() {
            communityEventHandler.handleBoardUpdated(payload) // When
            val updated = fakeBoardDetailRepository.findBoardDetail(boardId)
            assertNotNull(updated)
            assertEquals(payload.title, updated?.title)
            assertEquals(payload.content, updated?.content)
            assertEquals(eventTs, updated?.eventTs)
            assertEquals(eventTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("실패(데이터 없음): 수정할 게시글 없으면 상태 변화 없음")
        fun `board not found`() {
            fakeBoardDetailRepository.clear() // Given
            communityEventHandler.handleBoardUpdated(payload) // When
            assertNull(fakeBoardDetailRepository.findBoardDetail(boardId)) // Then
        }

        @Test
        @DisplayName("성공(건너뜀): 오래된 이벤트 시 Fake 상태 미변경")
        fun `skip older event`() {
            val olderPayload = payload.copy(eventTs = initialTs - 1000) // Given
            communityEventHandler.handleBoardUpdated(olderPayload) // When
            val model = fakeBoardDetailRepository.findBoardDetail(boardId)
            assertEquals(initialModel.title, model?.title)
            assertEquals(initialTs, model?.eventTs)
            assertEquals(initialTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("실패(직렬화): 업데이트 모델 직렬화 실패 시 처리 중단")
        fun `serialization failure stops processing`() {
            // Given: Mockito mock 시리얼라이저 사용 (업데이트 모델에 대해 실패)
            val mockSerializer = mock<DataSerializer>()
            val updatedModel =
                initialModel.copy(
                    title = payload.title,
                    content = payload.content,
                    author = payload.author,
                    eventTs = payload.eventTs,
                )
            whenever(mockSerializer.serialize(eq(updatedModel))).thenReturn(null)

            // mock 시리얼라이저로 핸들러 생성
            val handlerWithMock =
                CommunityEventHandlerVerLua(
                    fakeCommunityEventUpdateRepository,
                    fakeBoardDetailRepository,
                    fakeCommentDetailRepository,
                    mockSerializer,
                )

            // When
            handlerWithMock.handleBoardUpdated(payload)

            // Then: 이벤트 TS가 업데이트되지 않았는지, 상세정보가 초기 상태로 유지되는지 확인
            assertEquals(initialTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
            val currentModel = fakeBoardDetailRepository.findBoardDetail(boardId)
            assertEquals(initialModel.title, currentModel?.title)
            assertEquals(initialModel.eventTs, currentModel?.eventTs)
        }
    }

    @Nested
    @DisplayName("게시글 삭제 이벤트 (handleBoardDeleted)")
    inner class HandleBoardDeleted {
        private val payload =
            BoardDeletedEventPayloadFixture
                .aBoardDeletedEventPayload()
                .withBoardId(boardId)
                .withCategoryId(categoryId)
                .withEventTs(eventTs)
                .create()

        @BeforeEach
        fun setup() {
            fakeBoardDetailRepository.saveBoardDetail(
                BoardReadModelFixture
                    .aBoardReadModel()
                    .withId(boardId)
                    .withCategoryId(categoryId)
                    .create(),
                initialTs,
            )
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId), initialTs)
            fakeBoardIdListRepository.internalAdd(
                key = RedisBoardIdListRepository.ALL_BOARDS_KEY,
                score = initialTs.toDouble(),
                member = boardId.toString(),
                limit = RedisBoardIdListRepository.ALL_BOARD_LIMIT_SIZE,
            )
            fakeBoardIdListRepository.internalAdd(
                key = RedisBoardIdListRepository.getCategoryKey(categoryId),
                score = initialTs.toDouble(),
                member = boardId.toString(),
                limit = RedisBoardIdListRepository.CATEGORY_LIMIT_SIZE,
            )
            fakeBoardCountRepository.setCount(RedisBoardCountRepository.BOARD_TOTAL_COUNT_KEY, 1L)
        }

        @Test
        @DisplayName("성공: 게시글 삭제 시 Fake 상태 반영")
        fun `success case`() {
            communityEventHandler.handleBoardDeleted(payload) // When
            assertNull(fakeBoardDetailRepository.findBoardDetail(boardId))
            assertTrue(fakeBoardIdListRepository.readAllByTimeByOffset(PageRequest.of(0, 10)).content.isEmpty())
            assertEquals(0L, fakeBoardCountRepository.getTotalCount())
            assertNull(fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(건너뜀): 오래된 삭제 이벤트 시 상태 미변경")
        fun `skip older event`() {
            val olderPayload = payload.copy(eventTs = initialTs - 1000) // Given older payload
            communityEventHandler.handleBoardDeleted(olderPayload) // When
            assertNotNull(fakeBoardDetailRepository.findBoardDetail(boardId))
            assertEquals(initialTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
            assertEquals(1L, fakeBoardCountRepository.getTotalCount())
        }
    }

    @Nested
    @DisplayName("게시글 좋아요 이벤트 (handleBoardLiked)")
    inner class HandleBoardLiked {
        private val initialModel =
            BoardReadModelFixture
                .aBoardReadModel()
                .withId(boardId)
                .withLikeCount(5)
                .withEventTs(initialTs)
                .create()

        private val payload =
            BoardLikedEventPayloadFixture
                .aBoardLikedEventPayload()
                .withBoardId(boardId)
                .withLikeCount(6)
                .withEventTs(eventTs)
                .create()

        @BeforeEach
        fun setup() {
            fakeBoardDetailRepository.saveBoardDetail(initialModel, initialTs)
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId), initialTs)
        }

        @Test
        @DisplayName("성공: 게시글 존재 시 좋아요 수 업데이트")
        fun `success case`() {
            communityEventHandler.handleBoardLiked(payload) // When
            val model = fakeBoardDetailRepository.findBoardDetail(boardId)
            assertNotNull(model)
            assertEquals(payload.likeCount, model?.likeCount)
            assertEquals(eventTs, model?.eventTs)
            assertEquals(eventTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("실패(데이터 없음): 게시글 없으면 변화 없음")
        fun `board not found`() {
            fakeBoardDetailRepository.clear() // Given no board
            communityEventHandler.handleBoardLiked(payload) // When
            assertNull(fakeBoardDetailRepository.findBoardDetail(boardId)) // Then still null
        }

        @Test
        @DisplayName("성공(건너뜀): 오래된 이벤트 시 상태 미변경")
        fun `skip older event`() {
            val olderPayload = payload.copy(eventTs = initialTs - 1000) // Given older payload
            communityEventHandler.handleBoardLiked(olderPayload) // When
            val model = fakeBoardDetailRepository.findBoardDetail(boardId)
            assertEquals(initialModel.likeCount, model?.likeCount)
            assertEquals(initialTs, model?.eventTs)
            assertEquals(initialTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
        }
    }

    @Nested
    @DisplayName("게시글 조회수 이벤트 (handleBoardViewed)")
    inner class HandleBoardViewed {
        private val initialModel =
            BoardReadModelFixture
                .aBoardReadModel()
                .withId(boardId)
                .withViewCount(10)
                .withEventTs(initialTs)
                .create()

        private val payload =
            BoardViewedEventPayloadFixture
                .aBoardViewedEventPayload()
                .withBoardId(boardId)
                .withViewCount(11)
                .withEventTs(eventTs)
                .create()

        @BeforeEach
        fun setup() {
            fakeBoardDetailRepository.saveBoardDetail(initialModel, initialTs)
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId), initialTs)
        }

        @Test
        @DisplayName("성공: 게시글 존재 시 조회수 업데이트")
        fun `success case`() {
            communityEventHandler.handleBoardViewed(payload) // When
            val model = fakeBoardDetailRepository.findBoardDetail(boardId)
            assertNotNull(model)
            assertEquals(payload.viewCount, model?.viewCount)
            assertEquals(eventTs, model?.eventTs)
            assertEquals(eventTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("실패(데이터 없음): 게시글 없으면 변화 없음")
        fun `board not found`() {
            fakeBoardDetailRepository.clear() // Given no board
            communityEventHandler.handleBoardViewed(payload) // When
            assertNull(fakeBoardDetailRepository.findBoardDetail(boardId)) // Then still null
        }

        @Test
        @DisplayName("성공(건너뜀): 오래된 이벤트 시 상태 미변경")
        fun `skip older event`() {
            val olderPayload = payload.copy(eventTs = initialTs - 1000) // Given older payload
            communityEventHandler.handleBoardViewed(olderPayload) // When
            val model = fakeBoardDetailRepository.findBoardDetail(boardId)
            assertEquals(initialModel.viewCount, model?.viewCount)
            assertEquals(initialTs, model?.eventTs)
            assertEquals(initialTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
        }
    }

    @Nested
    @DisplayName("게시글 랭킹 업데이트 이벤트 (handleBoardRankingUpdated)")
    inner class HandleBoardRankingUpdated {
        private val metric = BoardRankingMetric.LIKES
        private val period = BoardRankingPeriod.WEEK
        private val rankings =
            listOf(
                com.wildrew.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload
                    .RankingEntry(10L, 500.0),
            )

        private val payload =
            BoardRankingUpdatedEventPayloadFixture
                .aBoardRankingUpdatedEventPayload()
                .withMetric(metric)
                .withPeriod(period)
                .withRankings(rankings)
                .withEventTs(eventTs)
                .create()
        private val key = RedisBoardIdListRepository.getRankingKey(metric, period)!!
        private val tsKey = RedisCommunityEventUpdateRepository.rankingEventTsKey(metric.name.lowercase(), period.name.lowercase())

        @Test
        @DisplayName("성공: 유효 페이로드 시 Fake 랭킹 목록 교체")
        fun `success case`() {
            fakeCommunityEventUpdateRepository.setEventTs(tsKey, initialTs)
            communityEventHandler.handleBoardRankingUpdated(payload)
            val map = fakeBoardIdListRepository.getMapForKey(key)
            assertEquals(1, map.size)
            assertEquals("10", map[500.0])
            assertEquals(eventTs, fakeCommunityEventUpdateRepository.getEventTs(tsKey))
        }

        @Test
        @DisplayName("성공(건너뜀): 오래된 이벤트 시 상태 미변경")
        fun `skip older event`() {
            val newerTs = eventTs + 1000
            fakeCommunityEventUpdateRepository.setEventTs(tsKey, newerTs)
            communityEventHandler.handleBoardRankingUpdated(payload)
            assertTrue(fakeBoardIdListRepository.getMapForKey(key).isEmpty())
            assertEquals(newerTs, fakeCommunityEventUpdateRepository.getEventTs(tsKey))
        }
    }

    @Nested
    @DisplayName("댓글 생성 이벤트 (handleCommentCreated)")
    inner class HandleCommentCreated {
        private val payload =
            CommentCreatedEventPayloadFixture
                .aCommentCreatedEventPayload()
                .withCommentId(commentId)
                .withBoardId(boardId)
                .withEventTs(eventTs)
                .create()

        private val initialBoardModel =
            BoardReadModelFixture
                .aBoardReadModel()
                .withId(boardId)
                .withCommentCount(0)
                .withEventTs(initialTs)
                .create()

        @Test
        @DisplayName("성공(게시글 찾음): 댓글 생성 시 Fake 상태 반영 (댓글+게시글)")
        fun `success with board found`() {
            fakeBoardDetailRepository.saveBoardDetail(initialBoardModel, initialTs)
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId), initialTs)
            fakeCommentCountRepository.setCount(RedisCommentCountRepository.getBoardCommentCountKey(boardId), 0L)
            fakeCommentCountRepository.setCount(RedisCommentCountRepository.getTotalCommentCountKey(), 0L)

            communityEventHandler.handleCommentCreated(payload)

            assertNotNull(fakeCommentDetailRepository.findCommentDetail(commentId))
            assertTrue(fakeCommentIdListRepository.getMapForBoard(boardId).containsValue(commentId.toString()))
            assertEquals(1L, fakeCommentCountRepository.getCommentCountByBoardId(boardId))
            assertEquals(1L, fakeCommentCountRepository.getTotalCount())
            assertEquals(eventTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.commentEventTsKey(commentId)))

            val board = fakeBoardDetailRepository.findBoardDetail(boardId)
            assertNotNull(board)
            assertEquals(1, board?.commentCount)
            assertEquals(eventTs, board?.eventTs)
            assertEquals(eventTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(게시글 못찾음): 댓글 생성 시 Fake 상태 반영 (댓글만)")
        fun `success board not found`() {
            fakeCommentCountRepository.setCount(RedisCommentCountRepository.getBoardCommentCountKey(boardId), 0L)
            fakeCommentCountRepository.setCount(RedisCommentCountRepository.getTotalCommentCountKey(), 0L)

            communityEventHandler.handleCommentCreated(payload)

            assertNotNull(fakeCommentDetailRepository.findCommentDetail(commentId))
            assertTrue(fakeCommentIdListRepository.getMapForBoard(boardId).containsValue(commentId.toString()))
            assertEquals(1L, fakeCommentCountRepository.getCommentCountByBoardId(boardId))
            assertEquals(1L, fakeCommentCountRepository.getTotalCount())
            assertEquals(eventTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.commentEventTsKey(commentId)))

            assertNull(fakeBoardDetailRepository.findBoardDetail(boardId))
            assertNull(fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(건너뜀 - 댓글): 오래된 댓글 이벤트 시 상태 미변경")
        fun `skip older comment event`() {
            val newerTs = eventTs + 1000
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.commentEventTsKey(commentId), newerTs) // Given newer comment TS

            communityEventHandler.handleCommentCreated(payload) // When

            // Then check comment state
            assertNull(fakeCommentDetailRepository.findCommentDetail(commentId))
            assertEquals(0L, fakeCommentCountRepository.getTotalCount())
            assertEquals(0L, fakeCommentCountRepository.getCommentCountByBoardId(boardId))
            assertFalse(fakeCommentIdListRepository.getMapForBoard(boardId).containsValue(commentId.toString()))
            assertEquals(newerTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.commentEventTsKey(commentId)))
        }

        @Test
        @DisplayName("성공(게시글 건너뜀): 댓글 최신, 게시글 오래됨 시 댓글만 반영, 게시글 미반영")
        fun `skip board update if board TS is newer`() {
            val newerBoardTs = eventTs + 1000
            // Given board with newer TS and initial counts
            fakeBoardDetailRepository.saveBoardDetail(initialBoardModel.copy(eventTs = newerBoardTs), newerBoardTs)
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId), newerBoardTs)
            fakeCommentCountRepository.setCount(RedisCommentCountRepository.getBoardCommentCountKey(boardId), 0L)
            fakeCommentCountRepository.setCount(RedisCommentCountRepository.getTotalCommentCountKey(), 0L)

            communityEventHandler.handleCommentCreated(payload) // When

            // Then check comment state
            assertNotNull(fakeCommentDetailRepository.findCommentDetail(commentId))
            assertEquals(1L, fakeCommentCountRepository.getTotalCount())
            assertEquals(eventTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.commentEventTsKey(commentId)))

            // Then check board state
            val board = fakeBoardDetailRepository.findBoardDetail(boardId)
            assertNotNull(board)
            assertEquals(0, board?.commentCount)
            assertEquals(newerBoardTs, board?.eventTs)
            assertEquals(newerBoardTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
        }
    }

    @Nested
    @DisplayName("댓글 수정 이벤트 (handleCommentUpdated)")
    inner class HandleCommentUpdated {
        private val initialCommentModel =
            CommentReadModelFixture
                .aCommentReadModel()
                .withId(commentId)
                .withBoardId(boardId)
                .withContent("Old")
                .withEventTs(initialTs)
                .create()

        private val payload =
            CommentUpdatedEventPayloadFixture
                .aCommentUpdatedEventPayload()
                .withCommentId(commentId)
                .withBoardId(boardId)
                .withEventTs(eventTs)
                .create()

        @BeforeEach
        fun setup() {
            fakeCommentDetailRepository.saveCommentDetail(initialCommentModel, initialTs)
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.commentEventTsKey(commentId), initialTs)
        }

        @Test
        @DisplayName("성공: 댓글 존재 시 Fake 상세 정보 업데이트")
        fun `success case`() {
            communityEventHandler.handleCommentUpdated(payload) // When
            val model = fakeCommentDetailRepository.findCommentDetail(commentId)
            assertNotNull(model)
            assertEquals(payload.content, model?.content)
            assertEquals(payload.updatedAt, model?.updatedAt)
            assertEquals(eventTs, model?.eventTs)
            assertEquals(eventTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.commentEventTsKey(commentId)))
        }

        @Test
        @DisplayName("실패(데이터 없음): 수정할 댓글 없으면 상태 변화 없음")
        fun `comment not found`() {
            fakeCommentDetailRepository.clear() // Given no comment
            communityEventHandler.handleCommentUpdated(payload) // When
            assertNull(fakeCommentDetailRepository.findCommentDetail(commentId)) // Then still null
        }

        @Test
        @DisplayName("성공(건너뜀): 오래된 이벤트 시 Fake 상태 미변경")
        fun `skip older event`() {
            val olderPayload = payload.copy(eventTs = initialTs - 1000) // Given older payload
            communityEventHandler.handleCommentUpdated(olderPayload) // When
            val model = fakeCommentDetailRepository.findCommentDetail(commentId)
            assertEquals(initialCommentModel.content, model?.content)
            assertEquals(initialCommentModel.updatedAt, model?.updatedAt)
            assertEquals(initialTs, model?.eventTs)
            assertEquals(initialTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.commentEventTsKey(commentId)))
        }
    }

    @Nested
    @DisplayName("댓글 삭제 이벤트 (handleCommentDeleted)")
    inner class HandleCommentDeleted {
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
                .withEventTs(initialTs)
                .create()

        private val initialCommentModel =
            CommentReadModelFixture
                .aCommentReadModel()
                .withId(commentId)
                .withBoardId(boardId)
                .withEventTs(initialTs)
                .create()

        @BeforeEach
        fun setup() {
            // 초기 댓글 상태 설정
            fakeCommentDetailRepository.saveCommentDetail(initialCommentModel, initialTs)
            fakeCommentIdListRepository.internalAdd(boardId, commentId, initialTs.toDouble())
            fakeCommentCountRepository.setCount(RedisCommentCountRepository.getBoardCommentCountKey(boardId), 1L)
            fakeCommentCountRepository.setCount(RedisCommentCountRepository.getTotalCommentCountKey(), 1L)
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.commentEventTsKey(commentId), initialTs)
            // 초기 게시글 상태 설정
            fakeBoardDetailRepository.saveBoardDetail(initialBoardModel, initialTs)
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId), initialTs)
        }

        @Test
        @DisplayName("성공(게시글 찾음): 댓글 삭제 시 Fake 상태 반영 (댓글+게시글)")
        fun `success with board found`() {
            communityEventHandler.handleCommentDeleted(payload) // When

            // Then check comment state
            assertNull(fakeCommentDetailRepository.findCommentDetail(commentId))
            assertTrue(fakeCommentIdListRepository.getMapForBoard(boardId).isEmpty())
            assertEquals(0L, fakeCommentCountRepository.getCommentCountByBoardId(boardId))
            assertEquals(0L, fakeCommentCountRepository.getTotalCount())
            assertNull(fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.commentEventTsKey(commentId)))

            // Then check board state
            val board = fakeBoardDetailRepository.findBoardDetail(boardId)
            assertNotNull(board)
            assertEquals(0, board?.commentCount)
            assertEquals(eventTs, board?.eventTs)
            assertEquals(eventTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
        }

        @Test
        @DisplayName("성공(게시글 못찾음): 댓글 삭제 시 Fake 상태 반영 (댓글만)")
        fun `success board not found`() {
            // Given board gone
            fakeBoardDetailRepository.clear()
            fakeCommunityEventUpdateRepository.eventTimestamps.remove(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId))

            communityEventHandler.handleCommentDeleted(payload) // When

            // Then check comment state
            assertNull(fakeCommentDetailRepository.findCommentDetail(commentId))
            assertTrue(fakeCommentIdListRepository.getMapForBoard(boardId).isEmpty())
            assertEquals(0L, fakeCommentCountRepository.getCommentCountByBoardId(boardId))
            assertEquals(0L, fakeCommentCountRepository.getTotalCount())
            assertNull(fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.commentEventTsKey(commentId)))

            // Then check board state
            assertNull(fakeBoardDetailRepository.findBoardDetail(boardId))
        }

        @Test
        @DisplayName("성공(건너뜀 - 댓글): 오래된 댓글 이벤트 시 상태 미변경")
        fun `skip older comment event`() {
            val newerTs = eventTs + 1000
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.commentEventTsKey(commentId), newerTs) // Given newer comment TS

            communityEventHandler.handleCommentDeleted(payload) // When

            // Then check comment state
            assertNotNull(fakeCommentDetailRepository.findCommentDetail(commentId))
            assertFalse(fakeCommentIdListRepository.getMapForBoard(boardId).isEmpty())
            assertEquals(1L, fakeCommentCountRepository.getCommentCountByBoardId(boardId))
            assertEquals(1L, fakeCommentCountRepository.getTotalCount())
            assertEquals(newerTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.commentEventTsKey(commentId)))

            // Then check board state
            val board = fakeBoardDetailRepository.findBoardDetail(boardId)
            assertEquals(initialBoardModel.commentCount, board?.commentCount)
            assertEquals(initialTs, board?.eventTs)
            assertEquals(initialTs, fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)))
        }
    }

    @Nested
    @DisplayName("이벤트 순서 역전 시나리오 (TS 기반 처리)")
    inner class EventOutOfOrderProcessing {
        private val initialTs = System.currentTimeMillis()
        private val boardUpdateTs = initialTs + 1000
        private val commentCreateTs = initialTs + 2000

        private val boardUpdatePayload =
            BoardUpdatedEventPayloadFixture
                .aBoardUpdatedEventPayload()
                .withBoardId(boardId)
                .withTitle("Updated Title at T=9")
                .withEventTs(boardUpdateTs)
                .create()

        private val commentCreatePayload =
            CommentCreatedEventPayloadFixture
                .aCommentCreatedEventPayload()
                .withBoardId(boardId)
                .withCommentId(commentId)
                .withEventTs(commentCreateTs)
                .create()

        @Test
        @DisplayName("성공(건너뜀): 최신 이벤트(댓글) 처리 후 과거 이벤트(게시글 수정) 도착 시 과거 이벤트 무시")
        fun `skip older board update event after processing newer comment event`() {
            // Given: 초기 상태 설정
            val initialBoard =
                BoardReadModelFixture
                    .aBoardReadModel()
                    .withId(boardId)
                    .withTitle("Initial Title")
                    .withCommentCount(0)
                    .withEventTs(initialTs)
                    .create()
            fakeBoardDetailRepository.saveBoardDetail(initialBoard, initialTs)
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId), initialTs)
            fakeCommentCountRepository.setCount(RedisCommentCountRepository.getBoardCommentCountKey(boardId), 0L)
            fakeCommentCountRepository.setCount(RedisCommentCountRepository.getTotalCommentCountKey(), 0L)

            // When: 이벤트 처리 순서 시뮬레이션
            communityEventHandler.handleCommentCreated(commentCreatePayload) // T=10분 댓글 이벤트 처리
            communityEventHandler.handleBoardUpdated(boardUpdatePayload) // T=9분 게시글 수정 이벤트 처리 시도

            // Then: 최종 상태 검증
            val finalBoard = fakeBoardDetailRepository.findBoardDetail(boardId)
            assertNotNull(finalBoard)

            assertEquals(1, finalBoard?.commentCount, "최신 이벤트인 댓글 생성은 반영되어야 함")
            assertEquals(commentCreateTs, finalBoard?.eventTs, "게시글의 최종 eventTs는 최신 이벤트의 시간이어야 함")
            assertNotEquals(boardUpdatePayload.title, finalBoard?.title, "과거 이벤트인 게시글 제목 업데이트는 무시되어야 함")
            assertEquals(initialBoard.title, finalBoard?.title, "게시글 제목은 초기 상태를 유지해야 함")
            assertEquals(
                commentCreateTs,
                fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId)),
                "게시글 타임스탬프 추적기는 최신 이벤트 시간으로 업데이트 되어야 함",
            )
            assertNotNull(fakeCommentDetailRepository.findCommentDetail(commentId))
            assertEquals(1L, fakeCommentCountRepository.getCommentCountByBoardId(boardId))
        }

        @Test
        @DisplayName("성공(건너뜀): 최신 좋아요 처리 후 과거 게시글 수정 이벤트 도착 시 수정 무시")
        fun `skipOlderBoardUpdateAfterNewerLike`() {
            // Given: 시간 설정 및 페이로드 준비
            val initialTs = System.currentTimeMillis()
            val likeEventTs = initialTs + 1000
            val updateEventTs = initialTs
            val initialLikeCount = 5
            val newLikeCount = initialLikeCount + 1
            val oldUpdateTitle = "Old Title Update T_Update"

            val initialBoard =
                BoardReadModelFixture
                    .aBoardReadModel()
                    .withId(boardId)
                    .withLikeCount(initialLikeCount)
                    .withEventTs(initialTs)
                    .create()
            fakeBoardDetailRepository.saveBoardDetail(initialBoard, initialTs)
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId), initialTs)

            val likePayload =
                BoardLikedEventPayloadFixture
                    .aBoardLikedEventPayload()
                    .withBoardId(boardId)
                    .withLikeCount(newLikeCount)
                    .withEventTs(likeEventTs)
                    .create()
            val updatePayload =
                BoardUpdatedEventPayloadFixture
                    .aBoardUpdatedEventPayload()
                    .withBoardId(boardId)
                    .withTitle(oldUpdateTitle)
                    .withEventTs(updateEventTs)
                    .create()

            // When: 최신(좋아요) 이벤트 먼저 처리 후 과거(수정) 이벤트 처리 시도
            communityEventHandler.handleBoardLiked(likePayload)
            communityEventHandler.handleBoardUpdated(updatePayload)

            // Then: 최종 상태 검증
            val finalBoard = fakeBoardDetailRepository.findBoardDetail(boardId)
            val finalBoardTs = fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId))

            assertNotNull(finalBoard)
            assertEquals(newLikeCount, finalBoard?.likeCount, "좋아요 수는 최신 이벤트 기준으로 업데이트되어야 함")
            assertNotEquals(oldUpdateTitle, finalBoard?.title, "게시글 제목은 과거 이벤트 기준으로 업데이트되지 않아야 함")
            assertEquals(likeEventTs, finalBoard?.eventTs, "게시글 eventTs는 최신 좋아요 이벤트 시간이어야 함")
            assertEquals(likeEventTs, finalBoardTs, "게시글 타임스탬프 추적기는 최신 좋아요 이벤트 시간이어야 함")
        }

        @Test
        @DisplayName("성공(게시글 미반영): 최신 게시글 수정 처리 후 과거 댓글 생성 이벤트 도착 시 댓글만 생성")
        fun `createCommentOnlyWhenBoardIsNewer`() {
            // Given: 시간 설정 및 페이로드 준비
            val initialTs = System.currentTimeMillis()
            val updateEventTs = initialTs + 1000
            val commentEventTs = initialTs
            val newUpdateTitle = "New Title T_Update"
            val initialCommentCount = 0

            val initialBoard =
                BoardReadModelFixture
                    .aBoardReadModel()
                    .withId(boardId)
                    .withTitle("Initial")
                    .withCommentCount(initialCommentCount)
                    .withEventTs(initialTs)
                    .create()
            fakeBoardDetailRepository.saveBoardDetail(initialBoard, initialTs)
            fakeCommunityEventUpdateRepository.setEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId), initialTs)

            val updatePayload =
                BoardUpdatedEventPayloadFixture
                    .aBoardUpdatedEventPayload()
                    .withBoardId(boardId)
                    .withTitle(newUpdateTitle)
                    .withEventTs(updateEventTs)
                    .create()
            val commentPayload =
                CommentCreatedEventPayloadFixture
                    .aCommentCreatedEventPayload()
                    .withBoardId(boardId)
                    .withCommentId(commentId)
                    .withEventTs(commentEventTs)
                    .create()

            // When: 최신(수정) 이벤트 먼저 처리 후 과거(댓글) 이벤트 처리 시도
            communityEventHandler.handleBoardUpdated(updatePayload)
            communityEventHandler.handleCommentCreated(commentPayload)

            // Then: 최종 상태 검증
            val finalBoard = fakeBoardDetailRepository.findBoardDetail(boardId)
            val finalBoardTs = fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId))
            val createdComment = fakeCommentDetailRepository.findCommentDetail(commentId)

            assertNotNull(createdComment, "댓글은 생성되어야 함")
            assertEquals(1, fakeCommentCountRepository.getCommentCountByBoardId(boardId), "게시글별 댓글 카운트는 증가해야 함 (별도 관리)")

            assertNotNull(finalBoard)
            assertEquals(newUpdateTitle, finalBoard?.title, "게시글 제목은 최신 수정 이벤트 기준으로 유지되어야 함")
            assertEquals(updateEventTs, finalBoard?.eventTs, "게시글 eventTs는 최신 수정 이벤트 시간이어야 함")
            assertEquals(initialCommentCount, finalBoard?.commentCount, "게시글 모델 내 댓글 수는 과거 이벤트로 인해 변경되지 않아야 함")
            assertEquals(updateEventTs, finalBoardTs, "게시글 타임스탬프 추적기는 최신 수정 이벤트 시간이어야 함")
        }
    }

    @Nested
    @DisplayName("복합 이벤트 순차 처리")
    inner class SequentialEventProcessing {
        @Test
        @DisplayName("성공: 여러 이벤트 순차 처리 시 최종 상태 및 TS 일관성 검증")
        fun `sequentialEventsMaintainConsistency`() {
            // Given: 시간 순서대로 타임스탬프 정의
            val baseTs = System.currentTimeMillis()
            val t1 = baseTs // Board Created
            val t2 = baseTs + 100 // Board Liked (Like Count = 1)
            val t3 = baseTs + 200 // Comment Created
            val t4 = baseTs + 300 // Board Updated (Title)
            val t5 = baseTs + 400 // Comment Deleted
            val t6 = baseTs + 500 // Board Liked (Like Count = 0, 즉 Unlike)

            val newTitleT4 = "Updated Title at T4"
            val commentIdT3 = commentId + 1

            // 이벤트 페이로드 생성
            val boardCreatePayload =
                BoardCreatedEventPayloadFixture
                    .aBoardCreatedEventPayload()
                    .withBoardId(boardId)
                    .withCategoryId(categoryId)
                    .withEventTs(t1)
                    .create()
            val boardLikePayload1 =
                BoardLikedEventPayloadFixture
                    .aBoardLikedEventPayload()
                    .withBoardId(boardId)
                    .withLikeCount(1)
                    .withEventTs(t2)
                    .create()
            val commentCreatePayload =
                CommentCreatedEventPayloadFixture
                    .aCommentCreatedEventPayload()
                    .withBoardId(boardId)
                    .withCommentId(commentIdT3)
                    .withEventTs(t3)
                    .create()
            val boardUpdatePayload =
                BoardUpdatedEventPayloadFixture
                    .aBoardUpdatedEventPayload()
                    .withBoardId(boardId)
                    .withTitle(newTitleT4)
                    .withContent("Content at T4")
                    .withEventTs(t4)
                    .create()
            val commentDeletePayload =
                CommentDeletedEventPayloadFixture
                    .aCommentDeletedEventPayload()
                    .withBoardId(boardId)
                    .withCommentId(commentIdT3)
                    .withEventTs(t5)
                    .create()
            val boardLikePayload2 =
                BoardLikedEventPayloadFixture
                    .aBoardLikedEventPayload()
                    .withBoardId(boardId)
                    .withLikeCount(0)
                    .withEventTs(t6)
                    .create()

            // When: 이벤트 핸들러 순차 호출
            communityEventHandler.handleBoardCreated(boardCreatePayload) // T1
            communityEventHandler.handleBoardLiked(boardLikePayload1) // T2
            communityEventHandler.handleCommentCreated(commentCreatePayload) // T3
            communityEventHandler.handleBoardUpdated(boardUpdatePayload) // T4
            communityEventHandler.handleCommentDeleted(commentDeletePayload) // T5
            communityEventHandler.handleBoardLiked(boardLikePayload2) // T6

            // Then: 최종 상태 검증
            val finalBoard = fakeBoardDetailRepository.findBoardDetail(boardId)
            val finalBoardTs = fakeCommunityEventUpdateRepository.getEventTs(RedisCommunityEventUpdateRepository.boardEventTsKey(boardId))

            assertNotNull(finalBoard, "최종 게시글 상태는 null이 아니어야 합니다.")
            finalBoard?.apply {
                assertEquals(newTitleT4, title, "최종 제목은 T4의 제목이어야 합니다.")
                assertEquals(0, likeCount, "최종 좋아요 수는 0이어야 합니다 (T2에서 +1, T6에서 0).")
                assertEquals(0, commentCount, "최종 댓글 수는 0이어야 합니다 (T3에서 +1, T5에서 삭제).")
                assertEquals(t6, eventTs, "최종 BoardReadModel의 eventTs는 마지막 이벤트 시간(T6)이어야 합니다.")
            }
            assertEquals(t6, finalBoardTs, "게시글 타임스탬프 추적기는 마지막 이벤트 시간(T6)이어야 합니다.")
            assertNull(fakeCommentDetailRepository.findCommentDetail(commentIdT3), "T5에서 삭제된 댓글은 조회되지 않아야 합니다.")
        }
    }
}
