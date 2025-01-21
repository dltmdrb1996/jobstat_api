package com.example.jobstat.core.state

enum class ExperienceLevel {
    NEWBIE,     // 신입 (문자열에 "신입"이 있으면 우선적으로 여기에 매핑)
    FRESHER,    // 0 ~ 1년 미만
    JUNIOR,     // 1 ~ 3년 미만
    MID,        // 3 ~ 6년 미만
    MID_HIGH,   // 6 ~ 8년 미만
    SENIOR,     // 8 ~ 12년 미만
    EXPERT,     // 12년 이상
    UNKNOWN     // 파싱 실패 또는 범위 밖 등
}



