package com.example.jobstat.core.error

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val code: String,
    val defaultMessage: String,
    val defaultHttpStatus: HttpStatus,
    val type: AppExceptionType,
) {
    // 클라이언트 오류
    MISSING_PARAMETER("C001", "필수 매개변수가 누락되었습니다", HttpStatus.BAD_REQUEST, AppExceptionType.CLIENT_ERROR),
    INVALID_ARGUMENT("C002", "유효하지 않은 인자가 제공되었습니다", HttpStatus.BAD_REQUEST, AppExceptionType.CLIENT_ERROR),
    RESOURCE_NOT_FOUND("C003", "리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND, AppExceptionType.CLIENT_ERROR),
    AUTHENTICATION_FAILURE("C004", "인증에 실패했습니다", HttpStatus.UNAUTHORIZED, AppExceptionType.CLIENT_ERROR),
    FORBIDDEN_ACCESS("C005", "접근이 금지되었습니다", HttpStatus.FORBIDDEN, AppExceptionType.CLIENT_ERROR),
    DUPLICATE_RESOURCE("C006", "중복된 리소스입니다", HttpStatus.CONFLICT, AppExceptionType.CLIENT_ERROR),
    INVALID_REQUEST_BODY("C007", "유효하지 않은 요청 본문입니다", HttpStatus.BAD_REQUEST, AppExceptionType.CLIENT_ERROR),
    CONSTRAINT_VIOLATION("C008", "제약 조건 위반입니다", HttpStatus.BAD_REQUEST, AppExceptionType.CLIENT_ERROR),
    TOO_MANY_REQUESTS("C009", "요청이 너무 많습니다", HttpStatus.TOO_MANY_REQUESTS, AppExceptionType.CLIENT_ERROR),
    VERIFICATION_CODE_ALREADY_SENT("C010", "이미 발송된 인증 코드가 있습니다. 잠시 후 다시 시도해주세요.", HttpStatus.BAD_REQUEST, AppExceptionType.CLIENT_ERROR),
    VERIFICATION_NOT_FOUND("C011", "인증 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND, AppExceptionType.CLIENT_ERROR),
    VERIFICATION_EXPIRED("C012", "만료된 인증 코드입니다.", HttpStatus.BAD_REQUEST, AppExceptionType.CLIENT_ERROR),
    INVALID_VERIFICATION_CODE("C013", "잘못된 인증 코드입니다.", HttpStatus.BAD_REQUEST, AppExceptionType.CLIENT_ERROR),
    ACCOUNT_DISABLED("C014", "비활성화된 계정입니다.", HttpStatus.FORBIDDEN, AppExceptionType.CLIENT_ERROR),
    INSUFFICIENT_PERMISSION("C015", "권한이 부족합니다.", HttpStatus.FORBIDDEN, AppExceptionType.CLIENT_ERROR),
    ADMIN_ACCESS_REQUIRED("C016", "관리자 권한이 필요합니다", HttpStatus.FORBIDDEN, AppExceptionType.CLIENT_ERROR),
    TOKEN_INVALID("C017", "인증 토큰 검증에 실패했습니다", HttpStatus.UNAUTHORIZED, AppExceptionType.CLIENT_ERROR),
    INVALID_SORT_TYPE("C018", "잘못된 정렬 타입입니다", HttpStatus.BAD_REQUEST, AppExceptionType.CLIENT_ERROR),
    INVALID_OPERATION("C019", "유효하지 않은 작업입니다", HttpStatus.BAD_REQUEST, AppExceptionType.CLIENT_ERROR),

    // 서버 오류
    INTERNAL_ERROR("S001", "내부 서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    DATABASE_ERROR("S002", "데이터베이스 작업에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    EXTERNAL_SERVICE_ERROR("S003", "외부 서비스 호출에 실패했습니다", HttpStatus.SERVICE_UNAVAILABLE, AppExceptionType.SERVER_ERROR),
    SQL_SYNTAX_ERROR("S004", "SQL 구문 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    TRANSACTION_ERROR("S005", "트랜잭션 처리 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    EMAIL_SENDING_FAILURE("S006", "이메일 발송에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    SERIALIZATION_FAILURE("S007", "직렬화에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    REDIS_OPERATION_FAILED("S008", "Redis 작업 실패", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    CACHE_OPERATION_FAILED("S009", "캐시 작업 실패", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    VIEW_COUNT_UPDATE_FAILED("S010", "조회수 업데이트 실패", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    LIKE_COUNT_UPDATE_FAILED("S011", "좋아요 수 업데이트 실패", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    COMMENT_COUNT_UPDATE_FAILED("S012", "댓글 수 업데이트 실패", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    CACHE_INVALIDATION_FAILED("S013", "캐시 무효화 실패", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    INVALID_RESPONSE("S014", "유효하지 않은 응답입니다", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
}
