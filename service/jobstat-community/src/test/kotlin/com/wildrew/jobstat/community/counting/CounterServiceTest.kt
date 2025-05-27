package com.wildrew.jobstat.community.counting

import com.wildrew.jobstat.community.board.fixture.BoardFixture
import com.wildrew.jobstat.community.board.fixture.CategoryFixture
import com.wildrew.jobstat.community.board.repository.FakeBoardRepository
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.*
import java.util.concurrent.ConcurrentHashMap

@DisplayName("CounterService 단위/통합 테스트 (Mockito)")
class CounterServiceTest {
    private lateinit var counterService: CounterService
    private lateinit var fakeCounterRepository: FakeCounterRepository
    private lateinit var fakeBoardRepository: FakeBoardRepository
    private lateinit var mockCounterBatchService: CounterBatchService

    private val testBoardId1 = 1L
    private val testBoardId2 = 2L
    private val testBoardId3 = 3L
    private val testUserId1 = "user:101"
    private val testUserId2 = "user:102"
    private val maxRetry = 2

    @BeforeEach
    fun setUp() {
        fakeCounterRepository = FakeCounterRepository()
        fakeBoardRepository = FakeBoardRepository()
        mockCounterBatchService = mock<CounterBatchService>()

        counterService =
            CounterService(
                counterRepository = fakeCounterRepository,
                boardRepository = fakeBoardRepository,
                counterBatchService = mockCounterBatchService,
                maxRetryCount = maxRetry,
            )

        val dafaultCategory = CategoryFixture.aCategory().create()
        val board1 =
            BoardFixture
                .aBoard()
                .withId(testBoardId1)
                .withCategory(dafaultCategory)
                .create()
        board1.reflectivelySetField("viewCount", 100)
        board1.reflectivelySetField("likeCount", 10)
        fakeBoardRepository.save(board1)

        val board2 =
            BoardFixture
                .aBoard()
                .withId(testBoardId2)
                .withCategory(dafaultCategory)
                .create()
        board2.reflectivelySetField("viewCount", 50)
        board2.reflectivelySetField("likeCount", 5)
        fakeBoardRepository.save(board2)

        val board3 =
            BoardFixture
                .aBoard()
                .withId(testBoardId3)
                .withCategory(dafaultCategory)
                .create()
        board3.reflectivelySetField("viewCount", 0)
        board3.reflectivelySetField("likeCount", 0)
        fakeBoardRepository.save(board3)
    }

    @AfterEach
    fun tearDown() {
        fakeCounterRepository.clear()
        fakeBoardRepository.clear()
    }

    private fun Any.reflectivelySetField(
        fieldName: String,
        value: Any,
    ) {
        try {
            this::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
                set(this@reflectivelySetField, value)
            }
        } catch (e: NoSuchFieldException) {
            var superClass = this::class.java.superclass
            while (superClass != null) {
                try {
                    superClass.getDeclaredField(fieldName).apply {
                        isAccessible = true
                        set(this@reflectivelySetField, value)
                    }
                    return
                } catch (e2: NoSuchFieldException) {
                    superClass = superClass.superclass
                }
            }
            throw e
        }
    }

    private fun Any.reflectivelyGetField(fieldName: String): Any? {
        return try {
            this::class.java
                .getDeclaredField(fieldName)
                .apply {
                    isAccessible = true
                }.get(this)
        } catch (e: NoSuchFieldException) {
            var superClass = this::class.java.superclass
            while (superClass != null) {
                try {
                    return superClass
                        .getDeclaredField(fieldName)
                        .apply {
                            isAccessible = true
                        }.get(this)
                } catch (e2: NoSuchFieldException) {
                    superClass = superClass.superclass
                }
            }
            throw e
        }
    }

    @Nested
    @DisplayName("조회수 증가 (incrementViewCount)")
    inner class IncrementViewCount {
        @Test
        @DisplayName("성공: 조회수 증가 시 Redis 카운터 증가 및 Pending 목록 추가")
        fun `given boardId, when incrementViewCount, then calls repo and adds to pending`() {
            val boardId = testBoardId1

            counterService.incrementViewCount(boardId)

            val redisCounters = fakeCounterRepository.getSingleBoardCountersFromRedis(boardId, null)
            assertEquals(1, redisCounters.viewCount, "Redis 카운트 증가")
            assertTrue(fakeCounterRepository.fetchPendingBoardIds().contains(boardId.toString()))
        }

        @Test
        @DisplayName("성공: 여러 번 호출 시 조회수 누적 증가")
        fun `given boardId, when incrementViewCount called multiple times, then count accumulates`() {
            val boardId = testBoardId1

            counterService.incrementViewCount(boardId)
            counterService.incrementViewCount(boardId)
            counterService.incrementViewCount(boardId)

            val redisCounters = fakeCounterRepository.getSingleBoardCountersFromRedis(boardId, null)
            assertEquals(3, redisCounters.viewCount)
            assertTrue(fakeCounterRepository.fetchPendingBoardIds().contains(boardId.toString()))
        }

        @Test
        @DisplayName("실패: Redis 작업 실패 시 예외 발생")
        fun `given repo throws exception, when incrementViewCount, then throws exception`() {
            val boardId = testBoardId1
            val expectedException = RuntimeException("redis 에러")
            val failingRepo = spy(fakeCounterRepository)

            assertNotNull(boardId)
            doThrow(expectedException).whenever(failingRepo).atomicIncrementViewCountAndAddPending(eq(boardId))
            val serviceWithFailingRepo = CounterService(failingRepo, fakeBoardRepository, mockCounterBatchService, maxRetry)

            val actualException =
                assertThrows<RuntimeException> {
                    serviceWithFailingRepo.incrementViewCount(boardId)
                }
            assertEquals(expectedException, actualException)
        }
    }

    @Nested
    @DisplayName("좋아요 증가 (incrementLikeCount)")
    inner class IncrementLikeCount {
        @Test
        @DisplayName("성공: 첫 좋아요 시 Redis 카운터 1 증가, 사용자 추가, Pending 추가, 총 카운트 반환")
        fun `given first like, when incrementLikeCount, then updates redis, returns total count`() {
            val boardId = testBoardId1 // DB Like: 10
            val userId = testUserId1
            val expectedDbCount = 10

            val totalLikeCount = counterService.incrementLikeCount(boardId, userId)

            val redisCounters = fakeCounterRepository.getSingleBoardCountersFromRedis(boardId, userId)
            assertEquals(1, redisCounters.likeCount, "Redis 좋아요 수는 1이어야 함")
            assertTrue(redisCounters.userLiked, "사용자는 Redis에서 좋아요 표시가 되어야 함")
            assertTrue(fakeCounterRepository.fetchPendingBoardIds().contains(boardId.toString()), "boardId는 반영 대기 목록에 추가되어야 함")
            assertEquals(expectedDbCount + 1, totalLikeCount, "총 카운트는 DB + Redis여야 함")
        }

        @Test
        @DisplayName("성공: DB 좋아요 수 제공 시 DB 조회 없이 총 카운트 반환")
        fun `given first like with db count provided, when incrementLikeCount, then updates redis without db fetch`() {
            val boardId = testBoardId1 // DB Like: 10
            val userId = testUserId1
            val providedDbCount = 10

            val totalLikeCount = counterService.incrementLikeCount(boardId, userId, providedDbCount)

            assertEquals(providedDbCount + 1, totalLikeCount)
            val redisCounters = fakeCounterRepository.getSingleBoardCountersFromRedis(boardId, userId)
            assertEquals(1, redisCounters.likeCount)
            assertTrue(redisCounters.userLiked)
            assertTrue(fakeCounterRepository.fetchPendingBoardIds().contains(boardId.toString()))
        }

        @Test
        @DisplayName("성공: 여러 사용자 좋아요 시 Redis 카운터 누적")
        fun `given multiple likes from different users, when incrementLikeCount, then count accumulates`() {
            val boardId = testBoardId1 // DB Like: 10
            val userId1 = testUserId1
            val userId2 = testUserId2
            val expectedDbCount = 10

            val totalCount1 = counterService.incrementLikeCount(boardId, userId1)
            val totalCount2 = counterService.incrementLikeCount(boardId, userId2)

            assertEquals(expectedDbCount + 1, totalCount1)
            assertEquals(expectedDbCount + 2, totalCount2, "총 카운트는 두 번째 좋아요를 반영해야 함")

            val redisCounters1 = fakeCounterRepository.getSingleBoardCountersFromRedis(boardId, userId1)
            val redisCounters2 = fakeCounterRepository.getSingleBoardCountersFromRedis(boardId, userId2)
            assertEquals(2, redisCounters1.likeCount, "두 번째 좋아요 후 Redis 카운트는 2여야 함")
            assertEquals(2, redisCounters2.likeCount)
            assertTrue(redisCounters1.userLiked)
            assertTrue(redisCounters2.userLiked)
            assertTrue(fakeCounterRepository.fetchPendingBoardIds().contains(boardId.toString()))
        }

        @Test
        @DisplayName("실패: 이미 좋아요한 사용자가 다시 호출 시 AppException(INVALID_OPERATION) 발생")
        fun `given already liked user, when incrementLikeCount, then throws AppException`() {
            val boardId = testBoardId1
            val userId = testUserId1
            counterService.incrementLikeCount(boardId, userId)

            val exception =
                assertThrows<AppException> {
                    counterService.incrementLikeCount(boardId, userId)
                }
            assertEquals(ErrorCode.INVALID_OPERATION, exception.errorCode)
            assertTrue(exception.message?.contains("이미 좋아요 상태입니다") ?: false)

            val redisCounters = fakeCounterRepository.getSingleBoardCountersFromRedis(boardId, userId)
            assertEquals(1, redisCounters.likeCount)
        }

        @Test
        @DisplayName("실패: DB 조회 실패 시 0으로 간주하고 Redis 처리 후 결과 반환")
        fun `given db fetch fails, when incrementLikeCount, then defaults db count to 0`() {
            val nonExistentBoardId = 999L
            val userId = testUserId1

            val totalLikeCount = counterService.incrementLikeCount(nonExistentBoardId, userId)

            assertEquals(0 + 1, totalLikeCount, "총 카운트는 0(기본 DB) + 1(Redis)이어야 함")
            val redisCounters = fakeCounterRepository.getSingleBoardCountersFromRedis(nonExistentBoardId, userId)
            assertEquals(1, redisCounters.likeCount)
            assertTrue(redisCounters.userLiked)
            assertTrue(fakeCounterRepository.fetchPendingBoardIds().contains(nonExistentBoardId.toString()))
        }

        @Test
        @DisplayName("실패: Redis 작업 실패 시 예외 발생")
        fun `given repo throws exception, when incrementLikeCount, then throws exception`() {
            val boardId = testBoardId1
            val userId = testUserId1
            val expectedException = RuntimeException("Redis script failed")
            val failingRepo = spy(fakeCounterRepository)

            assertNotNull(boardId, "boardId는 stubbing 전에 null이 아니어야 함")
            assertNotNull(userId, "userId는 stubbing 전에 null이 아니어야 함")
            doThrow(expectedException).whenever(failingRepo).atomicLikeOperation(eq(boardId), eq(userId))
            val serviceWithFailingRepo = CounterService(failingRepo, fakeBoardRepository, mockCounterBatchService, maxRetry)

            val actualException =
                assertThrows<RuntimeException> {
                    serviceWithFailingRepo.incrementLikeCount(boardId, userId)
                }
            assertEquals(expectedException, actualException)
        }
    }

    @Nested
    @DisplayName("좋아요 감소 (decrementLikeCount)")
    inner class DecrementLikeCount {
        @Test
        @DisplayName("성공: 좋아요한 사용자가 취소 시 Redis 카운터 감소, 사용자 제거, Pending 추가, 총 카운트 반환")
        fun `given liked user cancels, when decrementLikeCount, then updates redis, returns total count`() {
            val boardId = testBoardId1 // DB Like: 10
            val userId = testUserId1
            val expectedDbCount = 10
            fakeCounterRepository.addUserLike(boardId, userId)
            fakeCounterRepository.setCounts(boardId, 0, 1)
            fakeCounterRepository.addPendingUpdate(boardId)

            val totalLikeCount = counterService.decrementLikeCount(boardId, userId)

            val redisCounters = fakeCounterRepository.getSingleBoardCountersFromRedis(boardId, userId)
            assertEquals(0, redisCounters.likeCount, "Redis 좋아요 수는 0이어야 함")
            assertFalse(redisCounters.userLiked, "사용자는 Redis에서 좋아요 표시가 되지 않아야 함")
            assertTrue(fakeCounterRepository.fetchPendingBoardIds().contains(boardId.toString()), "boardId는 여전히 반영 대기 상태여야 함")
            assertEquals(expectedDbCount + 0, totalLikeCount, "총 카운트는 DB + Redis(현재 0 델타)여야 함")
        }

        @Test
        @DisplayName("성공: DB 좋아요 수 제공 시 DB 조회 없이 총 카운트 반환")
        fun `given liked user cancels with db count provided, when decrementLikeCount, then updates redis without db fetch`() {
            val boardId = testBoardId1 // DB Like: 10
            val userId = testUserId1
            val providedDbCount = 10
            fakeCounterRepository.addUserLike(boardId, userId)
            fakeCounterRepository.setCounts(boardId, 0, 1)
            fakeCounterRepository.addPendingUpdate(boardId)

            val totalLikeCount = counterService.decrementLikeCount(boardId, userId, providedDbCount)

            assertEquals(providedDbCount + 0, totalLikeCount)
            val redisCounters = fakeCounterRepository.getSingleBoardCountersFromRedis(boardId, userId)
            assertEquals(0, redisCounters.likeCount)
            assertFalse(redisCounters.userLiked)
            assertTrue(fakeCounterRepository.fetchPendingBoardIds().contains(boardId.toString()))
        }

        @Test
        @DisplayName("실패: 좋아요하지 않은 사용자가 취소 시도 시 AppException(INVALID_OPERATION) 발생")
        fun `given not liked user cancels, when decrementLikeCount, then throws AppException`() {
            val boardId = testBoardId1 // DB Like: 10, Redis Like: 0
            val userId = testUserId1
            assertFalse(fakeCounterRepository.isUserLiked(boardId, userId))
            assertEquals(0, fakeCounterRepository.getLikeCount(boardId))

            val exception =
                assertThrows<AppException> {
                    counterService.decrementLikeCount(boardId, userId)
                }
            assertEquals(ErrorCode.INVALID_OPERATION, exception.errorCode)
            assertTrue(exception.message.contains("이미 좋아요 취소 상태입니다") ?: false)

            val redisCounters = fakeCounterRepository.getSingleBoardCountersFromRedis(boardId, userId)
            assertEquals(0, redisCounters.likeCount)
        }

        @Test
        @DisplayName("실패: DB 조회 실패 시 0으로 간주하고 Redis 처리 후 결과 반환")
        fun `given db fetch fails, when decrementLikeCount, then defaults db count to 0`() {
            val nonExistentBoardId = 999L
            val userId = testUserId1
            fakeCounterRepository.addUserLike(nonExistentBoardId, userId)
            fakeCounterRepository.setCounts(nonExistentBoardId, 0, 1)
            fakeCounterRepository.addPendingUpdate(nonExistentBoardId)

            val totalLikeCount = counterService.decrementLikeCount(nonExistentBoardId, userId)

            assertEquals(0 + 0, totalLikeCount, "총 카운트는 0(기본 DB) + 0(좋아요 취소 후 Redis)여야 함")
            val redisCounters = fakeCounterRepository.getSingleBoardCountersFromRedis(nonExistentBoardId, userId)
            assertEquals(0, redisCounters.likeCount)
            assertFalse(redisCounters.userLiked)
            assertTrue(fakeCounterRepository.fetchPendingBoardIds().contains(nonExistentBoardId.toString()))
        }

        @Test
        @DisplayName("실패: Redis 작업 실패 시 예외 발생")
        fun `given repo throws exception, when decrementLikeCount, then throws exception`() {
            val boardId = testBoardId1
            val userId = testUserId1
            fakeCounterRepository.addUserLike(boardId, userId)
            fakeCounterRepository.setCounts(boardId, 0, 1)

            val expectedException = RuntimeException("Redis script failed")
            val failingRepo = spy(fakeCounterRepository)

            assertNotNull(boardId, "boardId는 stubbing 전에 null이 아니어야 함")
            assertNotNull(userId, "userId는 stubbing 전에 null이 아니어야 함")
            doThrow(expectedException).whenever(failingRepo).atomicUnlikeOperation(eq(boardId), eq(userId))
            val serviceWithFailingRepo = CounterService(failingRepo, fakeBoardRepository, mockCounterBatchService, maxRetry)

            val actualException =
                assertThrows<RuntimeException> {
                    serviceWithFailingRepo.decrementLikeCount(boardId, userId)
                }
            assertEquals(expectedException, actualException)
        }
    }

    @Nested
    @DisplayName("단일 게시글 카운터 조회 (getSingleBoardCounters)")
    inner class GetSingleBoardCounters {
        @Test
        @DisplayName("성공: DB값과 Redis값 합산하여 반환 (사용자 ID 제공 시 좋아요 여부 포함)")
        fun `given boardId and userId, when getSingleBoardCounters, then returns combined counts and liked status`() {
            val boardId = testBoardId1 // DB View: 100, DB Like: 10
            val userId = testUserId1
            val expectedDbView = 100
            val expectedDbLike = 10

            fakeCounterRepository.setCounts(boardId, 5, 2) // Redis View: 5, Redis Like: 2
            fakeCounterRepository.addUserLike(boardId, userId) // User liked in Redis

            val counters = counterService.getSingleBoardCounters(boardId, userId)

            assertEquals(boardId, counters.boardId)
            assertEquals(expectedDbView + 5, counters.viewCount)
            assertEquals(expectedDbLike + 2, counters.likeCount)
            assertTrue(counters.userLiked)
        }

        @Test
        @DisplayName("성공: DB값만 제공 시 Redis 조회 없이 합산하여 반환")
        fun `given boardId, userId and db counts, when getSingleBoardCounters, then returns combined counts without db fetch`() {
            val boardId = testBoardId1
            val userId = testUserId1
            val providedDbView = 100
            val providedDbLike = 10

            fakeCounterRepository.setCounts(boardId, 5, 2)
            fakeCounterRepository.addUserLike(boardId, userId)

            val counters = counterService.getSingleBoardCounters(boardId, userId, providedDbView, providedDbLike)

            assertEquals(boardId, counters.boardId)
            assertEquals(providedDbView + 5, counters.viewCount)
            assertEquals(providedDbLike + 2, counters.likeCount)
            assertTrue(counters.userLiked)
        }

        @Test
        @DisplayName("성공: 사용자 ID 미제공 시 좋아요 여부는 false")
        fun `given boardId without userId, when getSingleBoardCounters, then userLiked is false`() {
            val boardId = testBoardId1 // DB View: 100, DB Like: 10
            val expectedDbView = 100
            val expectedDbLike = 10
            fakeCounterRepository.setCounts(boardId, 5, 2)
            fakeCounterRepository.addUserLike(boardId, testUserId1) // Some other user liked it

            val counters = counterService.getSingleBoardCounters(boardId, null) // No user ID provided

            assertEquals(boardId, counters.boardId)
            assertEquals(expectedDbView + 5, counters.viewCount)
            assertEquals(expectedDbLike + 2, counters.likeCount)
            assertFalse(counters.userLiked, "UserLiked should be false when no userId is given")
        }

        @Test
        @DisplayName("성공: Redis에 카운터 없으면 DB 값만 반환")
        fun `given no redis counts, when getSingleBoardCounters, then returns only db counts`() {
            val boardId = testBoardId1 // DB View: 100, DB Like: 10
            val userId = testUserId1
            val expectedDbView = 100
            val expectedDbLike = 10
            // No data set in fakeCounterRepository for boardId

            val counters = counterService.getSingleBoardCounters(boardId, userId)

            assertEquals(boardId, counters.boardId)
            assertEquals(expectedDbView + 0, counters.viewCount)
            assertEquals(expectedDbLike + 0, counters.likeCount)
            assertFalse(counters.userLiked) // User hasn't liked in Redis
        }

        @Test
        @DisplayName("실패: DB 조회 실패 시 DB 카운터 0으로 간주하고 Redis 값만 반환")
        fun `given db fetch fails, when getSingleBoardCounters, then uses 0 for db counts`() {
            val nonExistentBoardId = 999L
            val userId = testUserId1
            fakeCounterRepository.setCounts(nonExistentBoardId, 5, 2)
            fakeCounterRepository.addUserLike(nonExistentBoardId, userId)

            val counters = counterService.getSingleBoardCounters(nonExistentBoardId, userId)

            assertEquals(nonExistentBoardId, counters.boardId)
            assertEquals(0 + 5, counters.viewCount)
            assertEquals(0 + 2, counters.likeCount)
            assertTrue(counters.userLiked)
        }

        @Test
        @DisplayName("실패: Redis 조회 실패 시 예외 발생")
        fun `given repo throws exception, when getSingleBoardCounters, then throws exception`() {
            val boardId = testBoardId1
            val userId = testUserId1
            val expectedException = RuntimeException("Redis MGET failed")
            val failingRepo = spy(fakeCounterRepository)
            assertNotNull(boardId, "boardId should not be null before stubbing")

            doThrow(expectedException).whenever(failingRepo).getSingleBoardCountersFromRedis(
                eq(boardId),
                eq(userId),
            )

            val serviceWithFailingRepo = CounterService(failingRepo, fakeBoardRepository, mockCounterBatchService, maxRetry)

            val actualException =
                assertThrows<RuntimeException> {
                    serviceWithFailingRepo.getSingleBoardCounters(boardId, userId)
                }
            assertEquals(expectedException, actualException)
        }
    }

    @Nested
    @DisplayName("벌크 게시글 카운터 조회 (getBulkBoardCounters)")
    inner class GetBulkBoardCounters {
        @Test
        @DisplayName("성공: 여러 게시글의 DB값과 Redis값 합산하여 반환 (사용자 ID 제공 시 좋아요 여부 포함)")
        fun `given boardIds and userId, when getBulkBoardCounters, then returns combined counts`() {
            val boardId1 = testBoardId1 // DB(100, 10)
            val boardId2 = testBoardId2 // DB(50, 5)
            val userId = testUserId1

            fakeCounterRepository.setCounts(boardId1, 5, 2)
            fakeCounterRepository.addUserLike(boardId1, userId)
            fakeCounterRepository.setCounts(boardId2, 3, 0)

            val input =
                listOf(
                    Triple(boardId1, 100, 10), // (boardId, dbView, dbLike)
                    Triple(boardId2, 50, 5),
                )

            val results = counterService.getBulkBoardCounters(input, userId)

            assertEquals(2, results.size)

            val result1 = results.find { it.boardId == boardId1 }
            assertNotNull(result1)
            assertEquals(100 + 5, result1!!.viewCount)
            assertEquals(10 + 2, result1.likeCount)
            assertTrue(result1.userLiked)

            val result2 = results.find { it.boardId == boardId2 }
            assertNotNull(result2)
            assertEquals(50 + 3, result2!!.viewCount)
            assertEquals(5 + 0, result2.likeCount)
            assertFalse(result2.userLiked) // User didn't like board 2 in Redis
        }

        @Test
        @DisplayName("성공: 사용자 ID 미제공 시 모든 게시글의 좋아요 여부는 false")
        fun `given boardIds without userId, when getBulkBoardCounters, then all userLiked are false`() {
            val boardId1 = testBoardId1
            val boardId2 = testBoardId2
            fakeCounterRepository.setCounts(boardId1, 5, 2)
            fakeCounterRepository.addUserLike(boardId1, testUserId1)
            fakeCounterRepository.setCounts(boardId2, 3, 1)
            fakeCounterRepository.addUserLike(boardId2, testUserId2)

            val input = listOf(Triple(boardId1, 100, 10), Triple(boardId2, 50, 5))

            val results = counterService.getBulkBoardCounters(input, null) // No User ID

            assertEquals(2, results.size)
            assertTrue(results.all { !it.userLiked }) // Check all are false

            val result1 = results.find { it.boardId == boardId1 }
            assertEquals(100 + 5, result1!!.viewCount)
            assertEquals(10 + 2, result1.likeCount)

            val result2 = results.find { it.boardId == boardId2 }
            assertEquals(50 + 3, result2!!.viewCount)
            assertEquals(5 + 1, result2.likeCount)
        }

        @Test
        @DisplayName("성공: 빈 ID 목록 입력 시 빈 리스트 반환")
        fun `given empty list, when getBulkBoardCounters, then returns empty list`() {
            val emptyInput = emptyList<Triple<Long, Int, Int>>()

            val results = counterService.getBulkBoardCounters(emptyInput, testUserId1)

            assertTrue(results.isEmpty())
        }

        @Test
        @DisplayName("성공: 일부 게시글만 Redis에 카운터 존재 시 정상 처리")
        fun `given some boards have no redis counts, when getBulkBoardCounters, then handled correctly`() {
            val boardId1 = testBoardId1 // DB(100, 10) - Has Redis data
            val boardId2 = testBoardId2 // DB(50, 5) - No Redis data
            val userId = testUserId1

            fakeCounterRepository.setCounts(boardId1, 5, 2)
            fakeCounterRepository.addUserLike(boardId1, userId)

            val input = listOf(Triple(boardId1, 100, 10), Triple(boardId2, 50, 5))

            val results = counterService.getBulkBoardCounters(input, userId)

            assertEquals(2, results.size)
            val result1 = results.find { it.boardId == boardId1 }
            assertEquals(100 + 5, result1!!.viewCount)
            assertEquals(10 + 2, result1.likeCount)
            assertTrue(result1.userLiked)

            val result2 = results.find { it.boardId == boardId2 }
            assertEquals(50 + 0, result2!!.viewCount)
            assertEquals(5 + 0, result2.likeCount)
            assertFalse(result2.userLiked)
        }

        @Test
        @DisplayName("실패: Redis 조회 실패 시 예외 발생")
        fun `given repo throws exception, when getBulkBoardCounters, then throws exception`() {
            val boardId1 = testBoardId1
            val input = listOf(Triple(boardId1, 100, 10))
            val userId = testUserId1
            val expectedException = RuntimeException("Redis pipeline failed")

            val failingRepo = spy(fakeCounterRepository)
            assertNotNull(boardId1, "boardId1 should not be null for stubbing")

            doThrow(expectedException).whenever(failingRepo).getBulkBoardCounters(
                eq(listOf(boardId1)),
                eq(userId),
            )

            val serviceWithFailingRepo = CounterService(failingRepo, fakeBoardRepository, mockCounterBatchService, maxRetry)

            val actualException =
                assertThrows<RuntimeException> {
                    serviceWithFailingRepo.getBulkBoardCounters(input, userId)
                }
            assertEquals(expectedException, actualException)
        }
    }

    @Nested
    @DisplayName("DB 동기화 (flushCountersToDatabase)")
    inner class FlushCountersToDatabase {
        @BeforeEach
        fun setupBatch() {
            whenever(mockCounterBatchService.processSingleBoardCounter(anyLong(), anyInt(), anyInt())).thenReturn(true)
        }

        @Test
        @DisplayName("성공: Pending 목록의 카운터 DB 반영 및 Redis 데이터 삭제")
        fun `given pending updates, when flushCounters, then processes updates and clears redis`() {
            val boardId1 = testBoardId1 // DB(100, 10)
            val boardId2 = testBoardId2 // DB(50, 5)
            fakeCounterRepository.setCounts(boardId1, 5, 2) // V+5, L+2
            fakeCounterRepository.addUserLike(boardId1, testUserId1)
            fakeCounterRepository.addPendingUpdate(boardId1)

            fakeCounterRepository.setCounts(boardId2, 3, 0) // V+3, L+0
            fakeCounterRepository.addPendingUpdate(boardId2)

            counterService.flushCountersToDatabase()

            verify(mockCounterBatchService, times(1)).processSingleBoardCounter(eq(boardId1), eq(5), eq(2))
            verify(mockCounterBatchService, times(1)).processSingleBoardCounter(eq(boardId2), eq(3), eq(0))

            val postFlushCounters1 = fakeCounterRepository.getSingleBoardCountersFromRedis(boardId1, testUserId1)
            assertEquals(0, postFlushCounters1.viewCount, "Redis view count for board 1 should be deleted")
            assertEquals(0, postFlushCounters1.likeCount, "Redis like count for board 1 should be deleted")

            val postFlushCounters2 = fakeCounterRepository.getSingleBoardCountersFromRedis(boardId2, null)
            assertEquals(0, postFlushCounters2.viewCount, "Redis view count for board 2 should be deleted")
            assertEquals(0, postFlushCounters2.likeCount, "Redis like count for board 2 should be deleted")

            assertTrue(fakeCounterRepository.fetchPendingBoardIds().isEmpty())
        }

        @Test
        @DisplayName("성공: Pending 목록 비어있으면 작업 수행 안함")
        fun `given no pending updates, when flushCounters, then does nothing`() {
            counterService.flushCountersToDatabase()

            val pendingIds = fakeCounterRepository.fetchPendingBoardIds()
            assertTrue(pendingIds.isEmpty())
            verify(mockCounterBatchService, never()).processSingleBoardCounter(anyLong(), anyInt(), anyInt())
        }

        @Test
        @DisplayName("성공: 잘못된 형식의 ID는 처리하지 않고 Pending 목록에서 제거")
        fun `given invalid ids in pending, when flushCounters, then skips processing and removes them`() {
            val invalidId = "not-a-number"
            val validId = testBoardId1
            fakeCounterRepository.addPendingUpdate(validId)
            (fakeCounterRepository.reflectivelyGetField("pendingUpdates") as MutableSet<String>).add(invalidId)

            fakeCounterRepository.setCounts(validId, 1, 1)

            counterService.flushCountersToDatabase()

            verify(mockCounterBatchService, times(1)).processSingleBoardCounter(eq(validId), eq(1), eq(1))

            assertTrue(fakeCounterRepository.fetchPendingBoardIds().isEmpty())
        }

        @Test
        @DisplayName("실패: 일부 게시글 DB 업데이트 실패 시 해당 건만 실패 처리 및 재시도 카운트 증가")
        fun `given some updates fail in batch service, when flushCounters, then handles failures and increments retry count`() {
            val successId = testBoardId1
            val failId = testBoardId3 // This board exists
            fakeCounterRepository.setCounts(successId, 1, 1)
            fakeCounterRepository.addPendingUpdate(successId)
            fakeCounterRepository.setCounts(failId, 2, 2)
            fakeCounterRepository.addPendingUpdate(failId)

            whenever(mockCounterBatchService.processSingleBoardCounter(eq(successId), eq(1), eq(1))).thenReturn(true)
            whenever(mockCounterBatchService.processSingleBoardCounter(eq(failId), eq(2), eq(2))).thenReturn(false)

            counterService.flushCountersToDatabase()

            verify(mockCounterBatchService, times(1)).processSingleBoardCounter(eq(successId), eq(1), eq(1))
            verify(mockCounterBatchService, times(1)).processSingleBoardCounter(eq(failId), eq(2), eq(2))

            assertTrue(fakeCounterRepository.fetchPendingBoardIds().isEmpty())

            val failureCache = counterService.reflectivelyGetField("processingFailureCache") as ConcurrentHashMap<Long, Int>
            assertNull(failureCache[successId], "Successful ID should not be in failure cache")
            assertEquals(1, failureCache[failId], "Failed ID should have fail count of 1")

            fakeCounterRepository.setCounts(successId, 1, 1)
            fakeCounterRepository.addPendingUpdate(successId)
            fakeCounterRepository.setCounts(failId, 3, 3)
            fakeCounterRepository.addPendingUpdate(failId)

            whenever(mockCounterBatchService.processSingleBoardCounter(eq(failId), eq(3), eq(3))).thenReturn(false)

            counterService.flushCountersToDatabase()

            verify(mockCounterBatchService, times(2)).processSingleBoardCounter(eq(successId), eq(1), eq(1))
            verify(mockCounterBatchService, times(1)).processSingleBoardCounter(eq(failId), eq(3), eq(3))

            assertEquals(2, failureCache[failId], "Failed ID should have fail count of 2")
        }

        @Test
        @DisplayName("실패: 최대 재시도 횟수 초과 시 DB 업데이트 건너뜀")
        fun `given retry limit exceeded, when flushCounters, then skips processing`() {
            val failId = testBoardId3
            fakeCounterRepository.setCounts(failId, 1, 1)
            fakeCounterRepository.addPendingUpdate(failId)

            val failureCache = counterService.reflectivelyGetField("processingFailureCache") as ConcurrentHashMap<Long, Int>
            failureCache[failId] = maxRetry

            whenever(mockCounterBatchService.processSingleBoardCounter(eq(failId), anyInt(), anyInt())).thenReturn(false)

            counterService.flushCountersToDatabase()

            verify(mockCounterBatchService, never()).processSingleBoardCounter(eq(failId), eq(1), eq(1))

            assertTrue(fakeCounterRepository.fetchPendingBoardIds().isEmpty())

            assertEquals(maxRetry, failureCache[failId])
        }

        @Test
        @DisplayName("실패: Redis Get&Delete 실패 시 배치 중단 (예외는 내부 처리)")
        fun `given redis fails during getAndDelete, when flushCounters, then stops processing without throwing`() {
            val boardId1 = testBoardId1
            fakeCounterRepository.setCounts(boardId1, 1, 1) // View count key: board:1:view
            fakeCounterRepository.setCounts(boardId1, 0, 1) // Like count key: board:1:like (실제로는 getAndDeleteCounts 에서 처리됨)
            fakeCounterRepository.addPendingUpdate(boardId1)

            val failingRepo = spy(fakeCounterRepository)
            val expectedException = RuntimeException("Pipeline failed")

            doAnswer { invocation ->
                throw expectedException
            }.whenever(failingRepo).getAndDeleteCountsPipelined(anyList())

            val serviceWithFailingRepo = CounterService(failingRepo, fakeBoardRepository, mockCounterBatchService, maxRetry)

            assertDoesNotThrow { serviceWithFailingRepo.flushCountersToDatabase() }

            verify(failingRepo, times(1)).fetchPendingBoardIds()

            verify(failingRepo, times(1)).getAndDeleteCountsPipelined(eq(listOf(RedisCounterRepository.viewCountKey(boardId1))))

            verify(failingRepo, never()).getAndDeleteCountsPipelined(eq(listOf(RedisCounterRepository.likeCountKey(boardId1))))

            val pendingIds = failingRepo.fetchPendingBoardIds()
            assertEquals(setOf(boardId1.toString()), pendingIds)
        }
    }

    @Nested
    @DisplayName("카운터 정리 (cleanupBoardCounters)")
    inner class CleanupBoardCounters {
        @Test
        @DisplayName("성공: 게시글 ID 관련 모든 Redis 데이터 삭제")
        fun `given boardId, when cleanupBoardCounters, then deletes all related redis keys`() {
            val boardId = testBoardId1
            val userId = testUserId1
            fakeCounterRepository.setCounts(boardId, 5, 2)
            fakeCounterRepository.addUserLike(boardId, userId)
            fakeCounterRepository.addPendingUpdate(boardId)

            assertNotEquals(0, fakeCounterRepository.getViewCount(boardId))
            assertNotEquals(0, fakeCounterRepository.getLikeCount(boardId))
            assertTrue(fakeCounterRepository.isUserLiked(boardId, userId))
            assertTrue(fakeCounterRepository.fetchPendingBoardIds().contains(boardId.toString()))

            counterService.cleanupBoardCounters(boardId)

            assertEquals(0, fakeCounterRepository.getViewCount(boardId))
            assertEquals(0, fakeCounterRepository.getLikeCount(boardId))
            assertFalse(fakeCounterRepository.isUserLiked(boardId, userId))
            assertFalse(fakeCounterRepository.fetchPendingBoardIds().contains(boardId.toString()))
        }

        @Test
        @DisplayName("실패: Redis 작업 실패 시 예외 발생")
        fun `given repo throws exception, when cleanupBoardCounters, then throws exception`() {
            val boardId = testBoardId1
            val expectedException = RuntimeException("Redis DEL failed")
            val failingRepo = spy(fakeCounterRepository)
            assertNotNull(boardId, "boardId should not be null before stubbing")
            doThrow(expectedException).whenever(failingRepo).deleteBoardCounters(eq(boardId))
            val serviceWithFailingRepo = CounterService(failingRepo, fakeBoardRepository, mockCounterBatchService, maxRetry)

            val actualException =
                assertThrows<RuntimeException> {
                    serviceWithFailingRepo.cleanupBoardCounters(boardId)
                }
            assertEquals(expectedException, actualException)
        }
    }
}
