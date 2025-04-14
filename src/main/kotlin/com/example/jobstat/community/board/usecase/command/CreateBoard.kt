package com.example.jobstat.community.board.usecase.command

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.utils.BoardConstants
import com.example.jobstat.community.event.CommunityCommandEventPublisher
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.SecurityUtils
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 게시글 생성 유스케이스
 * - 로그인 사용자: 사용자 정보로 게시글 생성
 * - 비로그인 사용자: 비밀번호 설정 후 게시글 생성
 */
@Service
internal class CreateBoard(
    private val boardService: BoardService,
    private val securityUtils: SecurityUtils,
    private val passwordUtil: PasswordUtil,
    private val communityCommandEventPublisher: CommunityCommandEventPublisher,
    validator: Validator,
) : ValidUseCase<CreateBoard.Request, CreateBoard.Response>(validator) {
    /**
     * 트랜잭션 내에서 요청 처리
     */
    @Transactional
    override fun invoke(request: Request): Response = super.invoke(request)

    override fun execute(request: Request): Response =
        run {
            val userId = securityUtils.getCurrentUserId()

            // 로그인 상태가 아니면서 비밀번호가 없는 경우 예외 처리
            validatePasswordIfNotLoggedIn(userId, request.password)

            // 비밀번호 처리 (로그인 시 null, 비로그인 시 암호화)
            val encodedPassword = processPassword(userId, request.password)

            val createdBoard =
                boardService.createBoard(
                    title = request.title,
                    content = request.content,
                    author = request.author,
                    categoryId = request.categoryId,
                    password = encodedPassword,
                    userId = userId,
                )

            // 게시글 생성 이벤트 발행
            communityCommandEventPublisher.publishBoardCreated(
                board = createdBoard,
                categoryId = request.categoryId,
                userId = userId,
            )

            return Response.from(createdBoard)
        }

    private fun validatePasswordIfNotLoggedIn(
        userId: Long?,
        password: String?,
    ) {
        if (userId == null && password == null) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비로그인 상태에서는 비밀번호 설정이 필수입니다",
            )
        }
    }

    private fun processPassword(
        userId: Long?,
        password: String?,
    ): String? = userId?.let { null } ?: password?.let { passwordUtil.encode(it) }

    @Schema(
        name = "CreateBoardRequest",
        description = "게시글 생성 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "게시글 제목",
            example = "안녕하세요, 첫 게시글입니다",
            minLength = BoardConstants.MIN_TITLE_LENGTH,
            maxLength = BoardConstants.MAX_TITLE_LENGTH,
            required = true,
        )
        @field:Size(
            min = BoardConstants.MIN_TITLE_LENGTH,
            max = BoardConstants.MAX_TITLE_LENGTH,
            message = "제목은 ${BoardConstants.MIN_TITLE_LENGTH}~${BoardConstants.MAX_TITLE_LENGTH}자 사이여야 합니다",
        )
        val title: String,
        @field:Schema(
            description = "게시글 내용",
            example = "게시글 내용입니다. 여기에 자세한 내용을 작성합니다.",
            minLength = BoardConstants.MIN_CONTENT_LENGTH,
            maxLength = BoardConstants.MAX_CONTENT_LENGTH,
            required = true,
        )
        @field:Size(
            min = BoardConstants.MIN_CONTENT_LENGTH,
            max = BoardConstants.MAX_CONTENT_LENGTH,
            message = "내용은 ${BoardConstants.MIN_CONTENT_LENGTH}~${BoardConstants.MAX_CONTENT_LENGTH}자 사이여야 합니다",
        )
        val content: String,
        @field:Schema(
            description = "작성자명",
            example = "홍길동",
            required = true,
        )
        @field:NotBlank(message = "작성자명은 필수입니다")
        val author: String,
        @field:Schema(
            description = "카테고리 ID",
            example = "1",
            required = true,
        )
        @field:NotNull(message = "카테고리 ID는 필수입니다")
        val categoryId: Long,
        @field:Schema(
            description = "비밀번호 (비회원 작성 시 필수)",
            example = "password1234",
            nullable = true,
            minLength = BoardConstants.MIN_PASSWORD_LENGTH,
            maxLength = BoardConstants.MAX_PASSWORD_LENGTH,
        )
        @field:Size(
            min = BoardConstants.MIN_PASSWORD_LENGTH,
            max = BoardConstants.MAX_PASSWORD_LENGTH,
            message = "비밀번호는 ${BoardConstants.MIN_PASSWORD_LENGTH}~${BoardConstants.MAX_PASSWORD_LENGTH}자 사이여야 합니다",
        )
        val password: String?,
    )

    /**
     * 게시글 생성 응답 모델
     */
    @Schema(
        name = "CreateBoardResponse",
        description = "게시글 생성 응답 모델",
    )
    data class Response(
        @field:Schema(
            description = "생성된 게시글 ID",
            example = "1",
        )
        val id: String,
        @field:Schema(
            description = "게시글 제목",
            example = "안녕하세요, 첫 게시글입니다",
        )
        val title: String,
        @field:Schema(
            description = "생성 일시",
            example = "2023-05-10T14:30:15.123456",
        )
        val createdAt: String,
    ) {
        companion object {
            fun from(board: Board): Response =
                with(board) {
                    Response(
                        id = id.toString(),
                        title = title,
                        createdAt = createdAt.toString(),
                    )
                }
        }
    }
}
