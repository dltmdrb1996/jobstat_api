package com.example.jobstat.eacheach

import java.time.LocalDateTime
import java.time.ZoneOffset

fun LocalDateTime.toEpochMilli(): Long = this.toInstant(ZoneOffset.UTC).toEpochMilli()

fun LocalDateTime.format(pattern: String = "yyyy-MM-dd HH:mm:ss"): String =
    java.time.format.DateTimeFormatter
        .ofPattern(pattern)
        .format(this)
