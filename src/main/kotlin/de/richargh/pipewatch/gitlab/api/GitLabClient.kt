package de.richargh.pipewatch.gitlab.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class GitLabClient(
    val config: GitLabConfig,
    engine: HttpClientEngine? = null,
) {
    constructor(
        baseUrl: String,
        accessToken: String,
        engine: HttpClientEngine? = null,
    ) : this(GitLabConfig(baseUrl, accessToken), engine)

    internal val httpClient: HttpClient =
        HttpClient(engine ?: CIO.create()) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }

            defaultRequest {
                header("PRIVATE-TOKEN", config.accessToken)
            }
        }

    suspend fun getLatestPipeline(
        projectId: Long,
        ref: String? = null,
    ): Pipeline? {
        return try {
            val response: HttpResponse =
                httpClient.get("${config.baseUrl}/api/v4/projects/$projectId/pipelines") {
                    parameter("per_page", 1)
                    ref?.let { parameter("ref", it) }
                }

            if (!response.status.isSuccess()) {
                return null
            }

            val pipelines: List<Pipeline> = response.body()
            pipelines.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getProjects(
        search: String? = null,
        membership: Boolean = true,
        page: Int = 1,
        perPage: Int = 20,
    ): List<Project> {
        return try {
            val response: HttpResponse =
                httpClient.get("${config.baseUrl}/api/v4/projects") {
                    parameter("membership", membership)
                    parameter("page", page)
                    parameter("per_page", perPage)
                    search?.let { parameter("search", it) }
                }

            if (!response.status.isSuccess()) {
                return emptyList()
            }

            response.body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getProject(projectId: Long): Project? {
        return try {
            val response: HttpResponse =
                httpClient.get("${config.baseUrl}/api/v4/projects/$projectId")

            if (!response.status.isSuccess()) {
                return null
            }

            response.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getProjectByPath(projectPath: String): Project? {
        return try {
            val encodedPath = java.net.URLEncoder.encode(projectPath, "UTF-8")
            val response: HttpResponse =
                httpClient.get("${config.baseUrl}/api/v4/projects/$encodedPath")

            if (!response.status.isSuccess()) {
                return null
            }

            response.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPipeline(
        projectId: Long,
        pipelineId: Long,
    ): Pipeline? {
        return try {
            val response: HttpResponse =
                httpClient.get("${config.baseUrl}/api/v4/projects/$projectId/pipelines/$pipelineId")

            if (!response.status.isSuccess()) {
                return null
            }

            response.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPipelineJobs(
        projectId: Long,
        pipelineId: Long,
    ): List<Job> {
        return try {
            val response: HttpResponse =
                httpClient.get("${config.baseUrl}/api/v4/projects/$projectId/pipelines/$pipelineId/jobs")

            if (!response.status.isSuccess()) {
                return emptyList()
            }

            response.body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getProjectOrThrow(projectId: Long): Project {
        return executeOrThrow {
            httpClient.get("${config.baseUrl}/api/v4/projects/$projectId")
        }
    }

    suspend fun getPipelineOrThrow(
        projectId: Long,
        pipelineId: Long,
    ): Pipeline {
        return executeOrThrow {
            httpClient.get("${config.baseUrl}/api/v4/projects/$projectId/pipelines/$pipelineId")
        }
    }

    suspend fun getPipelineJobsOrThrow(
        projectId: Long,
        pipelineId: Long,
    ): List<Job> {
        return executeOrThrow {
            httpClient.get("${config.baseUrl}/api/v4/projects/$projectId/pipelines/$pipelineId/jobs")
        }
    }

    suspend fun testConnection(): Result<Unit> {
        return try {
            val response: HttpResponse =
                httpClient.get("${config.baseUrl}/api/v4/user")

            if (!response.status.isSuccess()) {
                Result.failure(mapStatusToException(response.status.value, "Connection test failed"))
            } else {
                Result.success(Unit)
            }
        } catch (e: GitLabApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(GitLabApiException.NetworkError("Failed to connect to GitLab", e))
        }
    }

    private suspend inline fun <reified T> executeOrThrow(request: () -> HttpResponse): T {
        try {
            val response = request()

            if (!response.status.isSuccess()) {
                throw mapStatusToException(response.status.value, response.status.description)
            }

            return response.body()
        } catch (e: GitLabApiException) {
            throw e
        } catch (e: Exception) {
            throw GitLabApiException.NetworkError("Network request failed", e)
        }
    }

    private fun mapStatusToException(
        statusCode: Int,
        message: String,
    ): GitLabApiException {
        return when (statusCode) {
            401 -> GitLabApiException.Unauthorized(message)
            403 -> GitLabApiException.Forbidden(message)
            404 -> GitLabApiException.NotFound(message)
            in 500..599 -> GitLabApiException.ServerError(message, statusCode)
            else -> GitLabApiException.Unknown(message, statusCode)
        }
    }

    fun close() {
        httpClient.close()
    }
}
