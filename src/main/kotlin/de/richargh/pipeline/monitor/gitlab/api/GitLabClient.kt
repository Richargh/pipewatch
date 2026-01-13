package de.richargh.pipeline.monitor.gitlab.api

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

    fun close() {
        httpClient.close()
    }
}
