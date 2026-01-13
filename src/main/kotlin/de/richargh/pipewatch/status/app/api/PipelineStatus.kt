package de.richargh.pipewatch.status.app.api

import java.awt.Color

enum class PipelineStatus(val displayName: String, val color: Color) {
    SUCCESS("Success", Color(76, 175, 80)),
    FAILED("Failed", Color(244, 67, 54)),
    RUNNING("Running", Color(255, 193, 7)),
    PENDING("Pending", Color(255, 193, 7)),
    CANCELED("Canceled", Color(158, 158, 158)),
    UNKNOWN("Unknown", Color(128, 128, 128)),
    ;

    companion object {
        fun fromString(status: String?): PipelineStatus {
            return when (status?.lowercase()) {
                "success" -> SUCCESS
                "failed" -> FAILED
                "running" -> RUNNING
                "pending" -> PENDING
                "canceled", "cancelled" -> CANCELED
                else -> UNKNOWN
            }
        }
    }
}
