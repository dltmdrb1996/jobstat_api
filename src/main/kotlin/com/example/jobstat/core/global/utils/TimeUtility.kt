package com.example.jobstat.core.global.utils

object TimeUtility {
    fun durationInSeconds(startTime: Long): CustomDuration = CustomDuration(startTime)

    private fun formatDuration(duration: Double): String = String.format("%.3f", duration)

    class CustomDuration(
        startTime: Long,
    ) {
        private val durationInSeconds = (System.nanoTime() - startTime) / 1000000000.0

        override fun toString(): String = formatDuration(durationInSeconds)
    }
}
