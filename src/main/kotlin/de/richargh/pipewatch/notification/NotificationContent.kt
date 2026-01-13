package de.richargh.pipewatch.notification

data class NotificationContent(
    val title: String,
    val body: String,
    val url: String? = null,
)
