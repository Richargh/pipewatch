package de.richargh.pipeline.monitor.gitlab.api

class GitLabConfig(
    baseUrl: String,
    val accessToken: String,
) {
    val baseUrl: String = baseUrl.trimEnd('/')

    init {
        require(this.baseUrl.isNotBlank()) { "Base URL cannot be blank" }
        require(accessToken.isNotBlank()) { "Access token cannot be blank" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GitLabConfig) return false
        return baseUrl == other.baseUrl && accessToken == other.accessToken
    }

    override fun hashCode(): Int {
        var result = baseUrl.hashCode()
        result = 31 * result + accessToken.hashCode()
        return result
    }

    override fun toString(): String = "GitLabConfig(baseUrl='$baseUrl', accessToken='***')"
}
