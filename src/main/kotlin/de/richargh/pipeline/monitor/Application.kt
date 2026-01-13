package de.richargh.pipeline.monitor

class Application {
    var isRunning: Boolean = false
        private set

    var onShutdown: (() -> Unit)? = null
    var onCleanup: (() -> Unit)? = null

    fun start() {
        isRunning = true
    }

    fun stop() {
        onCleanup?.invoke()
        onShutdown?.invoke()
        isRunning = false
    }
}
