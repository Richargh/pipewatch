package de.richargh.pipewatch.error

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ApplicationStateTest {
    @Test
    fun `Loading state has correct properties`() {
        val state = ApplicationState.Loading

        assertTrue(state.isLoading)
        assertFalse(state.hasError)
        assertNull(state.errorMessage)
    }

    @Test
    fun `Connected state has correct properties`() {
        val state = ApplicationState.Connected

        assertFalse(state.isLoading)
        assertFalse(state.hasError)
        assertNull(state.errorMessage)
    }

    @Test
    fun `Error state has correct properties`() {
        val state = ApplicationState.Error("Connection failed", ErrorType.NETWORK)

        assertFalse(state.isLoading)
        assertTrue(state.hasError)
        assertEquals("Connection failed", state.errorMessage)
        assertEquals(ErrorType.NETWORK, state.errorType)
    }
}

class BackoffStrategyTest {
    private lateinit var backoff: BackoffStrategy

    @BeforeEach
    fun setup() {
        backoff = BackoffStrategy()
    }

    @Test
    fun `first delay is 1 second`() {
        val delay = backoff.nextDelay()
        assertEquals(1.seconds, delay)
    }

    @Test
    fun `delays increase exponentially`() {
        assertEquals(1.seconds, backoff.nextDelay())
        assertEquals(2.seconds, backoff.nextDelay())
        assertEquals(4.seconds, backoff.nextDelay())
        assertEquals(8.seconds, backoff.nextDelay())
        assertEquals(16.seconds, backoff.nextDelay())
    }

    @Test
    fun `delay caps at maximum of 5 minutes`() {
        repeat(20) { backoff.nextDelay() }
        val delay = backoff.nextDelay()
        assertEquals(5.minutes, delay)
    }

    @Test
    fun `reset clears the backoff counter`() {
        repeat(5) { backoff.nextDelay() }
        backoff.reset()
        assertEquals(1.seconds, backoff.nextDelay())
    }

    @Test
    fun `hasDelay is false initially`() {
        assertFalse(backoff.hasDelay)
    }

    @Test
    fun `hasDelay is true after first failure`() {
        backoff.nextDelay()
        assertTrue(backoff.hasDelay)
    }

    @Test
    fun `hasDelay is false after reset`() {
        backoff.nextDelay()
        backoff.reset()
        assertFalse(backoff.hasDelay)
    }
}

class ErrorRecoveryTest {
    @Test
    fun `error state transitions to connected on success`() {
        val stateManager = ErrorStateManager()
        stateManager.setError("Test error", ErrorType.NETWORK)

        assertIs<ApplicationState.Error>(stateManager.currentState)

        stateManager.clearError()

        assertIs<ApplicationState.Connected>(stateManager.currentState)
    }

    @Test
    fun `backoff resets on success`() {
        val stateManager = ErrorStateManager()
        stateManager.setError("Test error", ErrorType.NETWORK)

        // Simulate some backoff delays
        repeat(3) { stateManager.backoff.nextDelay() }
        assertTrue(stateManager.backoff.hasDelay)

        stateManager.clearError()

        assertFalse(stateManager.backoff.hasDelay)
    }
}

class ErrorTypeTest {
    @Test
    fun `error types have display messages`() {
        assertEquals("Network connection failed", ErrorType.NETWORK.displayMessage)
        assertEquals("Authentication failed", ErrorType.UNAUTHORIZED.displayMessage)
        assertEquals("Access denied", ErrorType.FORBIDDEN.displayMessage)
        assertEquals("Resource not found", ErrorType.NOT_FOUND.displayMessage)
        assertEquals("Server error", ErrorType.SERVER_ERROR.displayMessage)
        assertEquals("Unknown error", ErrorType.UNKNOWN.displayMessage)
    }
}

class ErrorStateManagerTest {
    @Nested
    inner class StateTransitions {
        @Test
        fun `initial state is Loading`() {
            val manager = ErrorStateManager()
            assertIs<ApplicationState.Loading>(manager.currentState)
        }

        @Test
        fun `setConnected transitions to Connected`() {
            val manager = ErrorStateManager()
            manager.setConnected()
            assertIs<ApplicationState.Connected>(manager.currentState)
        }

        @Test
        fun `setError transitions to Error`() {
            val manager = ErrorStateManager()
            manager.setError("Test error", ErrorType.NETWORK)
            assertIs<ApplicationState.Error>(manager.currentState)
        }

        @Test
        fun `clearError transitions from Error to Connected`() {
            val manager = ErrorStateManager()
            manager.setError("Test error", ErrorType.NETWORK)
            manager.clearError()
            assertIs<ApplicationState.Connected>(manager.currentState)
        }
    }

    @Nested
    inner class ManualRefresh {
        @Test
        fun `manual refresh bypasses backoff`() {
            val manager = ErrorStateManager()
            manager.setError("Test error", ErrorType.NETWORK)

            // Build up backoff
            repeat(5) { manager.backoff.nextDelay() }

            val shouldRefresh = manager.shouldRefreshManually()
            assertTrue(shouldRefresh)
            assertFalse(manager.backoff.hasDelay)
        }
    }
}
