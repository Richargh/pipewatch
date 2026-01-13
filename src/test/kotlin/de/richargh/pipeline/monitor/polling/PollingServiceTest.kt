package de.richargh.pipeline.monitor.polling

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class PollingServiceTest {
    @Test
    fun `can instantiate PollingService with interval`() {
        val service = PollingService(interval = 30.seconds)

        assertEquals(30.seconds, service.interval)
    }

    @Test
    fun `polling is not active initially`() {
        val service = PollingService(interval = 30.seconds)

        assertFalse(service.isPolling)
    }

    @Test
    fun `start begins polling`() =
        runTest {
            var callCount = 0
            val service = PollingService(interval = 100.milliseconds)

            val job =
                launch {
                    service.start { callCount++ }
                }

            // Let it run a bit
            advanceTimeBy(50)
            assertTrue(service.isPolling)

            service.stop()
            job.cancel()
        }

    @Test
    fun `poll action is called immediately on start`() =
        runTest {
            var callCount = 0
            val service = PollingService(interval = 1.seconds)

            val job =
                launch {
                    service.start { callCount++ }
                }

            // Advance just enough for the first call
            advanceTimeBy(10)

            assertTrue(callCount >= 1, "Poll action should be called at least once immediately")

            service.stop()
            job.cancel()
        }

    @Test
    fun `poll action is called at specified interval`() =
        runTest {
            var callCount = 0
            val service = PollingService(interval = 100.milliseconds)

            val job =
                launch {
                    service.start { callCount++ }
                }

            // First call is immediate
            advanceTimeBy(10)
            assertEquals(1, callCount)

            // After one interval
            advanceTimeBy(100)
            assertEquals(2, callCount)

            // After another interval
            advanceTimeBy(100)
            assertEquals(3, callCount)

            service.stop()
            job.cancel()
        }

    @Test
    fun `stop halts polling`() =
        runTest {
            var callCount = 0
            val service = PollingService(interval = 100.milliseconds)

            val job =
                launch {
                    service.start { callCount++ }
                }

            advanceTimeBy(10)
            assertEquals(1, callCount)

            service.stop()
            assertFalse(service.isPolling)

            val countAfterStop = callCount
            advanceTimeBy(500)

            assertEquals(countAfterStop, callCount, "No more calls should happen after stop")

            job.cancel()
        }

    @Test
    fun `can change interval`() {
        val service = PollingService(interval = 30.seconds)

        service.setInterval(60.seconds)

        assertEquals(60.seconds, service.interval)
    }
}
