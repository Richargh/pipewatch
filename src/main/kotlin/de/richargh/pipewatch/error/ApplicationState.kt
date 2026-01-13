package de.richargh.pipewatch.error

sealed class ApplicationState {
    abstract val isLoading: Boolean
    abstract val hasError: Boolean
    abstract val errorMessage: String?

    data object Loading : ApplicationState() {
        override val isLoading = true
        override val hasError = false
        override val errorMessage: String? = null
    }

    data object Connected : ApplicationState() {
        override val isLoading = false
        override val hasError = false
        override val errorMessage: String? = null
    }

    data class Error(
        override val errorMessage: String,
        val errorType: ErrorType,
    ) : ApplicationState() {
        override val isLoading = false
        override val hasError = true
    }
}

enum class ErrorType(val displayMessage: String) {
    NETWORK("Network connection failed"),
    UNAUTHORIZED("Authentication failed"),
    FORBIDDEN("Access denied"),
    NOT_FOUND("Resource not found"),
    SERVER_ERROR("Server error"),
    UNKNOWN("Unknown error"),
}
