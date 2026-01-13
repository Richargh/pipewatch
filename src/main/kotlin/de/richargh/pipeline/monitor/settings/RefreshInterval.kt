package de.richargh.pipeline.monitor.settings

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class RefreshInterval(
    val duration: Duration,
    val displayName: String,
) {
    THIRTY_SECONDS(30.seconds, "30 seconds"),
    ONE_MINUTE(1.minutes, "1 minute"),
    TWO_MINUTES(2.minutes, "2 minutes"),
    FIVE_MINUTES(5.minutes, "5 minutes"),
    TEN_MINUTES(10.minutes, "10 minutes"),
    ;

    companion object {
        val DEFAULT = ONE_MINUTE

        fun fromSeconds(seconds: Int): RefreshInterval {
            return entries.find { it.duration.inWholeSeconds.toInt() == seconds } ?: DEFAULT
        }
    }
}
