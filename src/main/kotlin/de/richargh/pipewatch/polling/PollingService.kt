package de.richargh.pipewatch.polling

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

class PollingService(
    interval: Duration,
) {
    var interval: Duration = interval
        private set

    var isPolling: Boolean = false
        private set

    suspend fun start(action: suspend () -> Unit) {
        isPolling = true
        try {
            while (coroutineContext.isActive && isPolling) {
                action()
                delay(interval)
            }
        } finally {
            isPolling = false
        }
    }

    fun stop() {
        isPolling = false
    }

    fun setInterval(newInterval: Duration) {
        interval = newInterval
    }
}
