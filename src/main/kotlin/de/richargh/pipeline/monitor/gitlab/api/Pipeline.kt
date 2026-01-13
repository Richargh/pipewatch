package de.richargh.pipeline.monitor.gitlab.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Pipeline(
    val id: Long,
    val iid: Long,
    val status: String,
    val ref: String,
    val sha: String,
    @SerialName("web_url")
    val webUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)
