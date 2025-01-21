// package com.example.jobstat.core.error
//
// /**
// * 애플리케이션에서 발생할 수 있는 다양한 오류 상황을 나타내는 열거형입니다.
// *
// * 오류 코드는 다음과 같은 구조를 가집니다:
// * - 첫 두 글자: 오류의 대분류 (CE: 클라이언트 오류, SE: 서버 오류, EE: 외부 서비스 오류)
// * - 숫자 3자리: 세부 오류 코드
// *   - 001-099: 일반적인 오류
// *   - 100-199: 인증/인가 관련 오류
// *   - 200-299: 입력 데이터 관련 오류
// *   - 300-399: 리소스 관련 오류
// *   - 400-499: 데이터베이스 관련 오류
// *   - 500-599: 비즈니스 로직 관련 오류
// *   - 600-999: 기타 및 확장을 위한 여유 공간
// *
// * @property code 오류 코드 문자열
// * @property message 오류에 대한 기본 메시지
// */
// enum class ErrorCode(val code: String, val message: String) {
//        MISSING_PARAMETER("CE201", "필수 매개변수가 누락되었습니다"),
//        INVALID_ARGUMENT("CE202", "유효하지 않은 인자가 제공되었습니다"),
//        RESOURCE_NOT_FOUND("CE301", "요청한 리소스를 찾을 수 없습니다"),
//        AUTHENTICATION_FAILURE("CE101", "인증에 실패했습니다"),
//        FORBIDDEN_ACCESS("CE102", "접근이 거부되었습니다"),
//        DUPLICATE_RESOURCE("CE302", "리소스가 중복되었습니다"),
//        INVALID_REQUEST_BODY("CE203", "유효하지 않은 요청 본문입니다"),
//        CONSTRAINT_VIOLATION("CE204", "제약 조건 위반이 발생했습니다"),
//        DUPLICATE_ENTRY("CE303", "중복된 항목이 존재합니다"),
//        INTERNAL_ERROR("SE001", "내부 서버 오류가 발생했습니다"),
//        DATABASE_ERROR("SE401", "데이터베이스 작업 중 오류가 발생했습니다"),
//        SQL_SYNTAX_ERROR("SE402", "SQL 구문 오류가 발생했습니다"),
//        TRANSACTION_ERROR("SE501", "트랜잭션 처리 중 오류가 발생했습니다"),
//        EXTERNAL_SERVICE_ERROR("EE001", "외부 서비스 호출 중 오류가 발생했습니다");
// }
