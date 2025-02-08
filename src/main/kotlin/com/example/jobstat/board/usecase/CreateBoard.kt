import com.example.jobstat.board.internal.model.BoardType
import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service

@Service
internal class CreateBoard(
    private val boardService: BoardService,
    private val bcryptPasswordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<CreateBoard.Request, CreateBoard.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val encodedPassword =
            when (request.boardOption) {
                is BoardOption.Guest -> bcryptPasswordUtil.encode(request.boardOption.password)
                is BoardOption.Member -> null
                else -> throw AppException.fromErrorCode(ErrorCode.INVALID_ARGUMENT, "게시글 작성자 정보가 올바르지 않습니다")
            }

        val userId =
            when (request.boardOption) {
                is BoardOption.Guest -> null
                is BoardOption.Member -> request.boardOption.userId
                else -> throw AppException.fromErrorCode(ErrorCode.INVALID_ARGUMENT, "게시글 작성자 정보가 올바르지 않습니다")
            }

        val board =
            boardService.createBoard(
                title = request.title,
                content = request.content,
                author = request.author,
                categoryId = request.categoryId,
                password = encodedPassword,
                userId = userId,
            )

        return Response(
            id = board.id,
            title = board.title,
            createdAt = board.createdAt.toString(),
            type = request.boardOption.type,
        )
    }

    sealed class BoardOption {
        abstract val type: BoardType

        data class Guest(
            @field:NotBlank
            @field:Size(min = 4, max = 20, message = "비밀번호는 4자 이상 20자 이하로 입력해주세요")
            val password: String,
        ) : BoardOption() {
            override val type = BoardType.GUEST
        }

        data class Member(
            @field:NotNull
            val userId: Long,
        ) : BoardOption() {
            override val type = BoardType.MEMBER
        }
    }

    data class Request(
        @field:NotBlank
        @field:Size(max = 100)
        val title: String,
        @field:NotBlank
        @field:Size(max = 5000)
        val content: String,
        @field:NotBlank
        val author: String,
        @field:NotNull
        val categoryId: Long,
        @field:Valid
        val boardOption: BoardOption,
    )

    data class Response(
        val id: Long,
        val title: String,
        val createdAt: String,
        val type: BoardType,
    )
}
