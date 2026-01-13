package de.richargh.pipeline.monitor.gitlab.api

sealed class GitLabApiException(
    override val message: String,
    val statusCode: Int,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    class Unauthorized(message: String = "Invalid or expired access token") :
        GitLabApiException(message, 401)

    class Forbidden(message: String = "Access denied to this resource") :
        GitLabApiException(message, 403)

    class NotFound(message: String = "Resource not found") :
        GitLabApiException(message, 404)

    class ServerError(message: String = "GitLab server error", statusCode: Int = 500) :
        GitLabApiException(message, statusCode)

    class NetworkError(message: String = "Network connection failed", cause: Throwable? = null) :
        GitLabApiException(message, 0, cause)

    class Unknown(message: String = "Unknown error occurred", statusCode: Int = 0) :
        GitLabApiException(message, statusCode)
}
