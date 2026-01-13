package de.richargh.pipewatch.gitlab.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PipelineTest {
    @Test
    fun `Pipeline can be deserialized from GitLab API response`() {
        val json =
            """
            {
                "id": 12345,
                "iid": 100,
                "status": "success",
                "ref": "main",
                "sha": "abc123def456",
                "web_url": "https://gitlab.example.com/project/-/pipelines/12345",
                "created_at": "2024-01-15T10:30:00.000Z",
                "updated_at": "2024-01-15T10:35:00.000Z"
            }
            """.trimIndent()

        val pipeline = Json.decodeFromString<Pipeline>(json)

        assertEquals(12345L, pipeline.id)
        assertEquals(100L, pipeline.iid)
        assertEquals("success", pipeline.status)
        assertEquals("main", pipeline.ref)
        assertEquals("abc123def456", pipeline.sha)
        assertEquals("https://gitlab.example.com/project/-/pipelines/12345", pipeline.webUrl)
        assertEquals("2024-01-15T10:30:00.000Z", pipeline.createdAt)
        assertEquals("2024-01-15T10:35:00.000Z", pipeline.updatedAt)
    }

    @Test
    fun `Pipeline handles optional fields being null`() {
        val json =
            """
            {
                "id": 12345,
                "iid": 100,
                "status": "running",
                "ref": "feature-branch",
                "sha": "abc123"
            }
            """.trimIndent()

        val pipeline = Json { ignoreUnknownKeys = true }.decodeFromString<Pipeline>(json)

        assertEquals(12345L, pipeline.id)
        assertEquals("running", pipeline.status)
        assertNull(pipeline.webUrl)
        assertNull(pipeline.createdAt)
    }
}

class GitLabClientPipelineApiTest {
    private val testConfig =
        GitLabConfig(
            baseUrl = "https://gitlab.example.com",
            accessToken = "test-token",
        )

    @Test
    fun `getLatestPipeline returns pipeline when API returns data`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals(
                        "https://gitlab.example.com/api/v4/projects/123/pipelines?per_page=1",
                        request.url.toString(),
                    )
                    assertEquals("test-token", request.headers["PRIVATE-TOKEN"])

                    respond(
                        content =
                            """
                            [
                                {
                                    "id": 12345,
                                    "iid": 100,
                                    "status": "success",
                                    "ref": "main",
                                    "sha": "abc123",
                                    "web_url": "https://gitlab.example.com/project/-/pipelines/12345",
                                    "created_at": "2024-01-15T10:30:00.000Z",
                                    "updated_at": "2024-01-15T10:35:00.000Z"
                                }
                            ]
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val pipeline = client.getLatestPipeline(projectId = 123)

            assertNotNull(pipeline)
            assertEquals(12345L, pipeline.id)
            assertEquals("success", pipeline.status)
            assertEquals("main", pipeline.ref)

            client.close()
        }

    @Test
    fun `getLatestPipeline returns null when no pipelines exist`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = "[]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val pipeline = client.getLatestPipeline(projectId = 123)

            assertNull(pipeline)

            client.close()
        }

    @Test
    fun `getLatestPipeline filters by ref when provided`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertTrue(request.url.toString().contains("ref=main"))

                    respond(
                        content =
                            """
                            [
                                {
                                    "id": 12345,
                                    "iid": 100,
                                    "status": "success",
                                    "ref": "main",
                                    "sha": "abc123"
                                }
                            ]
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val pipeline = client.getLatestPipeline(projectId = 123, ref = "main")

            assertNotNull(pipeline)
            assertEquals("main", pipeline.ref)

            client.close()
        }

    @Test
    fun `getLatestPipeline handles API error gracefully`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"error": "Not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val pipeline = client.getLatestPipeline(projectId = 123)

            assertNull(pipeline)

            client.close()
        }
}
