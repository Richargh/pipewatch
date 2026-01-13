package de.richargh.pipewatch.gitlab.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: Long,
    val name: String,
    val path: String,
    @SerialName("path_with_namespace")
    val pathWithNamespace: String,
    val description: String? = null,
    @SerialName("web_url")
    val webUrl: String? = null,
    @SerialName("default_branch")
    val defaultBranch: String? = null,
    val visibility: String? = null,
    val archived: Boolean = false,
)
