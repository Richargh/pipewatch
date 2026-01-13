package de.richargh.pipeline.monitor.util

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals

class TimeFormatterTest {
    @Test
    fun `formatDuration returns just now for less than 60 seconds`() {
        assertEquals("just now", TimeFormatter.formatDuration(Duration.ofSeconds(30)))
        assertEquals("just now", TimeFormatter.formatDuration(Duration.ofSeconds(59)))
    }

    @Test
    fun `formatDuration returns 1 minute ago for 60-119 seconds`() {
        assertEquals("1 minute ago", TimeFormatter.formatDuration(Duration.ofSeconds(60)))
        assertEquals("1 minute ago", TimeFormatter.formatDuration(Duration.ofSeconds(119)))
    }

    @Test
    fun `formatDuration returns X minutes ago for 2-59 minutes`() {
        assertEquals("2 minutes ago", TimeFormatter.formatDuration(Duration.ofMinutes(2)))
        assertEquals("30 minutes ago", TimeFormatter.formatDuration(Duration.ofMinutes(30)))
        assertEquals("59 minutes ago", TimeFormatter.formatDuration(Duration.ofMinutes(59)))
    }

    @Test
    fun `formatDuration returns 1 hour ago for 60-119 minutes`() {
        assertEquals("1 hour ago", TimeFormatter.formatDuration(Duration.ofMinutes(60)))
        assertEquals("1 hour ago", TimeFormatter.formatDuration(Duration.ofMinutes(119)))
    }

    @Test
    fun `formatDuration returns X hours ago for 2-23 hours`() {
        assertEquals("2 hours ago", TimeFormatter.formatDuration(Duration.ofHours(2)))
        assertEquals("12 hours ago", TimeFormatter.formatDuration(Duration.ofHours(12)))
        assertEquals("23 hours ago", TimeFormatter.formatDuration(Duration.ofHours(23)))
    }

    @Test
    fun `formatDuration returns 1 day ago for 24-47 hours`() {
        assertEquals("1 day ago", TimeFormatter.formatDuration(Duration.ofHours(24)))
        assertEquals("1 day ago", TimeFormatter.formatDuration(Duration.ofHours(47)))
    }

    @Test
    fun `formatDuration returns X days ago for more than 48 hours`() {
        assertEquals("2 days ago", TimeFormatter.formatDuration(Duration.ofHours(48)))
        assertEquals("7 days ago", TimeFormatter.formatDuration(Duration.ofDays(7)))
    }

    @Test
    fun `formatDuration handles negative duration as just now`() {
        assertEquals("just now", TimeFormatter.formatDuration(Duration.ofSeconds(-10)))
    }

    @Test
    fun `formatRelativeTime returns Unknown for null`() {
        assertEquals("Unknown", TimeFormatter.formatRelativeTime(null))
    }

    @Test
    fun `formatRelativeTime handles invalid timestamp`() {
        assertEquals("Unknown", TimeFormatter.formatRelativeTime("not-a-timestamp"))
    }
}
