package com.example.jobstat.auth

import com.example.jobstat.community.internal.repository.BoardRepository
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.wrapper.ApiResponse
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/test/users")
@Public
internal class TestUserController(
    private val boardRepository: BoardRepository
) {
    private val random = Random()

    @GetMapping("/{userId}/profile")
    fun getUserProfile(
        @PathVariable userId: Long,
    ): ResponseEntity<ApiResponse<User>> {
        val user =
            User(
                id = userId,
                name = "테스트유저_${random.nextInt(100)}",
                email = "test${random.nextInt(100)}@example.com",
                profileImage = "https://via.placeholder.com/${150 + random.nextInt(50)}",
                bio = "안녕하세요! 테스트 프로필_${random.nextInt(100)}입니다.",
            )
        return ApiResponse.ok(user)
    }

    @GetMapping("/{userId}/posts")
    fun getUserPosts(
        @PathVariable userId: Long,
    ): ResponseEntity<ApiResponse<List<Post>>> {
        val board = boardRepository.findById(5)
        val posts =
            (1..5).map {
                Post(
                    id = random.nextLong(),
                    userId = userId,
                    title = board.title,
                    content = board.content,
                    createdAt = LocalDateTime.now().minusDays(random.nextLong(30)),
                )
            }
        return ApiResponse.ok(posts)
    }

    @GetMapping("/{userId}/activities")
    fun getUserActivities(
        @PathVariable userId: Long,
    ): ResponseEntity<ApiResponse<List<UserActivity>>> {
        val activities =
            (1..3).map {
                UserActivity(
                    id = random.nextLong(),
                    userId = userId,
                    type = ActivityType.values()[random.nextInt(ActivityType.values().size)],
                    description = "활동내역_${random.nextInt(100)}",
                    timestamp = LocalDateTime.now().minusHours(random.nextLong(24)),
                )
            }
        return ApiResponse.ok(activities)
    }

    enum class ActivityType {
        POST_CREATED, // 게시물 작성됨
        COMMENT_ADDED, // 댓글 추가됨
        PROFILE_UPDATED, // 프로필 업데이트됨
    }

    data class User(
        val id: Long,
        val name: String,
        val email: String,
        val profileImage: String,
        val bio: String,
    )

    data class Post(
        val id: Long,
        val userId: Long,
        val title: String,
        val content: String,
        val createdAt: LocalDateTime,
    )

    data class UserActivity(
        val id: Long,
        val userId: Long,
        val type: ActivityType,
        val description: String,
        val timestamp: LocalDateTime,
    )
}
