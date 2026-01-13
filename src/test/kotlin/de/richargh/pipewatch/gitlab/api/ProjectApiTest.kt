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

class ProjectTest {
    @Test
    fun `Project can be deserialized from GitLab API response`() {
        val json =
            """
            {
                "id": 123,
                "name": "My Project",
                "path": "my-project",
                "path_with_namespace": "group/my-project",
                "description": "A test project",
                "web_url": "https://gitlab.example.com/group/my-project",
                "default_branch": "main",
                "visibility": "private",
                "archived": false
            }
            """.trimIndent()

        val project = Json { ignoreUnknownKeys = true }.decodeFromString<Project>(json)

        assertEquals(123L, project.id)
        assertEquals("My Project", project.name)
        assertEquals("my-project", project.path)
        assertEquals("group/my-project", project.pathWithNamespace)
        assertEquals("A test project", project.description)
        assertEquals("https://gitlab.example.com/group/my-project", project.webUrl)
        assertEquals("main", project.defaultBranch)
        assertEquals("private", project.visibility)
        assertEquals(false, project.archived)
    }

    @Test
    fun `Project handles optional fields being null`() {
        val json =
            """
            {
                "id": 123,
                "name": "My Project",
                "path": "my-project",
                "path_with_namespace": "group/my-project"
            }
            """.trimIndent()

        val project = Json { ignoreUnknownKeys = true }.decodeFromString<Project>(json)

        assertEquals(123L, project.id)
        assertEquals("My Project", project.name)
        assertNull(project.description)
        assertNull(project.webUrl)
        assertNull(project.defaultBranch)
    }
}

class GitLabClientProjectApiTest {
    private val testConfig =
        GitLabConfig(
            baseUrl = "https://gitlab.example.com",
            accessToken = "test-token",
        )

    @Test
    fun `getProjects returns list of projects`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertTrue(request.url.toString().contains("/api/v4/projects"))
                    assertEquals("test-token", request.headers["PRIVATE-TOKEN"])

                    respond(
                        content =
                            """
                            [
                                {
                                    "id": 123,
                                    "name": "Project One",
                                    "path": "project-one",
                                    "path_with_namespace": "group/project-one",
                                    "web_url": "https://gitlab.example.com/group/project-one"
                                },
                                {
                                    "id": 456,
                                    "name": "Project Two",
                                    "path": "project-two",
                                    "path_with_namespace": "group/project-two",
                                    "web_url": "https://gitlab.example.com/group/project-two"
                                }
                            ]
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val projects = client.getProjects()

            assertEquals(2, projects.size)
            assertEquals(123L, projects[0].id)
            assertEquals("Project One", projects[0].name)
            assertEquals(456L, projects[1].id)
            assertEquals("Project Two", projects[1].name)

            client.close()
        }

    @Test
    fun `getProjects returns empty list when no projects exist`() =
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
            val projects = client.getProjects()

            assertTrue(projects.isEmpty())

            client.close()
        }

    @Test
    fun `getProjects uses membership filter by default`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertTrue(request.url.toString().contains("membership=true"))

                    respond(
                        content = "[]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            client.getProjects()

            client.close()
        }

    @Test
    fun `getProjects supports search filter`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertTrue(request.url.toString().contains("search=test"))

                    respond(
                        content =
                            """
                            [
                                {
                                    "id": 123,
                                    "name": "Test Project",
                                    "path": "test-project",
                                    "path_with_namespace": "group/test-project"
                                }
                            ]
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val projects = client.getProjects(search = "test")

            assertEquals(1, projects.size)

            client.close()
        }

    @Test
    fun `getProjects supports pagination`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertTrue(request.url.toString().contains("per_page=50"))
                    assertTrue(request.url.toString().contains("page=2"))

                    respond(
                        content = "[]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            client.getProjects(page = 2, perPage = 50)

            client.close()
        }

    @Test
    fun `getProject returns single project by ID`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals(
                        "https://gitlab.example.com/api/v4/projects/123",
                        request.url.toString(),
                    )

                    respond(
                        content =
                            """
                            {
                                "id": 123,
                                "name": "My Project",
                                "path": "my-project",
                                "path_with_namespace": "group/my-project",
                                "description": "A detailed description",
                                "web_url": "https://gitlab.example.com/group/my-project",
                                "default_branch": "main"
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = GitLabClient(testConfig, mockEngine)
            val project = client.getProject(123)

            assertNotNull(project)
            assertEquals(123L, project.id)
            assertEquals("My Project", project.name)
            assertEquals("A detailed description", project.description)

            client.close()
        }

    @Test
    fun `getProject returns null for non-existent project`() =
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
            val project = client.getProject(999)

            assertNull(project)

            client.close()
        }
}
