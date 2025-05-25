package com.wildrew.app.community.counting

import com.wildrew.app.utils.config.BaseIntegrationTest
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate

@DisplayName("CounterRepository 통합 테스트 (Redis)")
class RedisCounterRepositoryIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var redisCounterRepository: RedisCounterRepository

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    private val testBoardId1 = 1L
    private val testBoardId2 = 2L
    private val testUserId1 = "user:101"
    private val testUserId2 = "user:102"

    @AfterEach
    fun cleanupRedisData() {
        val keysToDelete = mutableListOf<String>()
        keysToDelete.add(RedisCounterRepository.viewCountKey(testBoardId1))
        keysToDelete.add(RedisCounterRepository.likeCountKey(testBoardId1))
        keysToDelete.add(RedisCounterRepository.likeUsersKey(testBoardId1))
        keysToDelete.add(RedisCounterRepository.viewCountKey(testBoardId2))
        keysToDelete.add(RedisCounterRepository.likeCountKey(testBoardId2))
        keysToDelete.add(RedisCounterRepository.likeUsersKey(testBoardId2))
        keysToDelete.add(RedisCounterRepository.PENDING_UPDATES)

        redisTemplate.delete(keysToDelete)
    }

    @Nested
    @DisplayName("게시글 카운터 데이터 삭제 (deleteBoardCounters)")
    inner class DeleteBoardCounters {
        @Test
        @DisplayName("성공: 지정된 게시글 ID의 View, Like 카운트, Like 사용자 Set, Pending 상태 제거")
        fun `given existing counters, when deleteBoardCounters, then removes all related keys`() {
            val boardId = testBoardId1
            val viewKey = RedisCounterRepository.viewCountKey(boardId)
            val likeKey = RedisCounterRepository.likeCountKey(boardId)
            val userKey = RedisCounterRepository.likeUsersKey(boardId)
            val pendingKey = RedisCounterRepository.PENDING_UPDATES

            redisTemplate.opsForValue().set(viewKey, "10")
            redisTemplate.opsForValue().set(likeKey, "5")
            redisTemplate.opsForSet().add(userKey, testUserId1)
            redisTemplate.opsForSet().add(pendingKey, boardId.toString())

            redisCounterRepository.deleteBoardCounters(boardId)

            assertNull(redisTemplate.opsForValue().get(viewKey), "조회수 키가 삭제되어야 함")
            assertNull(redisTemplate.opsForValue().get(likeKey), "좋아요 수 키가 삭제되어야 함")
            assertEquals(0, redisTemplate.opsForSet().size(userKey), "좋아요 사용자 집합이 삭제되거나 비어 있어야 함")
            assertFalse(redisTemplate.opsForSet().isMember(pendingKey, boardId.toString()) ?: false, "pending 집합에서 boardId가 제거되어야 함")
        }

        @Test
        @DisplayName("성공: 존재하지 않는 게시글 ID에 대해 호출해도 오류 발생 안함")
        fun `given non-existent boardId, when deleteBoardCounters, then completes without error`() {
            val boardId = 999L

            assertDoesNotThrow {
                redisCounterRepository.deleteBoardCounters(boardId)
            }
            assertNull(redisTemplate.opsForValue().get(RedisCounterRepository.viewCountKey(boardId)))
        }
    }

    @Nested
    @DisplayName("조회수 원자적 증가 및 Pending 추가 (atomicIncrementViewCountAndAddPending)")
    inner class AtomicIncrementViewCount {
        @Test
        @DisplayName("성공: 첫 호출 시 조회수 1, Pending 추가 및 새로운 카운트 반환")
        fun `given new boardId, when called once, then sets count to 1, adds to pending, returns 1`() {
            val boardId = testBoardId1
            val viewKey = RedisCounterRepository.viewCountKey(boardId)
            val pendingKey = RedisCounterRepository.PENDING_UPDATES

            val newCount = redisCounterRepository.atomicIncrementViewCountAndAddPending(boardId)

            assertEquals(1L, newCount)
            assertEquals("1", redisTemplate.opsForValue().get(viewKey))
            assertTrue(redisTemplate.opsForSet().isMember(pendingKey, boardId.toString()) ?: false)
        }

        @Test
        @DisplayName("성공: 여러 번 호출 시 조회수 누적 증가 및 Pending 상태 유지")
        fun `given existing boardId, when called multiple times, then increments count, keeps pending, returns updated count`() {
            val boardId = testBoardId1
            val viewKey = RedisCounterRepository.viewCountKey(boardId)
            val pendingKey = RedisCounterRepository.PENDING_UPDATES
            redisCounterRepository.atomicIncrementViewCountAndAddPending(boardId)

            val newCount2 = redisCounterRepository.atomicIncrementViewCountAndAddPending(boardId)
            val newCount3 = redisCounterRepository.atomicIncrementViewCountAndAddPending(boardId)

            assertEquals(2L, newCount2)
            assertEquals(3L, newCount3)
            assertEquals("3", redisTemplate.opsForValue().get(viewKey))
            assertTrue(redisTemplate.opsForSet().isMember(pendingKey, boardId.toString()) ?: false)
        }
    }

    @Nested
    @DisplayName("좋아요 원자적 처리 (atomicLikeOperation)")
    inner class AtomicLikeOperation {
        @Test
        @DisplayName("성공: 첫 좋아요 시 카운트 1, 사용자 추가, Pending 추가, 카운트(1) 반환")
        fun `given first like, when called, then sets count 1, adds user and pending, returns 1`() {
            val boardId = testBoardId1
            val userId = testUserId1
            val likeKey = RedisCounterRepository.likeCountKey(boardId)
            val userKey = RedisCounterRepository.likeUsersKey(boardId)
            val pendingKey = RedisCounterRepository.PENDING_UPDATES

            val resultCount = redisCounterRepository.atomicLikeOperation(boardId, userId)

            assertEquals(1, resultCount)
            assertEquals("1", redisTemplate.opsForValue().get(likeKey))
            assertTrue(redisTemplate.opsForSet().isMember(userKey, userId) ?: false)
            assertTrue(redisTemplate.opsForSet().isMember(pendingKey, boardId.toString()) ?: false)
        }

        @Test
        @DisplayName("실패: 이미 좋아요한 사용자가 호출 시 AppException 발생")
        fun `given already liked, when called again, then throws AppException`() {
            val boardId = testBoardId1
            val userId = testUserId1
            redisCounterRepository.atomicLikeOperation(boardId, userId)

            val exception =
                assertThrows<AppException> {
                    redisCounterRepository.atomicLikeOperation(boardId, userId)
                }
            assertEquals(ErrorCode.INVALID_OPERATION, exception.errorCode)
            assertTrue(exception.message?.contains("이미 좋아요 상태입니다") ?: false)

            assertEquals("1", redisTemplate.opsForValue().get(RedisCounterRepository.likeCountKey(boardId)))
            assertTrue(redisTemplate.opsForSet().isMember(RedisCounterRepository.likeUsersKey(boardId), userId) ?: false)
        }

        @Test
        @DisplayName("성공: 다른 사용자가 좋아요 시 카운트 증가, 사용자 추가, 카운트(2) 반환")
        fun `given liked by user1, when liked by user2, then increments count, adds user2, returns 2`() {
            val boardId = testBoardId1
            redisCounterRepository.atomicLikeOperation(boardId, testUserId1)

            val resultCount = redisCounterRepository.atomicLikeOperation(boardId, testUserId2)

            assertEquals(2, resultCount)
            assertEquals("2", redisTemplate.opsForValue().get(RedisCounterRepository.likeCountKey(boardId)))
            assertTrue(redisTemplate.opsForSet().isMember(RedisCounterRepository.likeUsersKey(boardId), testUserId1) ?: false)
            assertTrue(redisTemplate.opsForSet().isMember(RedisCounterRepository.likeUsersKey(boardId), testUserId2) ?: false)
            assertTrue(redisTemplate.opsForSet().isMember(RedisCounterRepository.PENDING_UPDATES, boardId.toString()) ?: false)
        }
    }

    @Nested
    @DisplayName("좋아요 취소 원자적 처리 (atomicUnlikeOperation)")
    inner class AtomicUnlikeOperation {
        @Test
        @DisplayName("성공: 좋아요한 사용자가 취소 시 카운트 감소, 사용자 제거, Pending 추가, 카운트(0) 반환")
        fun `given liked user, when called, then decrements count, removes user, keeps pending, returns 0`() {
            val boardId = testBoardId1
            val userId = testUserId1
            redisCounterRepository.atomicLikeOperation(boardId, userId)
            assertEquals("1", redisTemplate.opsForValue().get(RedisCounterRepository.likeCountKey(boardId)))
            assertTrue(redisTemplate.opsForSet().isMember(RedisCounterRepository.likeUsersKey(boardId), userId) ?: false)

            val resultCount = redisCounterRepository.atomicUnlikeOperation(boardId, userId)

            assertEquals(0, resultCount)
            assertEquals("0", redisTemplate.opsForValue().get(RedisCounterRepository.likeCountKey(boardId)))
            assertFalse(redisTemplate.opsForSet().isMember(RedisCounterRepository.likeUsersKey(boardId), userId) ?: false)
            assertTrue(redisTemplate.opsForSet().isMember(RedisCounterRepository.PENDING_UPDATES, boardId.toString()) ?: false)
        }

        @Test
        @DisplayName("실패: 좋아요하지 않은 사용자가 호출 시 AppException 발생")
        fun `given not liked user, when called, then throws AppException`() {
            val boardId = testBoardId1
            val userId = testUserId1

            val exception =
                assertThrows<AppException> {
                    redisCounterRepository.atomicUnlikeOperation(boardId, userId)
                }
            assertEquals(ErrorCode.INVALID_OPERATION, exception.errorCode)
            assertTrue(exception.message?.contains("이미 좋아요 취소 상태입니다") ?: false)
        }

        @Test
        @DisplayName("성공: 여러 명이 좋아요한 상태에서 한 명 취소 시 카운트 감소")
        fun `given multiple likes, when one unlikes, then decrements count correctly`() {
            val boardId = testBoardId1
            redisCounterRepository.atomicLikeOperation(boardId, testUserId1)
            redisCounterRepository.atomicLikeOperation(boardId, testUserId2)

            val resultCount = redisCounterRepository.atomicUnlikeOperation(boardId, testUserId1)

            assertEquals(1, resultCount)
            assertEquals("1", redisTemplate.opsForValue().get(RedisCounterRepository.likeCountKey(boardId)))
            assertFalse(redisTemplate.opsForSet().isMember(RedisCounterRepository.likeUsersKey(boardId), testUserId1) ?: false)
            assertTrue(redisTemplate.opsForSet().isMember(RedisCounterRepository.likeUsersKey(boardId), testUserId2) ?: false)
        }
    }

    @Nested
    @DisplayName("단일 게시글 Redis 카운터 조회 (getSingleBoardCountersFromRedis)")
    inner class GetSingleBoardCountersFromRedis {
        @Test
        @DisplayName("성공: 카운터가 존재하는 게시글 ID의 Redis 카운터 및 좋아요 여부 조회")
        fun `given existing counters, when getSingleBoardCountersFromRedis, then returns correct counts and liked status`() {
            val boardId = testBoardId1
            val userId = testUserId1

            redisTemplate.opsForValue().set(RedisCounterRepository.viewCountKey(boardId), "5")
            redisTemplate.opsForValue().set(RedisCounterRepository.likeCountKey(boardId), "3")
            redisTemplate.opsForSet().add(RedisCounterRepository.likeUsersKey(boardId), userId)

            val counters = redisCounterRepository.getSingleBoardCountersFromRedis(boardId, userId)

            assertEquals(5, counters.viewCount)
            assertEquals(3, counters.likeCount)
            assertTrue(counters.userLiked)
        }

        @Test
        @DisplayName("성공: 카운터가 없는 게시글 ID는 0 카운트 및 좋아요 안함으로 조회")
        fun `given no counters, when getSingleBoardCountersFromRedis, then returns zeros and not liked`() {
            val boardId = testBoardId1
            val userId = testUserId1

            val counters = redisCounterRepository.getSingleBoardCountersFromRedis(boardId, userId)

            assertEquals(0, counters.viewCount)
            assertEquals(0, counters.likeCount)
            assertFalse(counters.userLiked)
        }

        @Test
        @DisplayName("성공: 사용자 ID 없이 호출 시 좋아요 여부는 false 반환")
        fun `given no user id, when getSingleBoardCountersFromRedis, then userLiked is false`() {
            val boardId = testBoardId1

            redisTemplate.opsForValue().set(RedisCounterRepository.viewCountKey(boardId), "5")
            redisTemplate.opsForValue().set(RedisCounterRepository.likeCountKey(boardId), "3")
            redisTemplate.opsForSet().add(RedisCounterRepository.likeUsersKey(boardId), testUserId1)

            val counters = redisCounterRepository.getSingleBoardCountersFromRedis(boardId, null)

            assertEquals(5, counters.viewCount)
            assertEquals(3, counters.likeCount)
            assertFalse(counters.userLiked)
        }
    }

    @Nested
    @DisplayName("벌크 게시글 Redis 카운터 조회 (getBulkBoardCounters)")
    inner class GetBulkBoardCounters {
        @Test
        @DisplayName("성공: 여러 게시글 ID의 Redis 카운터 및 좋아요 여부 조회")
        fun `given multiple boardIds, when getBulkBoardCounters, then returns map of counters`() {
            val boardId1 = testBoardId1
            val boardId2 = testBoardId2
            val userId = testUserId1

            redisTemplate.opsForValue().set(RedisCounterRepository.viewCountKey(boardId1), "5")
            redisTemplate.opsForValue().set(RedisCounterRepository.likeCountKey(boardId1), "3")
            redisTemplate.opsForSet().add(RedisCounterRepository.likeUsersKey(boardId1), userId)

            redisTemplate.opsForValue().set(RedisCounterRepository.viewCountKey(boardId2), "10")
            redisTemplate.opsForValue().set(RedisCounterRepository.likeCountKey(boardId2), "7")

            val results = redisCounterRepository.getBulkBoardCounters(listOf(boardId1, boardId2), userId)

            assertEquals(2, results.size)

            val counter1 = results[boardId1]
            assertNotNull(counter1)
            assertEquals(5, counter1?.viewCount)
            assertEquals(3, counter1?.likeCount)
            assertTrue(counter1?.userLiked ?: false)

            val counter2 = results[boardId2]
            assertNotNull(counter2)
            assertEquals(10, counter2?.viewCount)
            assertEquals(7, counter2?.likeCount)
            assertFalse(counter2?.userLiked ?: true)
        }

        @Test
        @DisplayName("성공: 빈 ID 목록 입력 시 빈 맵 반환")
        fun `given empty list, when getBulkBoardCounters, then returns empty map`() {
            val results = redisCounterRepository.getBulkBoardCounters(emptyList(), testUserId1)
            assertTrue(results.isEmpty())
        }
    }

    @Nested
    @DisplayName("Redis 카운터 값 조회 후 삭제 (getAndDeleteCount)")
    inner class GetAndDeleteCount {
        @Test
        @DisplayName("성공: 존재하는 키의 값 반환 후 키 삭제")
        fun `given existing key, when getAndDeleteCount, then returns value and deletes key`() {
            val key = "test:count:key"
            val value = "42"
            redisTemplate.opsForValue().set(key, value)

            val result = redisCounterRepository.getAndDeleteCount(key)

            assertEquals(42, result)
            assertNull(redisTemplate.opsForValue().get(key))
        }
    }

    @Nested
    @DisplayName("여러 Redis 카운터 값 동시 조회 후 삭제 (getAndDeleteCountsPipelined)")
    inner class GetAndDeleteCountsPipelined {
        @Test
        @DisplayName("성공: 여러 키의 값을 조회 후 모두 삭제")
        fun `given multiple keys, when getAndDeleteCountsPipelined, then returns all values and deletes keys`() {
            val key1 = "test:count:key1"
            val key2 = "test:count:key2"
            val key3 = "test:count:nonexistent"

            redisTemplate.opsForValue().set(key1, "10")
            redisTemplate.opsForValue().set(key2, "20")

            val results = redisCounterRepository.getAndDeleteCountsPipelined(listOf(key1, key2, key3))

            assertEquals(3, results.size)
            assertEquals(10, results[0])
            assertEquals(20, results[1])
            assertNull(results[2])

            assertNull(redisTemplate.opsForValue().get(key1))
            assertNull(redisTemplate.opsForValue().get(key2))
        }

        @Test
        @DisplayName("성공: 빈 키 목록 입력 시 빈 리스트 반환")
        fun `given empty key list, when getAndDeleteCountsPipelined, then returns empty list`() {
            val results = redisCounterRepository.getAndDeleteCountsPipelined(emptyList())
            assertTrue(results.isEmpty())
        }
    }

    @Nested
    @DisplayName("Pending 게시글 ID 목록 조회 (fetchPendingBoardIds)")
    inner class FetchPendingBoardIds {
        @Test
        @DisplayName("성공: Pending 목록에 추가된 게시글 ID 조회")
        fun `given pending boardIds, when fetchPendingBoardIds, then returns all IDs`() {
            val pendingKey = RedisCounterRepository.PENDING_UPDATES
            redisTemplate.opsForSet().add(pendingKey, "1", "2", "3")

            val result = redisCounterRepository.fetchPendingBoardIds()

            assertEquals(3, result.size)
            assertTrue(result.containsAll(setOf("1", "2", "3")))
        }

        @Test
        @DisplayName("성공: Pending 목록이 비어있으면 빈 Set 반환")
        fun `given empty pending set, when fetchPendingBoardIds, then returns empty set`() {
            val result = redisCounterRepository.fetchPendingBoardIds()
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Pending 게시글 ID 목록에서 제거 (removePendingBoardIds)")
    inner class RemovePendingBoardIds {
        @Test
        @DisplayName("성공: Pending 목록에서 지정된 게시글 ID 제거")
        fun `given pending IDs to remove, when removePendingBoardIds, then removes only specified IDs`() {
            val pendingKey = RedisCounterRepository.PENDING_UPDATES
            redisTemplate.opsForSet().add(pendingKey, "1", "2", "3", "4")

            redisCounterRepository.removePendingBoardIds(listOf("1", "3"))

            val remainingIds = redisTemplate.opsForSet().members(pendingKey)
            assertEquals(2, remainingIds?.size)
            assertTrue(remainingIds?.containsAll(setOf("2", "4")) ?: false)
        }

        @Test
        @DisplayName("성공: 빈 ID 목록 입력 시 Pending 목록 변경 없음")
        fun `given empty list to remove, when removePendingBoardIds, then makes no changes`() {
            val pendingKey = RedisCounterRepository.PENDING_UPDATES
            redisTemplate.opsForSet().add(pendingKey, "1", "2")

            redisCounterRepository.removePendingBoardIds(emptyList())

            val remainingIds = redisTemplate.opsForSet().members(pendingKey)
            assertEquals(2, remainingIds?.size)
        }

        @Test
        @DisplayName("성공: 존재하지 않는 ID 제거 시도 시 오류 없음")
        fun `given non-existent IDs, when removePendingBoardIds, then completes without error`() {
            val pendingKey = RedisCounterRepository.PENDING_UPDATES
            redisTemplate.opsForSet().add(pendingKey, "1", "2")

            redisCounterRepository.removePendingBoardIds(listOf("3", "4"))

            val remainingIds = redisTemplate.opsForSet().members(pendingKey)
            assertEquals(2, remainingIds?.size)
        }
    }
}
