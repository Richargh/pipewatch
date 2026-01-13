package de.richargh.pipewatch.viewmodel

import de.richargh.pipewatch.gitlab.api.GitLabClient
import de.richargh.pipewatch.gitlab.api.GitLabConfig
import de.richargh.pipewatch.status.app.api.PipelineStatus
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PipelineViewModelTest {
    private val testConfig =
        GitLabConfig(
            baseUrl = "https://gitlab.example.com",
            accessToken = "test-token",
        )

    @Test
    fun `initial state has UNKNOWN status`() {
        val mockEngine = MockEngine { respond("[]", HttpStatusCode.OK) }
        val client = GitLabClient(testConfig, mockEngine)
        val viewModel = PipelineViewModel(client, projectId = 123)

        assertEquals(PipelineStatus.UNKNOWN, viewModel.pipelineStatus.value)

        client.close()
    }

    @Test
    fun `refresh updates status to SUCCESS when pipeline is successful`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content =
                            """
                            [{
                                "id": 12345,
                                "iid": 100,
                                "status": "success",
                                "ref": "main",
                                "sha": "abc123",
                                "web_url": "https://gitlab.example.com/project/-/pipelines/12345"
                            }]
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val viewModel = PipelineViewModel(client, projectId = 123)

            viewModel.refresh()

            assertEquals(PipelineStatus.SUCCESS, viewModel.pipelineStatus.value)
            assertNotNull(viewModel.currentPipeline.value)
            assertEquals(12345L, viewModel.currentPipeline.value?.id)

            client.close()
        }

    @Test
    fun `refresh updates status to FAILED when pipeline failed`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content =
                            """
                            [{
                                "id": 12345,
                                "iid": 100,
                                "status": "failed",
                                "ref": "main",
                                "sha": "abc123"
                            }]
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val viewModel = PipelineViewModel(client, projectId = 123)

            viewModel.refresh()

            assertEquals(PipelineStatus.FAILED, viewModel.pipelineStatus.value)

            client.close()
        }

    @Test
    fun `refresh updates status to RUNNING when pipeline is running`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content =
                            """
                            [{
                                "id": 12345,
                                "iid": 100,
                                "status": "running",
                                "ref": "main",
                                "sha": "abc123"
                            }]
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val viewModel = PipelineViewModel(client, projectId = 123)

            viewModel.refresh()

            assertEquals(PipelineStatus.RUNNING, viewModel.pipelineStatus.value)

            client.close()
        }

    @Test
    fun `refresh sets status to UNKNOWN when no pipelines exist`() =
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
            val viewModel = PipelineViewModel(client, projectId = 123)

            viewModel.refresh()

            assertEquals(PipelineStatus.UNKNOWN, viewModel.pipelineStatus.value)
            assertNull(viewModel.currentPipeline.value)

            client.close()
        }

    @Test
    fun `refresh sets status to UNKNOWN on API error`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"error": "Unauthorized"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val viewModel = PipelineViewModel(client, projectId = 123)

            viewModel.refresh()

            assertEquals(PipelineStatus.UNKNOWN, viewModel.pipelineStatus.value)

            client.close()
        }

    @Test
    fun `pipeline web URL is available after successful refresh`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content =
                            """
                            [{
                                "id": 12345,
                                "iid": 100,
                                "status": "success",
                                "ref": "main",
                                "sha": "abc123",
                                "web_url": "https://gitlab.example.com/project/-/pipelines/12345"
                            }]
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val viewModel = PipelineViewModel(client, projectId = 123)

            viewModel.refresh()

            assertEquals(
                "https://gitlab.example.com/project/-/pipelines/12345",
                viewModel.currentPipeline.value?.webUrl,
            )

            client.close()
        }
}
