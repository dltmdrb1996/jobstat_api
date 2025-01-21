// StructuredLogger.kt
package com.example.jobstat.core.error

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.UUID

class StructuredLogger(
    private val clazz: Class<*>,
) {
    private val logger = LoggerFactory.getLogger(clazz)

    fun error(
        message: String,
        ex: Throwable? = null,
        additionalInfo: Map<String, Any?> = emptyMap(),
    ) {
        log(LogLevel.ERROR, message, ex, additionalInfo)
    }

    fun warn(
        message: String,
        ex: Throwable? = null,
        additionalInfo: Map<String, Any?> = emptyMap(),
    ) {
        log(LogLevel.WARN, message, ex, additionalInfo)
    }

    fun info(
        message: String,
        additionalInfo: Map<String, Any?> = emptyMap(),
    ) {
        log(LogLevel.INFO, message, null, additionalInfo)
    }

    fun debug(
        message: String,
        additionalInfo: Map<String, Any?> = emptyMap(),
    ) {
        log(LogLevel.DEBUG, message, null, additionalInfo)
    }

    private fun log(
        level: LogLevel,
        message: String,
        ex: Throwable? = null,
        additionalInfo: Map<String, Any?> = emptyMap(),
    ) {
        val eventId = UUID.randomUUID().toString()
        MDC.put("event_id", eventId)
        MDC.put("class", clazz.simpleName)

        additionalInfo.forEach { (key, value) ->
            MDC.put(key, value?.toString())
        }

        val logMessage = buildLogMessage(message, additionalInfo)

        when (level) {
            LogLevel.ERROR -> logger.error(logMessage, ex)
            LogLevel.WARN -> logger.warn(logMessage, ex)
            LogLevel.INFO -> logger.info(logMessage)
            LogLevel.DEBUG -> logger.debug(logMessage)
        }

        MDC.clear()
    }

    private fun buildLogMessage(
        message: String,
        additionalInfo: Map<String, Any?>,
    ): String {
        val sb = StringBuilder(message)
        if (additionalInfo.isNotEmpty()) {
            sb.append(" | Additional Info: ")
            additionalInfo.forEach { (key, value) ->
                sb.append("$key: $value, ")
            }
            sb.setLength(sb.length - 2) // Remove last ", "
        }
        return sb.toString()
    }

    enum class LogLevel {
        ERROR,
        WARN,
        INFO,
        DEBUG,
    }
}
