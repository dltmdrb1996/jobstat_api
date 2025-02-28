package com.example.jobstat.auth.user

object UserConstants {
    // 사용자 제한 관련
    const val MAX_USERNAME_LENGTH = 15
    const val MIN_USERNAME_LENGTH = 3
    const val MAX_EMAIL_LENGTH = 100
    const val MAX_PASSWORD_LENGTH = 60 // bcrypt 인코딩된 비밀번호 길이
    const val MIN_PASSWORD_LENGTH = 8

    // 로그인 시도 관련
    const val MAX_LOGIN_ATTEMPTS = 5
    const val LOGIN_LOCK_DURATION_MINUTES = 30
    const val LOGIN_ATTEMPTS_CACHE_NAME = "loginAttempts"

    // Role 관련
    const val MAX_ROLE_NAME_LENGTH = 50

    // 정규식 패턴
    object Patterns {
        const val USERNAME_PATTERN = "^[가-힣a-zA-Z0-9]{3,15}$"
        const val PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$"
        const val EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    }

    object ErrorMessages {
        const val INVALID_USERNAME = "유효하지 않은 사용자 이름입니다"
        const val INVALID_EMAIL = "유효하지 않은 이메일 주소입니다"
        const val INVALID_BIRTH_DATE = "유효하지 않은 생년월일입니다"
        const val PASSWORD_REQUIRED = "패스워드는 필수 값입니다"
        const val INVALID_PASSWORD = "비밀번호는 최소 8자 이상이며, 문자, 숫자, 특수문자를 포함해야 합니다"
        const val CURRENT_PASSWORD_MISMATCH = "현재 비밀번호가 일치하지 않습니다"
        const val DUPLICATE_EMAIL = "이미 사용중인 이메일입니다"
        const val DUPLICATE_USERNAME = "이미 사용중인 아이디입니다"
        const val ACCOUNT_LOCKED = "너무 많은 로그인 시도가 있었습니다. 잠시 후 다시 시도해주세요"
        const val ACCOUNT_DISABLED = "비활성화된 계정입니다"
        const val AUTHENTICATION_FAILURE = "인증에 실패했습니다"
        const val INVALID_ROLE = "UserRole은 현재 역할에 속해있어야 합니다"
    }
}
