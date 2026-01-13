package de.richargh.pipeline.monitor.settings.multiproject

import kotlinx.serialization.Serializable

@Serializable
data class TokenConfig(
    val id: String,
    val gitLabUrl: String,
    val accessToken: String
) {
    fun matchesGitLabUrl(url: String): Boolean {
        val normalizedStored = gitLabUrl.trimEnd('/').lowercase()
        val normalizedInput = url.trimEnd('/').lowercase()
        return normalizedStored == normalizedInput
    }
}
