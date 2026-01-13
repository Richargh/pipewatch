package de.richargh.pipewatch.gitlab.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Job(
    val id: Long,
    val name: String,
    val stage: String,
    val status: String,
    @SerialName("web_url")
    val webUrl: String? = null,
    @SerialName("failure_reason")
    val failureReason: String? = null,
    @SerialName("allow_failure")
    val allowFailure: Boolean = false,
    val duration: Double? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("finished_at")
    val finishedAt: String? = null,
)

fun List<Job>.getFailedJobsByStage(): Map<String, List<Job>> {
    return this
        .filter { it.status == "failed" && !it.allowFailure }
        .groupBy { it.stage }
}
