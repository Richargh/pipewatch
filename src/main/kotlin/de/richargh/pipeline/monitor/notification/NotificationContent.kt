package de.richargh.pipeline.monitor.notification

data class NotificationContent(
    val title: String,
    val body: String,
    val url: String? = null,
)
