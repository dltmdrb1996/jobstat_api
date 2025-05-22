package com.wildrew.app.eacheach

import kotlinx.coroutines.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.web.servlet.function.ServerRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

fun Map<*, *>.toJsonString(): String {
    val sb = StringBuilder()
    sb.append("{")
    this.forEach { (key, value) ->
        sb.append("\"$key\":\"$value\",")
    }
    sb.deleteCharAt(sb.length - 1)
    sb.append("}")
    return sb.toString()
}

// boolean extension
fun Boolean.trueOrThrow(exceptionSupplier: () -> Exception): Boolean {
    if (!this) {
        throw exceptionSupplier()
    }
    return true
}

fun Boolean.falseOrThrow(exceptionSupplier: () -> Exception): Boolean {
    if (this) {
        throw exceptionSupplier()
    }
    return false
}

fun <T> T?.requireNotNullOrEmpty(exceptionSupplier: () -> Exception): T {
    if (this == null || this.toString().isEmpty()) {
        throw exceptionSupplier()
    }
    return this
}

fun <T> T?.requireNotNullOrBlank(exceptionSupplier: () -> Exception): T {
    if (this == null || this.toString().isBlank()) {
        throw exceptionSupplier()
    }
    return this
}

fun <T> T?.requireNotNullOrZero(exceptionSupplier: () -> Exception): T {
    if (this == null || this.toString() == "0") {
        throw exceptionSupplier()
    }
    return this
}

fun <T> T?.requireNotNullOrFalse(exceptionSupplier: () -> Exception): T {
    if (this == null || this.toString() == "false") {
        throw exceptionSupplier()
    }
    return this
}

fun Date.add(
    field: Int,
    amount: Int,
): Date {
    Calendar.getInstance().apply {
        time = this@add
        add(field, amount)
        return time
    }
}

fun LocalDate?.toDate(): Date? =
    when {
        this != null ->
            try {
                Date.from(atStartOfDay(ZoneId.systemDefault()).toInstant())
            } catch (e: Exception) {
                null
            }
        else -> null
    }

fun LocalDateTime?.toDate(): Date? =
    when {
        this != null ->
            try {
                Date.from(atZone(ZoneId.systemDefault()).toInstant())
            } catch (e: Exception) {
                null
            }
        else -> null
    }

fun ServerRequest.locale(): Locale =
    this
        .headers()
        .asHttpHeaders()
        .acceptLanguageAsLocales
        .firstOrNull() ?: Locale.ENGLISH

fun LocalDate.formatDate(): String = this.format(englishDateFormatter)

private val daysLookup: Map<Long, String> =
    IntStream.rangeClosed(1, 31).boxed().collect(Collectors.toMap(Int::toLong, ::getOrdinal))

private val englishDateFormatter =
    DateTimeFormatterBuilder()
        .appendPattern("MMMM")
        .appendLiteral(" ")
        .appendText(ChronoField.DAY_OF_MONTH, daysLookup)
        .appendLiteral(" ")
        .appendPattern("yyyy")
        .toFormatter(Locale.ENGLISH)

private fun getOrdinal(n: Int) =
    when {
        n in 11..13 -> "${n}th"
        n % 10 == 1 -> "${n}st"
        n % 10 == 2 -> "${n}nd"
        n % 10 == 3 -> "${n}rd"
        else -> "${n}th"
    }

suspend fun <T> withRetry(
    maxAttempts: Int = 3,
    initialDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> T,
): T {
    var currentDelay = initialDelay
    repeat(maxAttempts) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == maxAttempts - 1) throw e
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
    }
    throw IllegalStateException("This line should never be reached")
}

fun <T, R> Page<T>.map(converter: (T) -> R): Page<R> =
    PageImpl(
        content.map(converter),
        pageable,
        totalElements,
    )
