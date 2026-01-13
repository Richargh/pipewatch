package de.richargh.pipeline.monitor.error

import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BackoffStrategy(
    private val initialDelay: Duration = 1.seconds,
    private val maxDelay: Duration = 5.minutes,
    private val multiplier: Double = 2.0,
) {
    private var attemptCount = 0

    val hasDelay: Boolean
        get() = attemptCount > 0

    fun nextDelay(): Duration {
        val delay =
            (initialDelay.inWholeMilliseconds * multiplier.pow(attemptCount.toDouble()))
                .toLong()
                .coerceAtMost(maxDelay.inWholeMilliseconds)

        attemptCount++

        return min(delay, maxDelay.inWholeMilliseconds).let { Duration.parse("${it}ms") }
    }

    fun reset() {
        attemptCount = 0
    }
}
