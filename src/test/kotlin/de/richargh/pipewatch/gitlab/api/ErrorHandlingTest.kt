package de.richargh.pipewatch.gitlab.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GitLabApiExceptionTest {
    @Test
    fun `Unauthorized creates correct exception`() {
        val exception = GitLabApiException.Unauthorized("Invalid token")

        assertEquals(401, exception.statusCode)
        assertEquals("Invalid token", exception.message)
        assertIs<GitLabApiException.Unauthorized>(exception)
    }

    @Test
    fun `Forbidden creates correct exception`() {
        val exception = GitLabApiException.Forbidden("Access denied")

        assertEquals(403, exception.statusCode)
        assertEquals("Access denied", exception.message)
    }

    @Test
    fun `NotFound creates correct exception`() {
        val exception = GitLabApiException.NotFound("Resource not found")

        assertEquals(404, exception.statusCode)
        assertEquals("Resource not found", exception.message)
    }

    @Test
    fun `ServerError creates correct exception`() {
        val exception = GitLabApiException.ServerError("Internal server error", 500)

        assertEquals(500, exception.statusCode)
        assertEquals("Internal server error", exception.message)
    }

    @Test
    fun `NetworkError creates correct exception`() {
        val cause = java.net.ConnectException("Connection refused")
        val exception = GitLabApiException.NetworkError("Failed to connect", cause)

        assertEquals(0, exception.statusCode)
        assertEquals("Failed to connect", exception.message)
        assertEquals(cause, exception.cause)
    }
}

class GitLabClientErrorHandlingTest {
    private val testConfig =
        GitLabConfig(
            baseUrl = "https://gitlab.example.com",
            accessToken = "test-token",
        )

    @Test
    fun `getProjectOrThrow throws Unauthorized on 401`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"message": "401 Unauthorized"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)

            val exception =
                assertThrows<GitLabApiException.Unauthorized> {
                    client.getProjectOrThrow(123)
                }
            assertEquals(401, exception.statusCode)

            client.close()
        }

    @Test
    fun `getProjectOrThrow throws Forbidden on 403`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"message": "403 Forbidden"}""",
                        status = HttpStatusCode.Forbidden,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)

            val exception =
                assertThrows<GitLabApiException.Forbidden> {
                    client.getProjectOrThrow(123)
                }
            assertEquals(403, exception.statusCode)

            client.close()
        }

    @Test
    fun `getProjectOrThrow throws NotFound on 404`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"message": "404 Project Not Found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)

            val exception =
                assertThrows<GitLabApiException.NotFound> {
                    client.getProjectOrThrow(123)
                }
            assertEquals(404, exception.statusCode)

            client.close()
        }

    @Test
    fun `getProjectOrThrow throws ServerError on 500`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"message": "Internal Server Error"}""",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)

            val exception =
                assertThrows<GitLabApiException.ServerError> {
                    client.getProjectOrThrow(123)
                }
            assertEquals(500, exception.statusCode)

            client.close()
        }

    @Test
    fun `getProjectOrThrow returns project on success`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content =
                            """
                            {
                                "id": 123,
                                "name": "My Project",
                                "path": "my-project",
                                "path_with_namespace": "group/my-project"
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val project = client.getProjectOrThrow(123)

            assertEquals(123L, project.id)
            assertEquals("My Project", project.name)

            client.close()
        }

    @Test
    fun `getPipelineJobsOrThrow throws on error`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"message": "403 Forbidden"}""",
                        status = HttpStatusCode.Forbidden,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)

            assertThrows<GitLabApiException.Forbidden> {
                client.getPipelineJobsOrThrow(123, 456)
            }

            client.close()
        }

    @Test
    fun `testConnection returns true when API is accessible`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content =
                            """
                            {
                                "id": 1,
                                "username": "testuser",
                                "name": "Test User"
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val result = client.testConnection()

            assertTrue(result.isSuccess)

            client.close()
        }

    @Test
    fun `testConnection returns failure when unauthorized`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"message": "401 Unauthorized"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val result = client.testConnection()

            assertTrue(result.isFailure)
            assertIs<GitLabApiException.Unauthorized>(result.exceptionOrNull())

            client.close()
        }
}
