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

class JobTest {
    @Test
    fun `Job can be deserialized from GitLab API response`() {
        val json =
            """
            {
                "id": 789,
                "name": "build",
                "stage": "build",
                "status": "failed",
                "web_url": "https://gitlab.example.com/group/project/-/jobs/789",
                "failure_reason": "script_failure",
                "allow_failure": false,
                "duration": 120.5,
                "created_at": "2024-01-15T10:30:00.000Z",
                "started_at": "2024-01-15T10:31:00.000Z",
                "finished_at": "2024-01-15T10:33:00.000Z"
            }
            """.trimIndent()

        val job = Json { ignoreUnknownKeys = true }.decodeFromString<Job>(json)

        assertEquals(789L, job.id)
        assertEquals("build", job.name)
        assertEquals("build", job.stage)
        assertEquals("failed", job.status)
        assertEquals("https://gitlab.example.com/group/project/-/jobs/789", job.webUrl)
        assertEquals("script_failure", job.failureReason)
        assertEquals(false, job.allowFailure)
        assertEquals(120.5, job.duration)
    }

    @Test
    fun `Job handles optional fields being null`() {
        val json =
            """
            {
                "id": 789,
                "name": "test",
                "stage": "test",
                "status": "running"
            }
            """.trimIndent()

        val job = Json { ignoreUnknownKeys = true }.decodeFromString<Job>(json)

        assertEquals(789L, job.id)
        assertEquals("test", job.name)
        assertEquals("running", job.status)
        assertNull(job.webUrl)
        assertNull(job.failureReason)
        assertNull(job.duration)
    }
}

class GitLabClientJobApiTest {
    private val testConfig =
        GitLabConfig(
            baseUrl = "https://gitlab.example.com",
            accessToken = "test-token",
        )

    @Test
    fun `getPipeline returns detailed pipeline by ID`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals(
                        "https://gitlab.example.com/api/v4/projects/123/pipelines/456",
                        request.url.toString(),
                    )

                    respond(
                        content =
                            """
                            {
                                "id": 456,
                                "iid": 10,
                                "status": "failed",
                                "ref": "main",
                                "sha": "abc123",
                                "web_url": "https://gitlab.example.com/group/project/-/pipelines/456",
                                "created_at": "2024-01-15T10:30:00.000Z",
                                "updated_at": "2024-01-15T10:35:00.000Z"
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val pipeline = client.getPipeline(projectId = 123, pipelineId = 456)

            assertNotNull(pipeline)
            assertEquals(456L, pipeline.id)
            assertEquals("failed", pipeline.status)

            client.close()
        }

    @Test
    fun `getPipeline returns null for non-existent pipeline`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"message": "404 Pipeline Not Found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val pipeline = client.getPipeline(projectId = 123, pipelineId = 999)

            assertNull(pipeline)

            client.close()
        }

    @Test
    fun `getPipelineJobs returns list of jobs`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals(
                        "https://gitlab.example.com/api/v4/projects/123/pipelines/456/jobs",
                        request.url.toString(),
                    )

                    respond(
                        content =
                            """
                            [
                                {
                                    "id": 789,
                                    "name": "build",
                                    "stage": "build",
                                    "status": "success",
                                    "web_url": "https://gitlab.example.com/group/project/-/jobs/789"
                                },
                                {
                                    "id": 790,
                                    "name": "test",
                                    "stage": "test",
                                    "status": "failed",
                                    "web_url": "https://gitlab.example.com/group/project/-/jobs/790",
                                    "failure_reason": "script_failure"
                                }
                            ]
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val jobs = client.getPipelineJobs(projectId = 123, pipelineId = 456)

            assertEquals(2, jobs.size)
            assertEquals("build", jobs[0].name)
            assertEquals("success", jobs[0].status)
            assertEquals("test", jobs[1].name)
            assertEquals("failed", jobs[1].status)
            assertEquals("script_failure", jobs[1].failureReason)

            client.close()
        }

    @Test
    fun `getPipelineJobs returns empty list when no jobs exist`() =
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
            val jobs = client.getPipelineJobs(projectId = 123, pipelineId = 456)

            assertTrue(jobs.isEmpty())

            client.close()
        }

    @Test
    fun `getPipelineJobs handles API error gracefully`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"error": "Forbidden"}""",
                        status = HttpStatusCode.Forbidden,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val jobs = client.getPipelineJobs(projectId = 123, pipelineId = 456)

            assertTrue(jobs.isEmpty())

            client.close()
        }
}

class FailedJobsExtractionTest {
    @Test
    fun `getFailedJobsByStage extracts failed jobs grouped by stage`() {
        val jobs =
            listOf(
                Job(id = 1, name = "build", stage = "build", status = "success"),
                Job(id = 2, name = "lint", stage = "test", status = "success"),
                Job(id = 3, name = "unit-test", stage = "test", status = "failed", failureReason = "script_failure"),
                Job(id = 4, name = "integration-test", stage = "test", status = "failed", failureReason = "timeout"),
                Job(id = 5, name = "deploy", stage = "deploy", status = "failed", failureReason = "script_failure"),
            )

        val failedByStage = jobs.getFailedJobsByStage()

        assertEquals(2, failedByStage.size)
        assertTrue(failedByStage.containsKey("test"))
        assertTrue(failedByStage.containsKey("deploy"))
        assertEquals(2, failedByStage["test"]?.size)
        assertEquals(1, failedByStage["deploy"]?.size)
        assertEquals("unit-test", failedByStage["test"]?.get(0)?.name)
        assertEquals("integration-test", failedByStage["test"]?.get(1)?.name)
        assertEquals("deploy", failedByStage["deploy"]?.get(0)?.name)
    }

    @Test
    fun `getFailedJobsByStage returns empty map when no jobs failed`() {
        val jobs =
            listOf(
                Job(id = 1, name = "build", stage = "build", status = "success"),
                Job(id = 2, name = "test", stage = "test", status = "success"),
            )

        val failedByStage = jobs.getFailedJobsByStage()

        assertTrue(failedByStage.isEmpty())
    }

    @Test
    fun `getFailedJobsByStage handles empty job list`() {
        val jobs = emptyList<Job>()

        val failedByStage = jobs.getFailedJobsByStage()

        assertTrue(failedByStage.isEmpty())
    }

    @Test
    fun `getFailedJobsByStage excludes allowed failures`() {
        val jobs =
            listOf(
                Job(id = 1, name = "optional-lint", stage = "test", status = "failed", allowFailure = true),
                Job(id = 2, name = "required-test", stage = "test", status = "failed", allowFailure = false),
            )

        val failedByStage = jobs.getFailedJobsByStage()

        assertEquals(1, failedByStage["test"]?.size)
        assertEquals("required-test", failedByStage["test"]?.get(0)?.name)
    }
}
