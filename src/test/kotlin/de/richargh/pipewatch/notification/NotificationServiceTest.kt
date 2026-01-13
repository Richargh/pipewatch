package de.richargh.pipewatch.notification

import de.richargh.pipewatch.gitlab.api.Job
import de.richargh.pipewatch.gitlab.api.Pipeline
import de.richargh.pipewatch.status.app.api.PipelineStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationContentTest {
    @Test
    fun `NotificationContent can be created with required fields`() {
        val content =
            NotificationContent(
                title = "Pipeline Failed",
                body = "Project X failed on branch main",
                url = "https://gitlab.example.com/pipeline/123",
            )

        assertEquals("Pipeline Failed", content.title)
        assertEquals("Project X failed on branch main", content.body)
        assertEquals("https://gitlab.example.com/pipeline/123", content.url)
    }

    @Test
    fun `NotificationContent can be created without URL`() {
        val content =
            NotificationContent(
                title = "Pipeline Failed",
                body = "Project X failed",
            )

        assertNull(content.url)
    }
}

class PipelineStateTrackerTest {
    private lateinit var tracker: PipelineStateTracker

    @BeforeEach
    fun setup() {
        tracker = PipelineStateTracker()
    }

    @Test
    fun `returns null for first status update`() {
        val result = tracker.updateAndCheckForFailure(PipelineStatus.SUCCESS, null)
        assertNull(result)
    }

    @Test
    fun `returns null when status remains the same`() {
        tracker.updateAndCheckForFailure(PipelineStatus.SUCCESS, null)
        val result = tracker.updateAndCheckForFailure(PipelineStatus.SUCCESS, null)
        assertNull(result)
    }

    @Test
    fun `returns failure info when status changes to FAILED`() {
        tracker.updateAndCheckForFailure(PipelineStatus.SUCCESS, null)
        val pipeline = createTestPipeline()
        val result = tracker.updateAndCheckForFailure(PipelineStatus.FAILED, pipeline)

        assertNotNull(result)
        assertTrue(result.isNewFailure)
    }

    @Test
    fun `returns null when FAILED status continues`() {
        val pipeline = createTestPipeline()
        tracker.updateAndCheckForFailure(PipelineStatus.SUCCESS, null)
        tracker.updateAndCheckForFailure(PipelineStatus.FAILED, pipeline)

        val result = tracker.updateAndCheckForFailure(PipelineStatus.FAILED, pipeline)
        assertNull(result)
    }

    @Test
    fun `returns failure info when new pipeline fails`() {
        val oldPipeline = createTestPipeline(id = 1, iid = 100)
        val newPipeline = createTestPipeline(id = 2, iid = 101)

        tracker.updateAndCheckForFailure(PipelineStatus.FAILED, oldPipeline)
        val result = tracker.updateAndCheckForFailure(PipelineStatus.FAILED, newPipeline)

        assertNotNull(result)
        assertTrue(result.isNewFailure)
    }

    @Test
    fun `returns null when status changes from FAILED to SUCCESS`() {
        val pipeline = createTestPipeline()
        tracker.updateAndCheckForFailure(PipelineStatus.FAILED, pipeline)
        val result = tracker.updateAndCheckForFailure(PipelineStatus.SUCCESS, pipeline)

        assertNull(result)
    }

    @Test
    fun `returns null when status is RUNNING`() {
        tracker.updateAndCheckForFailure(PipelineStatus.SUCCESS, null)
        val pipeline = createTestPipeline()
        val result = tracker.updateAndCheckForFailure(PipelineStatus.RUNNING, pipeline)

        assertNull(result)
    }

    private fun createTestPipeline(
        id: Long = 12345,
        iid: Long = 100,
    ) = Pipeline(
        id = id,
        iid = iid,
        status = "failed",
        ref = "main",
        sha = "abc123",
        webUrl = "https://gitlab.example.com/pipeline/$id",
    )
}

class NotificationBuilderTest {
    @Test
    fun `builds notification content for failed pipeline`() {
        val pipeline =
            Pipeline(
                id = 12345,
                iid = 100,
                status = "failed",
                ref = "main",
                sha = "abc123",
                webUrl = "https://gitlab.example.com/pipeline/12345",
            )
        val failedJobs =
            mapOf(
                "test" to
                    listOf(
                        Job(id = 1, name = "unit-test", stage = "test", status = "failed"),
                    ),
            )

        val content = NotificationBuilder.buildFailureNotification(pipeline, "my-project", failedJobs)

        assertEquals("Pipeline Failed", content.title)
        assertTrue(content.body.contains("my-project"))
        assertTrue(content.body.contains("main"))
        assertTrue(content.body.contains("test"))
        assertEquals("https://gitlab.example.com/pipeline/12345", content.url)
    }

    @Test
    fun `builds notification without failed jobs`() {
        val pipeline =
            Pipeline(
                id = 12345,
                iid = 100,
                status = "failed",
                ref = "feature/test",
                sha = "abc123",
                webUrl = "https://gitlab.example.com/pipeline/12345",
            )

        val content = NotificationBuilder.buildFailureNotification(pipeline, "my-project", emptyMap())

        assertEquals("Pipeline Failed", content.title)
        assertTrue(content.body.contains("feature/test"))
        assertFalse(content.body.contains("Failed in"))
    }
}

class FakeNotificationService : NotificationService {
    val sentNotifications = mutableListOf<NotificationContent>()
    var isEnabled = true

    override fun sendNotification(content: NotificationContent) {
        if (isEnabled) {
            sentNotifications.add(content)
        }
    }

    override var notificationsEnabled: Boolean
        get() = isEnabled
        set(value) {
            isEnabled = value
        }
}

class NotificationServiceIntegrationTest {
    @Test
    fun `notification is sent when enabled`() {
        val service = FakeNotificationService()
        service.notificationsEnabled = true

        val content = NotificationContent("Test", "Test body")
        service.sendNotification(content)

        assertEquals(1, service.sentNotifications.size)
    }

    @Test
    fun `notification respects enabled setting`() {
        val service = FakeNotificationService()
        service.notificationsEnabled = false

        val content = NotificationContent("Test", "Test body")
        service.sendNotification(content)

        assertEquals(0, service.sentNotifications.size)
    }
}
