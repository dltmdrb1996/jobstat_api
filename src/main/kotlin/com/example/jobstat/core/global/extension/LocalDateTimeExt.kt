package com.example.jobstat.core.global.extension

import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * LocalDateTime을 epoch 밀리초로 변환
 */
fun LocalDateTime.toEpochMilli(): Long = this.toInstant(ZoneOffset.UTC).toEpochMilli()

/**
 * LocalDateTime을 지정된 포맷의 문자열로 변환
 */
fun LocalDateTime.format(pattern: String = "yyyy-MM-dd HH:mm:ss"): String =
    java.time.format.DateTimeFormatter
        .ofPattern(pattern)
        .format(this)
