package de.richargh.pipewatch.util

import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object TimeFormatter {
    fun formatRelativeTime(timestamp: String?): String {
        if (timestamp == null) return "Unknown"

        return try {
            val instant = parseTimestamp(timestamp)
            val now = Instant.now()
            val duration = Duration.between(instant, now)

            formatDuration(duration)
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun formatDuration(duration: Duration): String {
        val seconds = duration.seconds
        return when {
            seconds < 0 -> "just now"
            seconds < 60 -> "just now"
            seconds < 120 -> "1 minute ago"
            seconds < 3600 -> "${seconds / 60} minutes ago"
            seconds < 7200 -> "1 hour ago"
            seconds < 86400 -> "${seconds / 3600} hours ago"
            seconds < 172800 -> "1 day ago"
            else -> "${seconds / 86400} days ago"
        }
    }

    private fun parseTimestamp(timestamp: String): Instant {
        return try {
            Instant.parse(timestamp)
        } catch (e: DateTimeParseException) {
            try {
                ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME).toInstant()
            } catch (e2: DateTimeParseException) {
                ZonedDateTime.parse(timestamp).toInstant()
            }
        }
    }
}
