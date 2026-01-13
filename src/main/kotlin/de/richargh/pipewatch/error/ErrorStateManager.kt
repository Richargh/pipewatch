package de.richargh.pipewatch.error

import de.richargh.pipewatch.gitlab.api.GitLabApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ErrorStateManager {
    private val _state = MutableStateFlow<ApplicationState>(ApplicationState.Loading)
    val state: StateFlow<ApplicationState> = _state.asStateFlow()

    val currentState: ApplicationState
        get() = _state.value

    val backoff = BackoffStrategy()

    fun setLoading() {
        _state.value = ApplicationState.Loading
    }

    fun setConnected() {
        _state.value = ApplicationState.Connected
        backoff.reset()
    }

    fun setError(
        message: String,
        errorType: ErrorType,
    ) {
        _state.value = ApplicationState.Error(message, errorType)
    }

    fun setErrorFromException(exception: Exception) {
        val (message, errorType) =
            when (exception) {
                is GitLabApiException.Unauthorized ->
                    "Invalid or expired access token" to ErrorType.UNAUTHORIZED
                is GitLabApiException.Forbidden ->
                    "Access denied to this resource" to ErrorType.FORBIDDEN
                is GitLabApiException.NotFound ->
                    "Project or pipeline not found" to ErrorType.NOT_FOUND
                is GitLabApiException.ServerError ->
                    "GitLab server error (${exception.statusCode})" to ErrorType.SERVER_ERROR
                is GitLabApiException.NetworkError ->
                    "Cannot connect to GitLab" to ErrorType.NETWORK
                else ->
                    (exception.message ?: "Unknown error") to ErrorType.UNKNOWN
            }
        setError(message, errorType)
    }

    fun clearError() {
        _state.value = ApplicationState.Connected
        backoff.reset()
    }

    fun shouldRefreshManually(): Boolean {
        backoff.reset()
        return true
    }

    fun getNextBackoffDelay() = backoff.nextDelay()
}
